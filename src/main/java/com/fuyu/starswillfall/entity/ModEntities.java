package com.fuyu.starswillfall.entity;

import com.fuyu.starswillfall.StarsWillFall;
import com.fuyu.starswillfall.scythe.ScytheProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, StarsWillFall.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ScytheProjectile>> SCYTHE_PROJECTILE =
            ENTITY_TYPES.register("scythe_projectile", () -> EntityType.Builder
                    .<ScytheProjectile>of(ScytheProjectile::new, MobCategory.MISC)
                    .sized(0.8F, 0.4F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .build("scythe_projectile"));

    private ModEntities() {
    }
}
