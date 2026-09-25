package com.fuyu.starswillfall.guardasol;

import com.fuyu.starswillfall.item.GuardaSolItem;
import com.fuyu.starswillfall.util.LookImpulse;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Impulso + Planar.
 *
 * <p>Segurar o clique direito com os pés no chão dá um impulso na direção do olhar; depois disso (ou se o
 * clique já foi feito no ar) o guarda-sol segura a queda e leva o jogador para onde ele olha.
 * O movimento do jogador é decidido pelo cliente, por isso a velocidade só é mexida quando
 * {@code level.isClientSide()}; o servidor cuida de anular o dano de queda e gastar durabilidade.
 */
public final class GuardaSolGlide {
    private GuardaSolGlide() {
    }

    public static InteractionResultHolder<ItemStack> start(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (player.isFallFlying() || player.isPassenger() || player.isInWaterOrBubble() || player.isInLava()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.onGround()) {
            launch(level, player);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    private static void launch(Level level, Player player) {
        LookImpulse.apply(level, player, GuardaSolTuning.LAUNCH_SPEED, GuardaSolTuning.LAUNCH_MIN_UP, true);

        if (level instanceof ServerLevel serverLevel) {
            level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(),
                    SoundSource.PLAYERS, 0.8F, 1.3F);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.1D, player.getZ(), 12, 0.3D, 0.05D, 0.3D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    player.getX(), player.getY() + 0.5D, player.getZ(), 14, 0.5D, 0.3D, 0.5D, 0.05D);
        }
    }

    /** Chamado a cada tick (cliente e servidor) enquanto o botão está pressionado. */
    public static void tick(Level level, LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int used = player.getTicksUsingItem();
        boolean landed = player.onGround() && used > 4;
        if (landed || used > GuardaSolTuning.GLIDE_MAX_TICKS || player.isInWaterOrBubble()
                || player.isInLava() || player.isFallFlying() || player.isPassenger()) {
            finish(player, stack);
            return;
        }

        player.fallDistance = 0.0F;

        if (level.isClientSide()) {
            steer(player);
            if (used % 3 == 0) {
                level.addParticle(ParticleTypes.CHERRY_LEAVES,
                        player.getX() + (player.getRandom().nextDouble() - 0.5D) * 1.4D,
                        player.getY() + 2.1D,
                        player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 1.4D,
                        0.0D, -0.02D, 0.0D);
            }
        } else if (used > 0 && used % GuardaSolTuning.GLIDE_DURABILITY_INTERVAL == 0) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
    }

    private static void steer(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 velocity = player.getDeltaMovement();
        double horizontalLook = Math.sqrt(look.x * look.x + look.z * look.z);
        double vx = velocity.x;
        double vz = velocity.z;
        if (horizontalLook > 1.0E-3D) {
            vx += (look.x / horizontalLook * GuardaSolTuning.GLIDE_SPEED - vx) * GuardaSolTuning.GLIDE_STEER;
            vz += (look.z / horizontalLook * GuardaSolTuning.GLIDE_SPEED - vz) * GuardaSolTuning.GLIDE_STEER;
        }
        // Só limita a queda; a subida do impulso inicial passa intacta.
        double vy = Math.max(velocity.y, -GuardaSolTuning.GLIDE_FALL_SPEED);
        player.setDeltaMovement(vx, vy, vz);
    }

    /** Termina o planeio por conta própria (pousou, acabou o tempo, caiu na água). */
    private static void finish(Player player, ItemStack stack) {
        player.getCooldowns().addCooldown(stack.getItem(), GuardaSolTuning.GLIDE_COOLDOWN_TICKS);
        player.stopUsingItem();
    }

    /** O jogador soltou o botão. Toques muito curtos não geram recarga. */
    public static void onRelease(GuardaSolItem item, LivingEntity entity, int ticksUsed) {
        if (entity instanceof Player player && ticksUsed >= 5) {
            player.getCooldowns().addCooldown(item, GuardaSolTuning.GLIDE_COOLDOWN_TICKS);
        }
    }
}
