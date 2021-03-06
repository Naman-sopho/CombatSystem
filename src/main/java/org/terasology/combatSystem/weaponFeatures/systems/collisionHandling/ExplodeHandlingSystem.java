package org.terasology.combatSystem.weaponFeatures.systems.collisionHandling;

import org.terasology.combatSystem.hurting.HurtEvent;
import org.terasology.combatSystem.weaponFeatures.components.ExplodeComponent;
import org.terasology.combatSystem.weaponFeatures.components.ExplosionComponent;
import org.terasology.combatSystem.weaponFeatures.events.ExplodeEvent;
import org.terasology.combatSystem.weaponFeatures.events.ExplosionEvent;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.health.DestroyEvent;
import org.terasology.logic.health.EngineDamageTypes;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.events.CollideEvent;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.BlockComponent;

@RegisterSystem
public class ExplodeHandlingSystem extends BaseComponentSystem{
    @In
    EntityManager entityManager;
    @In
    BlockEntityRegistry registry;
    
    @ReceiveEvent(components = ExplodeComponent.class)
    public void explodeOnContact(CollideEvent event, EntityRef entity){
        EntityRef target = event.getOtherEntity();
        
        // damage the other entity
        entity.send(new HurtEvent(target));
        
        entity.send(new ExplodeEvent());
        
        if(target != null && target != EntityRef.NULL && target.exists()){
            if(target.hasComponent(ExplodeComponent.class)){
                target.send(new ExplodeEvent());
            }
        }
        
        event.consume();
    }
    
    @ReceiveEvent(components = ExplodeComponent.class)
    public void exploding(ExplodeEvent event, EntityRef entity){
        explode(entity);
    }
    
    @ReceiveEvent(components = ExplodeComponent.class, priority = EventPriority.PRIORITY_HIGH)
    public void explodeOnDestroy(DestroyEvent event, EntityRef entity){
        explode(entity);
    }
    
    //--------------------------------private methods--------------------------------
    
    private void explode(EntityRef entity){
        LocationComponent location = entity.getComponent(LocationComponent.class);
        if(location == null){
            return;
        }
        ExplodeComponent explode = entity.getComponent(ExplodeComponent.class);
        EntityBuilder explosion = entityManager.newBuilder(explode.explosionPrefab);
        
        if(explosion != null){
            LocationComponent explosionLoc = explosion.getComponent(LocationComponent.class);
            if(explosionLoc != null){
                explosionLoc.setWorldPosition(location.getWorldPosition());
                explosionLoc.setWorldRotation(location.getWorldRotation());
                explosionLoc.setWorldScale(location.getWorldScale());
                
                explosion.addOrSaveComponent(explosionLoc);
            }
        }
        entity.removeComponent(ExplodeComponent.class);
        
        if(entity.hasComponent(BlockComponent.class)){
            BlockComponent block = entity.getComponent(BlockComponent.class);
            Vector3i blockPos = block.getPosition();
            Vector3i offset = new Vector3i();
            for(int x=-1; x<2; x++){
                for(int y=-1; y<2; y++){
                    for(int z=-1; z<2; z++){
                        if(x == 0 && y == 0 && z == 0){
                            continue;
                        }
                        offset.set(x, y, z);
                        offset.add(blockPos);
                        
                        EntityRef adjacentBlock = registry.getBlockEntityAt(offset);
                        if(adjacentBlock.hasComponent(ExplodeComponent.class)){
                            adjacentBlock.send(new ExplodeEvent());
                        }
                    }
                }
            }
        }
        
        entity.send(new DestroyEvent(EntityRef.NULL, EntityRef.NULL, EngineDamageTypes.EXPLOSIVE.get()));
        
        if(explosion != null){
            EntityRef explosionEntity = explosion.build();
            
            if(explosionEntity.hasComponent(ExplosionComponent.class)){
                explosionEntity.send(new ExplosionEvent());
            }
        }
    }

}
