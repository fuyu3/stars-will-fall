package com.fuyu.starswillfall.guardasol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Florescer: espalha flores em volta do bloco clicado.
 * Só põe flores onde elas realmente sobreviveriam (grama, terra, musgo...) e só troca ar ou
 * plantas pequenas, nunca água nem blocos sólidos.
 */
public final class FlowerBloom {
    private static final List<Block> FLOWERS = List.of(
            Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET,
            Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY);

    private FlowerBloom() {
    }

    public static InteractionResult bloom(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        // O cliente devolve "sucesso" para não cair no impulso do use(); o servidor decide de verdade.
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(true);
        }

        ItemStack stack = context.getItemInHand();
        BlockPos center = context.getClickedPos();
        RandomSource random = level.getRandom();
        int radius = GuardaSolTuning.BLOOM_RADIUS;

        List<BlockPos> columns = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius + 1) {
                    columns.add(center.offset(dx, 0, dz));
                }
            }
        }
        Collections.shuffle(columns, new java.util.Random(random.nextLong()));

        int placed = 0;
        for (BlockPos column : columns) {
            if (placed >= GuardaSolTuning.BLOOM_MAX_FLOWERS) {
                break;
            }
            if (random.nextFloat() > GuardaSolTuning.BLOOM_CHANCE) {
                continue;
            }
            BlockState flower = FLOWERS.get(random.nextInt(FLOWERS.size())).defaultBlockState();
            // Procura o chão daquela coluna, de cima para baixo, numa faixa curta de altura.
            for (int dy = 2; dy >= -1; dy--) {
                BlockPos pos = column.above(dy);
                if (canReplace(level, pos) && flower.canSurvive(level, pos)
                        && level.mayInteract(player, pos)
                        && player.mayUseItemAt(pos, Direction.UP, stack)) {
                    level.setBlock(pos, flower, Block.UPDATE_ALL);
                    level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, flower));
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D, 3, 0.25D, 0.2D, 0.25D, 0.0D);
                    placed++;
                    break;
                }
            }
        }

        if (placed == 0) {
            player.displayClientMessage(Component.translatable("message.starswillfall.guarda_sol.no_bloom"), true);
            return InteractionResult.FAIL;
        }

        level.playSound(null, center, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, center, SoundEvents.FLOWERING_AZALEA_PLACE, SoundSource.PLAYERS, 1.0F, 1.1F);
        stack.hurtAndBreak(GuardaSolTuning.BLOOM_DURABILITY_COST, player, LivingEntity.getSlotForHand(context.getHand()));
        player.getCooldowns().addCooldown(stack.getItem(), GuardaSolTuning.BLOOM_COOLDOWN_TICKS);
        return InteractionResult.sidedSuccess(false);
    }

    private static boolean canReplace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty() || state.getBlock() instanceof DoublePlantBlock) {
            return false;
        }
        return state.isAir() || state.is(BlockTags.REPLACEABLE);
    }
}
