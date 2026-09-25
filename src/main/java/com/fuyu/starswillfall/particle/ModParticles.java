package com.fuyu.starswillfall.particle;

import com.fuyu.starswillfall.StarsWillFall;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, StarsWillFall.MOD_ID);

    /** Estrelinha cintilante. Textura: assets/starswillfall/textures/particle/star.png */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR =
            PARTICLES.register("star", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
