package com.fuyu.starswillfall.util;

import com.fuyu.starswillfall.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Efeitos de estrelas da foice, num lugar só para ficar fácil de trocar. */
public final class StarEffects {
    private StarEffects() {
    }

    /** Explosão de estrelas (golpes, pegar a foice, dash). */
    public static void burst(ServerLevel level, Vec3 pos, int count) {
        level.sendParticles(ModParticles.STAR.get(), pos.x, pos.y, pos.z, count, 0.3D, 0.3D, 0.3D, 0.12D);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, Math.max(1, count / 3), 0.25D, 0.25D, 0.25D, 0.05D);
    }

    /** Rastro de estrelas (foice voando, jogador em dash). */
    public static void trail(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticles.STAR.get(), pos.x, pos.y, pos.z, 2, 0.2D, 0.2D, 0.2D, 0.02D);
    }
}
