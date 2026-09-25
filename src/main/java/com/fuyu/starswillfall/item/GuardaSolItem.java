package com.fuyu.starswillfall.item;

import com.fuyu.starswillfall.guardasol.FlowerBloom;
import com.fuyu.starswillfall.guardasol.FlowerMist;
import com.fuyu.starswillfall.guardasol.GuardaSolGlide;
import com.fuyu.starswillfall.guardasol.GuardaSolTuning;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Guarda-sol com duas formas, guardadas no componente {@link ModDataComponents#GUARDA_SOL_CLOSED}:
 *
 * <pre>
 * ABERTO  (guarda-sol)  clique direito no topo de um bloco  Florescer (cria flores)
 *                       segurar clique direito (no ar, ou
 *                       em parede/teto) ................. Impulso no olhar + Planar
 * FECHADO (espada)      clique direito .................. Névoa de flores (náusea + veneno)
 *                       ataque ......................... espada (com golpe varrido)
 * Agachar + clique direito em qualquer forma ............ abre/fecha o guarda-sol
 * </pre>
 *
 * A lógica de cada habilidade fica em {@code com.fuyu.starswillfall.guardasol}; esta classe só
 * decide qual delas chamar. Para mudar o botão de uma habilidade, mexa apenas em
 * {@link #use} e {@link #useOn}.
 */
public final class GuardaSolItem extends Item {
    /** Duração "infinita" de uso: o planeio é encerrado por {@link GuardaSolGlide}, não pelo relógio. */
    public static final int USE_DURATION = 72000;

    public static final ItemAttributeModifiers OPEN_ATTRIBUTES =
            attributes(GuardaSolTuning.OPEN_DAMAGE, GuardaSolTuning.OPEN_ATTACK_SPEED);
    public static final ItemAttributeModifiers CLOSED_ATTRIBUTES =
            attributes(GuardaSolTuning.CLOSED_DAMAGE, GuardaSolTuning.CLOSED_ATTACK_SPEED);

    public GuardaSolItem(Properties properties) {
        super(properties);
    }

    private static ItemAttributeModifiers attributes(double damage, double attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static boolean isClosed(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUARDA_SOL_CLOSED.get(), false);
    }

    // ---- Entradas do jogador -------------------------------------------------------------------

    /** Clique direito em um bloco (forma aberta: Florescer). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        // Só a face de cima de um bloco floresce. Nos demais casos (no ar, agachado, fechado, parede)
        // o clique cai no método use(): impulso/planar, abrir/fechar, névoa.
        if (player == null || player.isShiftKeyDown() || isClosed(stack) || !player.onGround()
                || context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        return FlowerBloom.bloom(context);
    }

    /** Clique direito no ar, ou em bloco quando useOn deixou passar. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return toggleForm(level, player, stack);
        }
        if (isClosed(stack)) {
            return FlowerMist.cast(level, player, hand, stack);
        }
        return GuardaSolGlide.start(level, player, hand, stack);
    }

    private InteractionResultHolder<ItemStack> toggleForm(Level level, Player player, ItemStack stack) {
        boolean closing = !isClosed(stack);
        stack.set(ModDataComponents.GUARDA_SOL_CLOSED.get(), closing);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, closing ? CLOSED_ATTRIBUTES : OPEN_ATTRIBUTES);
        player.getCooldowns().addCooldown(this, GuardaSolTuning.TOGGLE_COOLDOWN_TICKS);

        if (level instanceof ServerLevel serverLevel) {
            level.playSound(null, player.blockPosition(),
                    closing ? SoundEvents.ARMOR_EQUIP_IRON.value() : SoundEvents.WOOL_PLACE,
                    SoundSource.PLAYERS, 1.0F, closing ? 1.2F : 0.9F);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    player.getX(), player.getY() + 1.2D, player.getZ(), 10, 0.4D, 0.4D, 0.4D, 0.02D);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ---- Planar: enquanto o botão está pressionado ---------------------------------------------

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isClosed(stack) ? 0 : USE_DURATION;
    }

    /**
     * Planando, o jogador levanta o braço segurando o guarda-sol para cima. Usamos a animação
     * "lança" do jogo (o braço do tridente): o próprio Minecraft ergue o braço na terceira pessoa,
     * sem precisar de código de cliente. A orientação do item nessa pose está em
     * models/item/guarda_sol_gliding.json (bloco "display").
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isClosed(stack) ? UseAnim.NONE : UseAnim.SPEAR;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!isClosed(stack)) {
            GuardaSolGlide.tick(level, entity, stack);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        GuardaSolGlide.onRelease(this, entity, USE_DURATION - timeCharged);
    }

    // ---- Combate (forma fechada = espada) ------------------------------------------------------

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    /** Só a forma de espada faz o golpe varrido (o ataque em arco das espadas). */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return isClosed(stack) && itemAbility == ItemAbilities.SWORD_SWEEP;
    }

    // ---- Encantar e reparar --------------------------------------------------------------------

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    /** Qualquer flor conserta o guarda-sol na bigorna. */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ItemTags.FLOWERS);
    }

    // ---- Descrição -----------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isClosed(stack)) {
            tooltip.add(Component.translatable("tooltip.starswillfall.guarda_sol.form.closed").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("tooltip.starswillfall.guarda_sol.closed.use").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.starswillfall.guarda_sol.form.open").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("tooltip.starswillfall.guarda_sol.open.block").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.starswillfall.guarda_sol.open.air").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.starswillfall.guarda_sol.toggle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
