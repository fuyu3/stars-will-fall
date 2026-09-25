package com.fuyu.starswillfall.scythe;

import com.fuyu.starswillfall.entity.ModEntities;
import com.fuyu.starswillfall.util.StarEffects;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A foice arremessada. Vai em frente freando, volta curvando até o dono e, ao chegar, é devolvida
 * ao inventário (no mesmo espaço de onde saiu, se estiver livre). Atravessa os alvos e acerta cada
 * um uma vez na ida e uma vez na volta.
 *
 * <p>O servidor manda em tudo (movimento, acertos, devolução); o cliente só repete o movimento a
 * partir da velocidade sincronizada e desenha o item girando (ver ScytheProjectileRenderer).
 * A foice fica guardada dentro da entidade, inclusive no save do mundo. Se o dono sumir, morrer
 * ou mudar de dimensão, ela cai no chão como item.
 */
public class ScytheProjectile extends Projectile {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ScytheProjectile.class, EntityDataSerializers.ITEM_STACK);

    private final Set<Integer> alreadyHit = new HashSet<>();
    private boolean returning;
    private int flightTicks;
    private int returnTicks;
    private int originSlot = -1;

    public ScytheProjectile(EntityType<? extends ScytheProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    /** Cria a foice já saindo da frente dos olhos do jogador, na direção do olhar. */
    public static ScytheProjectile create(Level level, Player owner, ItemStack scythe, int originSlot) {
        ScytheProjectile projectile = new ScytheProjectile(ModEntities.SCYTHE_PROJECTILE.get(), level);
        Vec3 look = owner.getLookAngle();
        projectile.setOwner(owner);
        projectile.setItem(scythe);
        projectile.originSlot = originSlot;
        projectile.setPos(owner.getEyePosition().add(look.scale(0.6D)).subtract(0.0D, 0.25D, 0.0D));
        projectile.setDeltaMovement(look.scale(ScytheTuning.BOOMERANG_SPEED));
        return projectile;
    }

    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(DATA_ITEM, stack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    // ---- Tick ------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            // Cliente: só repete o movimento; o servidor corrige posição e velocidade.
            this.setPos(this.position().add(this.getDeltaMovement()));
            return;
        }

        if (this.getItem().isEmpty()) {
            this.discard();
            return;
        }
        Entity owner = this.getOwner();
        this.flightTicks++;
        if (this.flightTicks > ScytheTuning.BOOMERANG_MAX_LIFETIME_TICKS) {
            this.deliver(level, owner);
            return;
        }

        if (this.returning) {
            if (!this.tickReturning(level, owner)) {
                return;
            }
        } else {
            this.tickOutgoing(level);
        }

        this.hitEntities(level, owner);
        if (this.isRemoved()) {
            return;
        }

        StarEffects.trail(level, this.position());
        if (this.flightTicks % 4 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    private void tickOutgoing(ServerLevel level) {
        double progress = Math.min(1.0D, this.flightTicks / (double) ScytheTuning.BOOMERANG_OUT_TICKS);
        double speed = ScytheTuning.BOOMERANG_SPEED * (1.0D - (1.0D - ScytheTuning.BOOMERANG_END_SPEED_FACTOR) * progress);
        Vec3 velocity = this.getDeltaMovement();
        velocity = velocity.lengthSqr() < 1.0E-6D ? Vec3.ZERO : velocity.normalize().scale(speed);

        Vec3 from = this.position();
        Vec3 to = from.add(velocity);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) {
            this.setPos(hit.getLocation());
            StarEffects.burst(level, hit.getLocation(), 8);
            this.startReturning(level);
            return;
        }

        this.setDeltaMovement(velocity);
        this.setPos(to);
        if (this.flightTicks >= ScytheTuning.BOOMERANG_OUT_TICKS || this.getY() < level.getMinBuildHeight() + 1) {
            this.startReturning(level);
        }
    }

    /** @return false se a foice foi entregue/descartada neste tick. */
    private boolean tickReturning(ServerLevel level, Entity owner) {
        if (owner == null || !owner.isAlive() || owner.level() != level) {
            this.dropAsItem(level);
            return false;
        }
        this.returnTicks++;

        Vec3 toOwner = owner.position().add(0.0D, owner.getBbHeight() * 0.6D, 0.0D).subtract(this.position());
        double distance = toOwner.length();
        if (distance < ScytheTuning.BOOMERANG_CATCH_DISTANCE) {
            this.deliver(level, owner);
            return false;
        }

        double speed = Math.min(ScytheTuning.BOOMERANG_RETURN_MAX_SPEED,
                ScytheTuning.BOOMERANG_RETURN_START_SPEED + ScytheTuning.BOOMERANG_RETURN_ACCEL * this.returnTicks);
        Vec3 desired = toOwner.scale(Math.min(speed, distance) / distance);
        Vec3 velocity = this.getDeltaMovement().add(desired.subtract(this.getDeltaMovement()).scale(ScytheTuning.BOOMERANG_RETURN_STEER));
        this.setDeltaMovement(velocity);
        this.setPos(this.position().add(velocity)); // na volta atravessa blocos, para nunca ficar presa
        return true;
    }

    private void startReturning(ServerLevel level) {
        this.returning = true;
        this.returnTicks = 0;
        this.alreadyHit.clear(); // cada alvo pode ser acertado de novo na volta
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    // ---- Acertos ---------------------------------------------------------------------------------

    private void hitEntities(ServerLevel level, Entity owner) {
        // A caixa cobre o caminho percorrido neste tick (a foice já andou, então estende para trás).
        AABB area = this.getBoundingBox().expandTowards(this.getDeltaMovement().reverse()).inflate(0.4D);
        for (Entity entity : level.getEntities(this, area, e -> this.canHit(e, owner))) {
            if (this.alreadyHit.add(entity.getId()) && entity instanceof LivingEntity target) {
                this.hurtTarget(level, owner, target);
                if (this.isRemoved()) {
                    return;
                }
            }
        }
    }

    private boolean canHit(Entity target, Entity owner) {
        if (!(target instanceof LivingEntity) || target == owner || !target.isAlive()
                || target.isSpectator() || !target.isAttackable()) {
            return false;
        }
        if (owner != null && target instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) {
            return false;
        }
        return !(owner instanceof Player attacker && target instanceof Player victim && !attacker.canHarmPlayer(victim));
    }

    private void hurtTarget(ServerLevel level, Entity owner, LivingEntity target) {
        ItemStack scythe = this.getItem();
        DamageSource source = level.damageSources().thrown(this, owner);
        float damage = EnchantmentHelper.modifyDamage(level, scythe, target, source, ScytheTuning.BOOMERANG_DAMAGE);
        if (!target.hurt(source, damage)) {
            return;
        }
        EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, scythe);
        StarEffects.burst(level, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), 10);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 1.1F);

        ServerPlayer player = owner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        scythe.hurtAndBreak(ScytheTuning.BOOMERANG_HIT_DURABILITY, level, player, item -> { });
        if (scythe.isEmpty()) {
            level.playSound(null, this.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.8F);
            this.discard();
        }
    }

    // ---- Devolução -------------------------------------------------------------------------------

    /** Entrega a foice ao dono; se ele não puder receber, ela cai no chão. */
    private void deliver(ServerLevel level, Entity owner) {
        if (!(owner instanceof Player player) || !player.isAlive() || player.level() != level) {
            this.dropAsItem(level);
            return;
        }
        ItemStack scythe = this.getItem();
        Inventory inventory = player.getInventory();
        boolean placed;
        if (this.originSlot >= 0 && this.originSlot < inventory.getContainerSize()
                && inventory.getItem(this.originSlot).isEmpty()) {
            inventory.setItem(this.originSlot, scythe);
            placed = true;
        } else {
            placed = inventory.add(scythe);
        }
        if (!placed) { // inventário cheio: cai aos pés do jogador
            ItemEntity drop = new ItemEntity(level, player.getX(), player.getY() + 0.5D, player.getZ(), scythe);
            drop.setNoPickUpDelay();
            level.addFreshEntity(drop);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.3F);
        StarEffects.burst(level, player.position().add(0.0D, 1.0D, 0.0D), 8);
        this.setItem(ItemStack.EMPTY);
        this.discard();
    }

    private void dropAsItem(ServerLevel level) {
        ItemEntity drop = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), this.getItem());
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
        this.setItem(ItemStack.EMPTY);
        this.discard();
    }

    // ---- Save ------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Scythe", this.getItem().saveOptional(this.registryAccess()));
        tag.putBoolean("Returning", this.returning);
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putInt("ReturnTicks", this.returnTicks);
        tag.putInt("OriginSlot", this.originSlot);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setItem(ItemStack.parseOptional(this.registryAccess(), tag.getCompound("Scythe")));
        this.returning = tag.getBoolean("Returning");
        this.flightTicks = tag.getInt("FlightTicks");
        this.returnTicks = tag.getInt("ReturnTicks");
        this.originSlot = tag.getInt("OriginSlot");
    }
}
