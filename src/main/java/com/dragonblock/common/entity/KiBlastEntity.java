package com.dragonblock.common.entity;

import com.dragonblock.common.registry.ModEntities;
import com.dragonblock.common.registry.ModItems;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class KiBlastEntity extends ThrowableItemProjectile {
    private float damage = 8.0f;
    private float explosionPower = 1.5f;

    public KiBlastEntity(EntityType<? extends KiBlastEntity> entityType, Level level) {
        super(entityType, level);
    }

    public KiBlastEntity(Level level, LivingEntity shooter, float damage, float explosionPower) {
        super(ModEntities.KI_BLAST.get(), shooter, level);
        this.damage = damage;
        this.explosionPower = explosionPower;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.KI_BLAST_ITEM.get();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Glowing trail particles
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                        0.0, 0.0, 0.0
                );
            }
        }

        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.hurt(this.damageSources().mobAttack(this.getOwner() instanceof LivingEntity l ? l : null), this.damage);
            }
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            explode();
        }
    }

    private void explode() {
        if (!this.level().isClientSide) {
            this.level().explode(
                    this,
                    this.getX(), this.getY(), this.getZ(),
                    this.explosionPower,
                    Level.ExplosionInteraction.MOB
            );
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.KI_EXPLODE.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
            this.discard();
        }
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getExplosionPower() {
        return explosionPower;
    }

    public void setExplosionPower(float explosionPower) {
        this.explosionPower = explosionPower;
    }
}
