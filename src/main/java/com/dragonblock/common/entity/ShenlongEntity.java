package com.dragonblock.common.entity;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.item.ScouterItem;
import com.dragonblock.common.item.SenzuBeanItem;
import com.dragonblock.common.item.TrainingWeightsItem;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2COpenWishScreenPacket;
import com.dragonblock.common.registry.ModEntities;
import com.dragonblock.common.registry.ModItems;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class ShenlongEntity extends Entity {
    private final List<BlockPos> ballPositions = new ArrayList<>();
    private boolean wishGranted = false;
    private double baseY;

    public ShenlongEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();

        if (this.baseY == 0 && this.getY() > 0) {
            this.baseY = this.getY();
        }

        // Gentle floating hover
        double floatY = this.baseY + Math.sin(this.tickCount * 0.05) * 0.4;
        this.setPos(this.getX(), floatY, this.getZ());

        // Ambient particles around Shenlong
        if (this.level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                double px = this.getX() + (this.random.nextDouble() - 0.5) * 3.0;
                double py = this.getY() + this.random.nextDouble() * 3.0;
                double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 3.0;
                this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 0, 0.05, 0);
            }
            if (this.tickCount % 5 == 0) {
                this.level().addParticle(ParticleTypes.FLASH, this.getX(), this.getY() + 1.5, this.getZ(), 0, 0, 0);
            }
        } else {
            // Despawn after 5 minutes (6000 ticks) if summoner abandons
            if (this.tickCount > 6000) {
                dismissWithoutWish();
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.wishGranted && player instanceof ServerPlayer sp) {
            ModMessages.sendToPlayer(new S2COpenWishScreenPacket(this.getId()), sp);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void grantWish(int wishId, ServerPlayer player) {
        if (this.wishGranted || !(this.level() instanceof ServerLevel serverLevel)) return;
        this.wishGranted = true;

        PlayerDBCData data = PlayerDBCData.get(player);

        switch (wishId) {
            case 0 -> { // +50,000 TP
                data.addTrainingPoints(50000);
                player.displayClientMessage(
                        Component.literal("§a§l[SHENLONG] §e¡Te he otorgado 50,000 Puntos de Entrenamiento (TP)!")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        false
                );
            }
            case 1 -> { // +15 STR and +15 WIL
                data.setStr(data.getStr() + 15);
                data.setWil(data.getWil() + 15);
                player.displayClientMessage(
                        Component.literal("§a§l[SHENLONG] §e¡Tu Fuerza y Voluntad han aumentado en +15 permanentemente!")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        false
                );
            }
            case 2 -> { // +10 CON (+10 Corazones permanentes)
                data.setCon(data.getCon() + 10);
                player.displayClientMessage(
                        Component.literal("§a§l[SHENLONG] §e¡Tu Vitalidad ha aumentado en +10 (+10 Corazones permanentes)!")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                        false
                );
            }
            case 3 -> { // +100 Max Ki & Full Ki recharge
                data.setWil(data.getWil() + 10);
                data.setCurrentKi(data.getMaxKi());
                player.displayClientMessage(
                        Component.literal("§a§l[SHENLONG] §e¡Tu Ki Máximo ha crecido y tu energía divina está al 100%!")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                        false
                );
            }
            case 4 -> { // Divine Supplies
                player.getInventory().add(new ItemStack(ModItems.SENZU_BEAN.get(), 16));
                player.getInventory().add(new ItemStack(ModItems.SCOUTER.get(), 1));
                player.getInventory().add(new ItemStack(ModItems.TRAINING_WEIGHTS.get(), 1));
                player.displayClientMessage(
                        Component.literal("§a§l[SHENLONG] §e¡Aquí tienes las provisiones sagradas: 16 Semillas, Scouter y Pesas!")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                        false
                );
            }
            case 5 -> { // Potential Awakening
                data.addTrainingPoints(25000);
                if (data.getStr() < 20) data.setStr(20);
                if (data.getWil() < 20) data.setWil(20);
                player.displayClientMessage(
                        Component.literal("§a§l[SHENLONG] §e¡He despertado tu potencial latente para desbloquear transformaciones!")
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                        false
                );
            }
        }

        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        // Turn dragon balls into stone or scatter them
        for (BlockPos bp : this.ballPositions) {
            if (serverLevel.getBlockState(bp).getBlock() instanceof net.minecraft.world.level.block.Block) {
                serverLevel.setBlock(bp, Blocks.COBBLESTONE.defaultBlockState(), 3);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // Lightning strike at farewell
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (bolt != null) {
            bolt.moveTo(this.getX(), this.getY(), this.getZ());
            bolt.setVisualOnly(true);
            serverLevel.addFreshEntity(bolt);
        }

        // Farewell sounds & broadcast
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.WEATHER, 3.0f, 0.8f);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.TRANSFORM.get(), SoundSource.WEATHER, 2.0f, 1.2f);

        for (Player p : serverLevel.players()) {
            if (p.distanceToSqr(this.position()) < 64.0 * 64.0) {
                p.displayClientMessage(
                        Component.literal("§a§l[SHENLONG] §2¡¡TU DESEO HA SIDO CUMPLIDO!! ¡¡HASTA LA PRÓXIMA...!!")
                                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD),
                        false
                );
            }
        }

        // Clear weather
        serverLevel.setWeatherParameters(6000, 0, false, false);

        // Discard Shenlong
        this.discard();
    }

    private void dismissWithoutWish() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.setWeatherParameters(6000, 0, false, false);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.WEATHER, 2.0f, 0.5f);
        }
        this.discard();
    }

    public void setBallPositions(List<BlockPos> positions) {
        this.ballPositions.clear();
        this.ballPositions.addAll(positions);
    }

    public boolean isWishGranted() {
        return wishGranted;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.wishGranted = tag.getBoolean("WishGranted");
        this.baseY = tag.getDouble("BaseY");
        this.ballPositions.clear();
        if (tag.contains("Balls", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Balls", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag posTag = list.getCompound(i);
                this.ballPositions.add(new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("WishGranted", this.wishGranted);
        tag.putDouble("BaseY", this.baseY);
        ListTag list = new ListTag();
        for (BlockPos bp : this.ballPositions) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("X", bp.getX());
            posTag.putInt("Y", bp.getY());
            posTag.putInt("Z", bp.getZ());
            list.add(posTag);
        }
        tag.put("Balls", list);
    }
}
