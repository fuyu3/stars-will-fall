package com.fuyu.starswillfall.item;

import com.fuyu.starswillfall.particle.ModParticles;
import com.fuyu.starswillfall.scythe.ScytheDash;
import com.fuyu.starswillfall.scythe.ScytheProjectile;
import com.fuyu.starswillfall.scythe.ScytheTuning;
import com.fuyu.starswillfall.util.StarEffects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Uma espada visualmente representada como foice. Herdar SwordItem preserva
 * o ataque em área, os encantamentos e as demais interações das espadas.
 *
 * <pre>
 * Agachar + clique direito ..... troca o modo: BUMERANGUE <-> IMPULSO ESTELAR
 * Clique direito (modo bumerangue) ... arremessa (volta para o inventário)
 * Clique direito (modo impulso) ...... impulso estelar na direção do olhar, sem recarga
 * Golpes e o item na mão ....... soltam estrelas
 * </pre>
 *
 * A lógica fica em {@code com.fuyu.starswillfall.scythe}; os números em {@code ScytheTuning}.
 */
public final class ScytheItem extends SwordItem {
    public ScytheItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return toggleMode(level, player, stack);
        }
        return isDashMode(stack) ? ScytheDash.dash(level, player, hand, stack) : throwBoomerang(level, player, hand, stack);
    }

    public static boolean isDashMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SCYTHE_DASH_MODE.get(), false);
    }

    private InteractionResultHolder<ItemStack> toggleMode(Level level, Player player, ItemStack stack) {
        boolean dashMode = !isDashMode(stack);
        stack.set(ModDataComponents.SCYTHE_DASH_MODE.get(), dashMode);
        player.getCooldowns().addCooldown(this, ScytheTuning.MODE_SWITCH_COOLDOWN_TICKS);

        if (level instanceof ServerLevel serverLevel) {
            player.displayClientMessage(Component.translatable(dashMode
                    ? "message.starswillfall.scythe.mode.dash" : "message.starswillfall.scythe.mode.boomerang"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    1.0F, dashMode ? 1.5F : 0.9F);
            StarEffects.burst(serverLevel, player.position().add(0.0D, 1.2D, 0.0D), 8);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private InteractionResultHolder<ItemStack> throwBoomerang(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (level instanceof ServerLevel serverLevel) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
            serverLevel.addFreshEntity(ScytheProjectile.create(level, player, stack.copyWithCount(1), slot));
            level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
            StarEffects.burst(serverLevel, player.getEyePosition().add(player.getLookAngle()), 6);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.shrink(1); // a foice está no ar; volta pelas mãos da entidade
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel level) {
            StarEffects.burst(level, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), 10);
        }
    }

    /** Estrelas ocasionais em volta de quem segura a foice. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (isSelected && level instanceof ServerLevel serverLevel && level.getGameTime() % 10L == 0L) {
            serverLevel.sendParticles(ModParticles.STAR.get(),
                    entity.getX(), entity.getY() + 1.0D, entity.getZ(), 1, 0.5D, 0.6D, 0.5D, 0.0D);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean dashMode = isDashMode(stack);
        tooltip.add(Component.translatable(dashMode ? "tooltip.starswillfall.scythe.mode.dash" : "tooltip.starswillfall.scythe.mode.boomerang")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable(dashMode ? "tooltip.starswillfall.scythe.use.dash" : "tooltip.starswillfall.scythe.use.boomerang")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.starswillfall.scythe.toggle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
