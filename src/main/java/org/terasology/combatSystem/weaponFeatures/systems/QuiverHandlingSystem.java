package org.terasology.combatSystem.weaponFeatures.systems;

import org.terasology.combatSystem.weaponFeatures.components.LaunchEntityComponent;
import org.terasology.combatSystem.weaponFeatures.components.WorldAvatarComponent;
import org.terasology.combatSystem.weaponFeatures.events.ReduceAmmoEvent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.events.BeforeItemPutInInventory;
import org.terasology.registry.In;

@RegisterSystem
public class QuiverHandlingSystem extends BaseComponentSystem{
    @In
    InventoryManager inventory;
    
    @ReceiveEvent(priority = EventPriority.PRIORITY_HIGH)
    public void avoidPuttingNonThrowableItemsInQuiver(BeforeItemPutInInventory event, EntityRef quiver){
        if(!quiver.hasComponent(LaunchEntityComponent.class)){
            return;
        }
        
        EntityRef item = event.getItem();
        
        if(!item.hasComponent(WorldAvatarComponent.class)){
            event.consume();
        }
    }
    
    @ReceiveEvent( components = {InventoryComponent.class})
    public void makePrefabForLaunchAvailable(OnChangedComponent event, EntityRef entity){
        LaunchEntityComponent launchEntity = entity.getComponent(LaunchEntityComponent.class);
        if(launchEntity == null){
            return;
        }
        EntityRef entityToLaunch = InventoryUtils.getItemAt(entity, 0);
        
        if(entityToLaunch == EntityRef.NULL){
            launchEntity.launchEntityPrefab = null;
            entity.saveComponent(launchEntity);
            return;
        }
        
        WorldAvatarComponent avatar = entityToLaunch.getComponent(WorldAvatarComponent.class);
        if(avatar == null){
            launchEntity.launchEntityPrefab = null;
            entity.saveComponent(launchEntity);
            return;
        }
        
        launchEntity.launchEntityPrefab = avatar.worldAvatarPrefab;
        entity.saveComponent(launchEntity);
    }
    
    @ReceiveEvent (components = InventoryComponent.class)
    public void reduceAmmo(ReduceAmmoEvent event, EntityRef entity){
        EntityRef item = InventoryUtils.getItemAt(entity, 0);
        
        ItemComponent itemComp = item.getComponent(ItemComponent.class);
        if(itemComp == null){
            return;
        }
        
        // to see if the item can be consumed
        if(itemComp.stackId.isEmpty()){
            return;
        }
        
        inventory.removeItem(entity, entity, item, true, 1);
        
        if(InventoryUtils.getItemAt(entity, 0) == EntityRef.NULL){
            LaunchEntityComponent launchEntity = entity.getComponent(LaunchEntityComponent.class);
            if(launchEntity == null){
                return;
            }
            
            launchEntity.launchEntityPrefab = null;
            
            entity.saveComponent(launchEntity);
        }
    }

}
