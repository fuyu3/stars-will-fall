package com.fuyu.starswillfall.scythe;

import com.fuyu.starswillfall.StarsWillFall;
import com.fuyu.starswillfall.util.LookImpulse;
import com.fuyu.starswillfall.util.StarEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Impulso estelar: lança o jogador na direção do olhar deixando um rastro de estrelas.
 * Não tem recarga (igual ao impulso do guarda-sol); veja {@link ScytheTuning#DASH_GROUND_ONLY}.
 * Como o guarda-sol, o cliente aplica o movimento na hora e o servidor confirma.
 */
@EventBusSubscriber(modid = StarsWillFall.MOD_ID)
public final class ScytheDash {
    private static final String SAFE_FALL_TAG = StarsWillFall.MOD_ID + "_dash_safe_until";

    private ScytheDash() {
    }

    public static InteractionResultHolder<ItemStack> dash(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (ScytheTuning.DASH_GROUND_ONLY && !player.onGround()) {
            return InteractionResultHolder.fail(stack);
        }

        LookImpulse.apply(level, player, ScytheTuning.DASH_SPEED, ScytheTuning.DASH_MIN_UP, true);

        if (level instanceof ServerLevel serverLevel) {
            player.getPersistentData().putLong(SAFE_FALL_TAG, level.getGameTime() + ScytheTuning.DASH_SAFE_FALL_TICKS);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.5F);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
            StarEffects.burst(serverLevel, player.position().add(0.0D, 0.9D, 0.0D), 16);
            stack.hurtAndBreak(ScytheTuning.DASH_DURABILITY_COST, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Rastro de estrelas e amortecimento de queda enquanto dura o impulso. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.getPersistentData().getLong(SAFE_FALL_TAG) > level.getGameTime()) {
            player.fallDistance = 0.0F;
            StarEffects.trail(level, player.position().add(0.0D, 0.6D, 0.0D));
        }
    }
}
