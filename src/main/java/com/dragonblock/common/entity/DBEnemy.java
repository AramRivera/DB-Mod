package com.dragonblock.common.entity;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModItems;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A hostile fighter: punches up close, and the ki users aim (arm raised, so you can see it coming) and
 * fire blasts from range. Defeating one gives the killer training points. The emperor has a boss bar and,
 * at half health, transforms: untouchable for a moment, then bigger, golden and faster.
 */
public class DBEnemy extends Monster {
    private static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(DBEnemy.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DBEnemy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRANSFORMING = SynchedEntityData.defineId(DBEnemy.class, EntityDataSerializers.INT);
    public static final int TRANSFORM_TICKS = 60;

    private final EnemyKind kind;
    private final ServerBossEvent bossBar;

    public DBEnemy(EntityType<? extends DBEnemy> type, Level level, EnemyKind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = kind.getTp() / 4;
        this.bossBar = kind == EnemyKind.EMPEROR
                ? new ServerBossEvent(Component.literal(kind.getDisplayName()), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10)
                : null;
        addGoals(); // Mob's constructor calls registerGoals() before kind is set, so the goals go here
    }

    public static AttributeSupplier.Builder attributes(EnemyKind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.getHealth())
                .add(Attributes.ATTACK_DAMAGE, kind.getDamage())
                .add(Attributes.MOVEMENT_SPEED, kind.getSpeed())
                .add(Attributes.ARMOR, kind.getArmor())
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.SCALE, kind.getScale())
                .add(Attributes.KNOCKBACK_RESISTANCE, kind == EnemyKind.EMPEROR ? 0.8 : 0.1);
    }

    public EnemyKind getKind() { return kind; }
    public boolean isAiming() { return entityData.get(AIMING); }
    /** 0 normally; 1 once the emperor has transformed. */
    public int getPhase() { return entityData.get(PHASE); }
    /** Ticks left in the emperor's transformation, 0 when not transforming. */
    public int getTransforming() { return entityData.get(TRANSFORMING); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AIMING, false);
        builder.define(PHASE, 0);
        builder.define(TRANSFORMING, 0);
    }

    @Override
    protected void registerGoals() {}

    private void addGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (kind.shoots()) goalSelector.addGoal(2, new KiShotGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        int t = getTransforming();
        if (t > 0) {
            entityData.set(TRANSFORMING, t - 1);
            if (level() instanceof ServerLevel sl && t % 4 == 0) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.5, getZ(), 10, 0.8, 1.2, 0.8, 0.2);
            }
            if (t == 1) finishTransformation();
        }
        if (bossBar != null) bossBar.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getTransforming() > 0) return false; // nothing touches him while he transforms
        boolean hurt = super.hurt(source, amount);
        if (hurt && kind == EnemyKind.EMPEROR && getPhase() == 0 && getHealth() <= getMaxHealth() / 2 && !isDeadOrDying()) startTransformation();
        return hurt;
    }

    private void startTransformation() {
        entityData.set(TRANSFORMING, TRANSFORM_TICKS);
        getNavigation().stop();
        playSound(ModSounds.TRANSFORM.get(), 3f, 0.6f);
        say("¿Crees que esto es todo mi poder? Te mostraré mi verdadera forma.");
    }

    private void finishTransformation() {
        entityData.set(PHASE, 1);
        setHealth(getMaxHealth() * 0.75f);
        var scale = getAttribute(Attributes.SCALE);
        if (scale != null) scale.setBaseValue(kind.getScale() * 1.35);
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(kind.getDamage() * 1.6);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(kind.getSpeed() * 1.25);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3f, 0.7f);
        }
        if (bossBar != null) bossBar.setColor(BossEvent.BossBarColor.YELLOW);
    }

    private void say(String line) {
        Component msg = Component.literal("[" + kind.getDisplayName() + "] ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal(line).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (level() instanceof ServerLevel sl) for (ServerPlayer p : sl.players()) if (p.distanceTo(this) < 64) p.sendSystemMessage(msg);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        if (source.getEntity() instanceof ServerPlayer killer) {
            PlayerDBCData data = PlayerDBCData.get(killer);
            data.addTrainingPoints(kind.getTp());
            data.saveToPlayer(killer);
            ModMessages.syncToPlayer(killer);
        }
        if (kind == EnemyKind.EMPEROR) say("Imposible... derrotado por... un simple guerrero...");
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        switch (kind) {
            case SOLDIER -> {
                if (random.nextFloat() < 0.06f) spawnAtLocation(new ItemStack(ModItems.IMPERIAL_BEACON.get()));
                if (random.nextFloat() < 0.08f) spawnAtLocation(new ItemStack(ModItems.SCOUTER.get()));
            }
            case ANDROID -> spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 2 + random.nextInt(4)));
            case EMPEROR -> {
                spawnAtLocation(new ItemStack(ModItems.SENZU_BEAN.get(), 3));
                spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 6));
            }
            default -> {}
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bossBar != null) bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossBar != null) bossBar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return kind != EnemyKind.EMPEROR && super.removeWhenFarAway(distance);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHASE, tag.getInt("Phase"));
    }

    /** Stops, raises an arm for a beat, then fires a ki blast at the target; the emperor fires faster and bigger. */
    private final class KiShotGoal extends Goal {
        private int cooldown = 40, aim = 0;

        KiShotGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || getTransforming() > 0 || --cooldown > 0) return false;
            double d = distanceTo(target);
            return d > 4 && d < 22 && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return aim > 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            aim = kind == EnemyKind.EMPEROR ? 12 : 18;
            getNavigation().stop();
            entityData.set(AIMING, true);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30f, 30f);
            if (--aim > 0) return;
            Vec3 from = getEyePosition();
            Vec3 dir = target.getEyePosition().subtract(0, 0.3, 0).subtract(from);
            boolean big = kind == EnemyKind.EMPEROR && getPhase() == 1 && random.nextInt(3) == 0;
            float damage = kind.getKiDamage() * (getPhase() == 1 ? 1.5f : 1f) * (big ? 1.8f : 1f);
            KiBlastEntity blast = new KiBlastEntity(level(), DBEnemy.this, damage, kind == EnemyKind.EMPEROR ? (big ? 2.5f : 1f) : 0f,
                    kind.getKiColor(), big ? 0.9f : 0.32f, big ? Technique.BIG_BANG : Technique.KI_BLAST);
            blast.setPos(from.add(dir.normalize().scale(0.8)));
            blast.shoot(dir.x, dir.y, dir.z, big ? 1.4f : 1.9f, kind == EnemyKind.ANDROID ? 0.5f : 2.5f);
            level().addFreshEntity(blast);
            playSound(ModSounds.KI_BLAST.get(), 1.2f, big ? 0.6f : 1.2f);
        }

        @Override
        public void stop() {
            entityData.set(AIMING, false);
            cooldown = kind == EnemyKind.EMPEROR ? (getPhase() == 1 ? 25 : 40) : 50 + random.nextInt(40);
        }
    }
}
