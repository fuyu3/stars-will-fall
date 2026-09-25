package com.fuyu.starswillfall.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Impulso na direção em que o jogador está olhando. Usado pelo guarda-sol e pela foice.
 */
public final class LookImpulse {
    private LookImpulse() {
    }

    /**
     * @param speed           velocidade do impulso em blocos por tick, na direção do olhar
     * @param minUp           velocidade vertical mínima (descola o jogador do chão mesmo olhando reto ou para baixo)
     * @param predictOnClient true = o cliente também aplica na hora (sem atraso, mas o servidor não
     *                        consegue impor recarga própria); false = só o servidor aplica e avisa o cliente
     */
    public static void apply(Level level, Player player, double speed, double minUp, boolean predictOnClient) {
        if (level.isClientSide() && !predictOnClient) {
            return;
        }
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * speed, Math.max(look.y * speed, minUp), look.z * speed);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        if (!level.isClientSide()) {
            player.hurtMarked = true; // envia a velocidade ao cliente do jogador
        }
    }
}
