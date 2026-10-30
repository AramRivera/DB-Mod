package com.dragonblock.client.renderer;

import com.dragonblock.common.entity.KiBlastEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;

public class KiBlastRenderer extends ThrownItemRenderer<KiBlastEntity> {
    public KiBlastRenderer(EntityRendererProvider.Context context) {
        super(context, 1.8f, true);
    }

    @Override
    protected int getBlockLightLevel(KiBlastEntity entity, BlockPos pos) {
        return 15; // Full bright glowing Ki energy
    }
}
