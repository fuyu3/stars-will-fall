package com.fuyu.starswillfall.guardasol;

import com.fuyu.starswillfall.StarsWillFall;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Névoa de flores: uma nuvem de pétalas que dá náusea e veneno a quem estiver dentro dela.
 *
 * <p>Não usa uma entidade própria (assim não precisa de renderizador nem de registro extra):
 * as nuvens ativas ficam numa lista e são processadas a cada tick do mundo. Quem lançou a névoa
 * e os animais domesticados dele não são afetados. As nuvens não sobrevivem a reiniciar o servidor.
 * Para trocar os efeitos, veja {@link #applyEffects}; para trocar as partículas, {@link #spawnParticles}.
 */
@EventBusSubscriber(modid = StarsWillFall.MOD_ID)
public final class FlowerMist {
    private static final String COOLDOWN_TAG = StarsWillFall.MOD_ID + "_mist_ready_at";
    private static final List<Cloud> CLOUDS = new ArrayList<>();

    private FlowerMist() {
    }

    private static final class Cloud {
        final ResourceKey<Level> dimension;
        final Vec3 center;
        final UUID owner;
        int age;

        Cloud(ResourceKey<Level> dimension, Vec3 center, UUID owner) {
            this.dimension = dimension;
            this.center = center;
            this.owner = owner;
        }
    }

    // ---- Lançamento ------------------------------------------------------------------------------

    public static InteractionResultHolder<ItemStack> cast(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        long now = level.getGameTime();
        long readyAt = player.getPersistentData().getLong(COOLDOWN_TAG);
        if (readyAt > now) {
            long seconds = (readyAt - now + 19L) / 20L;
            player.displayClientMessage(Component.translatable("message.starswillfall.guarda_sol.mist_cooldown", seconds), true);
            return InteractionResultHolder.fail(stack);
        }
        player.getPersistentData().putLong(COOLDOWN_TAG, now + GuardaSolTuning.MIST_COOLDOWN_TICKS);

        HitResult hit = player.pick(GuardaSolTuning.MIST_RANGE, 1.0F, false);
        Vec3 center = hit.getLocation();
        CLOUDS.add(new Cloud(serverLevel.dimension(), center, player.getUUID()));

        level.playSound(null, player.blockPosition(), SoundEvents.SPORE_BLOSSOM_PLACE, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 1.2F, 0.8F);
        stack.hurtAndBreak(GuardaSolTuning.MIST_DURABILITY_COST, player, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // ---- Vida da névoa ---------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || CLOUDS.isEmpty()) {
            return;
        }
        Iterator<Cloud> iterator = CLOUDS.iterator();
        while (iterator.hasNext()) {
            Cloud cloud = iterator.next();
            if (!cloud.dimension.equals(level.dimension())) {
                continue;
            }
            if (cloud.age++ >= GuardaSolTuning.MIST_DURATION_TICKS) {
                iterator.remove();
                continue;
            }
            if (cloud.age % 2 == 0) {
                spawnParticles(level, cloud);
            }
            if (cloud.age % GuardaSolTuning.MIST_EFFECT_INTERVAL == 0) {
                applyEffects(level, cloud);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CLOUDS.clear();
    }

    private static void spawnParticles(ServerLevel level, Cloud cloud) {
        double spread = GuardaSolTuning.MIST_RADIUS * 0.45D;
        Vec3 c = cloud.center;
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 0.8D, c.z, 8, spread, 0.6D, spread, 0.02D);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 0.8D, c.z, 6, spread, 0.6D, spread, 0.0D);
    }

    private static void applyEffects(ServerLevel level, Cloud cloud) {
        double radius = GuardaSolTuning.MIST_RADIUS;
        Vec3 c = cloud.center;
        AABB area = new AABB(c.x - radius, c.y - 1.0D, c.z - radius, c.x + radius, c.y + 2.5D, c.z + radius);
        Entity owner = level.getEntity(cloud.owner);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> canAffect(e, cloud))) {
            double dx = target.getX() - c.x;
            double dz = target.getZ() - c.z;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, GuardaSolTuning.NAUSEA_TICKS, 0), owner);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, GuardaSolTuning.POISON_TICKS, GuardaSolTuning.POISON_AMPLIFIER), owner);
        }
    }

    private static boolean canAffect(LivingEntity target, Cloud cloud) {
        if (!target.isAlive() || target.isSpectator() || target.getUUID().equals(cloud.owner)) {
            return false;
        }
        if (target instanceof OwnableEntity ownable && cloud.owner.equals(ownable.getOwnerUUID())) {
            return false;
        }
        return GuardaSolTuning.MIST_AFFECTS_PLAYERS || !(target instanceof Player);
    }
}
