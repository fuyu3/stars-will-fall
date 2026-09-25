package com.fuyu.starswillfall.event;

import com.fuyu.starswillfall.StarsWillFall;
import com.fuyu.starswillfall.item.ModItems;
import com.fuyu.starswillfall.util.StarEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = StarsWillFall.MOD_ID)
public final class ScytheLifeStealHandler {
    private ScytheLifeStealHandler() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        // O evento pós-dano informa o quanto de vida foi realmente retirado do alvo.
        if (event.getNewDamage() <= 0.0F || !(event.getSource().getDirectEntity() instanceof Player player)) {
            return;
        }

        // A entidade direta precisa ser o jogador: projéteis disparados enquanto a foice
        // está na mão não ativam roubo de vida.
        if (player.getMainHandItem().is(ModItems.SCYTHE.get())) {
            player.heal(event.getNewDamage() / 4.0F);
            if (player.level() instanceof ServerLevel level) {
                StarEffects.burst(level, player.position().add(0.0D, 1.0D, 0.0D), 4);
            }
        }
    }
}
