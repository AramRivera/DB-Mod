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
 * at half health, transforms: untouchable for a moment, then bigger, golden and faster. The sprouts leap
 * and, beaten down, cling to you and blow up; the invader becomes a giant ape under the full moon; the
 * pink demon regenerates when left alone, body-slams and sheds smaller copies as it weakens.
 */
public class DBEnemy extends Monster {
    private static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(DBEnemy.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DBEnemy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRANSFORMING = SynchedEntityData.defineId(DBEnemy.class, EntityDataSerializers.INT);
    public static final int TRANSFORM_TICKS = 60;
    /** A sprout's fuse once it clings to you. */
    public static final int FUSE_TICKS = 30;

    private final EnemyKind kind;
    private final ServerBossEvent bossBar;
    private int sinceHurt, slamTicks, slamCooldown = 80, splits;

    public DBEnemy(EntityType<? extends DBEnemy> type, Level level, EnemyKind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = kind.getTp() / 4;
        this.bossBar = kind.isBoss() ? new ServerBossEvent(Component.literal(kind.getDisplayName()), switch (kind) {
            case MAJIN -> BossEvent.BossBarColor.PINK;
            case RAIDER -> BossEvent.BossBarColor.RED;
            default -> BossEvent.BossBarColor.PURPLE;
        }, BossEvent.BossBarOverlay.NOTCHED_10) : null;
        if (bossBar != null && kind == EnemyKind.RAIDER) bossBar.setVisible(false); // only once he is the ape
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
                .add(Attributes.KNOCKBACK_RESISTANCE, kind == EnemyKind.EMPEROR || kind == EnemyKind.MAJIN ? 0.8 : kind == EnemyKind.DINO ? 0.4 : 0.1);
    }

    public EnemyKind getKind() { return kind; }
    public boolean isAiming() { return entityData.get(AIMING); }
    /** 0 normally; 1 once the emperor has transformed or the invader became the ape. */
    public int getPhase() { return entityData.get(PHASE); }
    /** Ticks left in a transformation (or a sprout's fuse), 0 when none is under way. */
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
        if (kind == EnemyKind.SPROUT) goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(this, 0.45f));
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
                sl.sendParticles(kind == EnemyKind.SPROUT ? ParticleTypes.SMOKE : ParticleTypes.ELECTRIC_SPARK,
                        getX(), getY() + 1.5 * getScale(), getZ(), 10, 0.8 * getScale(), 1.2 * getScale(), 0.8 * getScale(), 0.2);
            }
            if (kind == EnemyKind.SPROUT && getTarget() != null) { // clinging on
                getNavigation().stop();
                setDeltaMovement(getTarget().position().subtract(position()).scale(0.25).add(0, getDeltaMovement().y, 0));
            }
            if (t == 1) finishTransformation();
            if (isRemoved()) return;
        }
        LivingEntity target = getTarget();
        switch (kind) {
            case SPROUT -> {
                if (t == 0 && target != null && distanceTo(target) < 2.2 && (getHealth() < getMaxHealth() * 0.5f || random.nextInt(80) == 0)) {
                    entityData.set(TRANSFORMING, FUSE_TICKS);
                    playSound(SoundEvents.CREEPER_PRIMED, 1f, 1.4f);
                }
            }
            case RAIDER -> {
                if (getPhase() == 0 && t == 0 && tickCount % 40 == 0 && underFullMoon()) {
                    entityData.set(TRANSFORMING, TRANSFORM_TICKS);
                    getNavigation().stop();
                    playSound(ModSounds.TRANSFORM.get(), 3f, 0.5f);
                    say("¡La luna llena! ¡Ahora verás el verdadero poder de mi raza!");
                }
            }
            case MAJIN -> {
                // Left alone, it puffs itself back together
                if (++sinceHurt > 60 && tickCount % 10 == 0 && getHealth() < getMaxHealth()) heal(getMaxHealth() * 0.01f);
                tickSlam(target);
            }
            default -> {}
        }
        if (bossBar != null) bossBar.setProgress(getHealth() / getMaxHealth());
    }

    private boolean underFullMoon() {
        return level().getMoonPhase() == 0 && !level().isDay() && level().canSeeSky(blockPosition().above());
    }

    /** The demon's body slam: a hop toward you and a shockwave where it lands. */
    private void tickSlam(LivingEntity target) {
        if (slamTicks > 0) {
            if (++slamTicks > 6 && onGround()) {
                slamTicks = 0;
                slamCooldown = 100 + random.nextInt(60);
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5),
                        e -> e != this && !(e instanceof DBEnemy other && (other.kind == EnemyKind.MAJIN || other.kind == EnemyKind.MAJIN_SPAWN)))) {
                    if (!e.hurt(damageSources().mobAttack(this), 10f)) continue;
                    Vec3 away = e.position().subtract(position()).multiply(1, 0, 1).normalize();
                    e.push(away.x * 1.2, 0.6, away.z * 1.2);
                    e.hurtMarked = true;
                }
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 8, 2.5, 0.2, 2.5, 0);
                    sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
                }
            }
            return;
        }
        if (--slamCooldown > 0 || target == null || !onGround() || distanceTo(target) > 7) return;
        Vec3 to = target.position().subtract(position()).multiply(1, 0, 1).scale(0.12);
        setDeltaMovement(to.x, 0.95, to.z);
        hasImpulse = true;
        slamTicks = 1;
    }

    /** Ends what TRANSFORMING was counting down: the emperor's form, the ape, or the sprout's blast. */
    private void finishTransformation() {
        switch (kind) {
            case SPROUT -> {
                level().explode(this, getX(), getY() + 0.5, getZ(), 2.6f, Level.ExplosionInteraction.NONE);
                discard();
            }
            case RAIDER -> becomeApe();
            default -> finishEmperor();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getTransforming() > 0 && kind != EnemyKind.SPROUT) return false; // nothing touches them while they transform
        boolean hurt = super.hurt(source, amount);
        if (!hurt || isDeadOrDying()) return hurt;
        sinceHurt = 0;
        if (kind == EnemyKind.EMPEROR && getPhase() == 0 && getHealth() <= getMaxHealth() / 2) startTransformation();
        if (kind == EnemyKind.MAJIN && splits < 2 && getHealth() < getMaxHealth() * (splits == 0 ? 0.66f : 0.33f)) split();
        return hurt;
    }

    private void startTransformation() {
        entityData.set(TRANSFORMING, TRANSFORM_TICKS);
        getNavigation().stop();
        playSound(ModSounds.TRANSFORM.get(), 3f, 0.6f);
        say("¿Crees que esto es todo mi poder? Te mostraré mi verdadera forma.");
    }

    /** Two small copies pop off the demon's body. */
    private void split() {
        splits++;
        say(splits == 1 ? "¡Jejeje! ¡Más Majin! ¡Más amigos para jugar!" : "¡Majin enfadado! ¡MUY enfadado!");
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < 2; i++) {
            DBEnemy small = com.dragonblock.common.registry.ModEntities.MAJIN_SPAWN.get().create(sl);
            if (small == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            small.moveTo(getX() + Math.cos(a) * 1.5, getY() + 0.5, getZ() + Math.sin(a) * 1.5, random.nextFloat() * 360f, 0f);
            small.setDeltaMovement(Math.cos(a) * 0.4, 0.4, Math.sin(a) * 0.4);
            small.setTarget(getTarget());
            sl.addFreshEntity(small);
        }
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 30, 1, 1, 1, 0.1);
        playSound(SoundEvents.SLIME_SQUISH, 2f, 0.6f);
    }

    /** Giant ape: three times the size and the health, a crushing punch, and a blast from the mouth. */
    private void becomeApe() {
        entityData.set(PHASE, 1);
        setAttr(Attributes.SCALE, kind.getScale() * 3.2);
        setAttr(Attributes.MAX_HEALTH, kind.getHealth() * 3);
        setAttr(Attributes.ATTACK_DAMAGE, kind.getDamage() * 2.2);
        setAttr(Attributes.MOVEMENT_SPEED, kind.getSpeed() * 0.85);
        setAttr(Attributes.KNOCKBACK_RESISTANCE, 0.9);
        setHealth(getMaxHealth());
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4f, 0.5f);
        }
        if (bossBar != null) {
            bossBar.setName(Component.literal(kind.getDisplayName() + " (Ozaru)"));
            bossBar.setVisible(true);
        }
    }

    private void setAttr(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        var inst = getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    private void finishEmperor() {
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
        switch (kind) {
            case EMPEROR -> say("Imposible... derrotado por... un simple guerrero...");
            case MAJIN -> say("Majin... cansado... Majin quiere... dormir...");
            case RAIDER -> { if (getPhase() == 1) say("Maldito... insecto... ¿cómo es posible...?"); }
            default -> {}
        }
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
            case DINO -> spawnAtLocation(new ItemStack(isOnFire() ? net.minecraft.world.item.Items.COOKED_BEEF : net.minecraft.world.item.Items.BEEF, 3 + random.nextInt(3)));
            case SPROUT -> spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 1 + random.nextInt(3)));
            case RAIDER -> {
                if (random.nextFloat() < 0.15f) spawnAtLocation(new ItemStack(ModItems.SCOUTER.get()));
                if (getPhase() == 1) {
                    spawnAtLocation(new ItemStack(ModItems.SENZU_BEAN.get(), 2));
                    spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
                }
            }
            case MAJIN -> {
                spawnAtLocation(new ItemStack(ModItems.SENZU_BEAN.get(), 5));
                spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 8));
                spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.COOKIE, 16));
            }
            case MAJIN_SPAWN -> spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.COOKIE, 1 + random.nextInt(2)));
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
        return kind != EnemyKind.EMPEROR && kind != EnemyKind.MAJIN && getPhase() == 0 && super.removeWhenFarAway(distance);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
        tag.putInt("Splits", splits);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHASE, tag.getInt("Phase"));
        splits = tag.getInt("Splits");
        if (bossBar != null && kind == EnemyKind.RAIDER) bossBar.setVisible(getPhase() == 1);
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
            return d > 4 * getScale() && d < 22 + 4 * getScale() && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return aim > 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            aim = kind.isBoss() ? 12 : 18;
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
            boolean heavy = kind == EnemyKind.EMPEROR || kind == EnemyKind.MAJIN || kind == EnemyKind.RAIDER;
            boolean big = heavy && (getPhase() == 1 || kind == EnemyKind.MAJIN) && random.nextInt(3) == 0;
            float damage = kind.getKiDamage() * (getPhase() == 1 ? 1.5f : 1f) * (big ? 1.8f : 1f);
            KiBlastEntity blast = new KiBlastEntity(level(), DBEnemy.this, damage, heavy ? (big ? 2.5f : 1f) : 0f,
                    kind.getKiColor(), (big ? 0.9f : 0.32f) * (getPhase() == 1 && kind == EnemyKind.RAIDER ? 2f : 1f), big ? Technique.BIG_BANG : Technique.KI_BLAST);
            blast.setPos(from.add(dir.normalize().scale(0.8 * getScale())));
            blast.shoot(dir.x, dir.y, dir.z, big ? 1.4f : 1.9f, kind == EnemyKind.ANDROID ? 0.5f : 2.5f);
            level().addFreshEntity(blast);
            playSound(ModSounds.KI_BLAST.get(), 1.2f, big ? 0.6f : 1.2f);
        }

        @Override
        public void stop() {
            entityData.set(AIMING, false);
            cooldown = kind.isBoss() ? (getPhase() == 1 ? 25 : 40) : 50 + random.nextInt(40);
        }
    }
}
