package com.phasetranscrystal.fpsmatch.common.mapselect.thumbnail;

import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class MapThumbnailSampler {
    /** Packed colour byte for an empty (no solid block found) column. */
    public static final int EMPTY = 0;

    private MapThumbnailSampler() {
    }

    public record Sample(int width, int height, byte[] pixels) {
    }

    /**
     * Sample a top-down, vanilla-map-style colour image of {@code area}. Each output pixel is the
     * packed {@link MapColor} id of the highest non-air, non-{@link MapColor#NONE} block in that
     * column, downsized to at most {@link ThumbnailGeometry#MAX_DIMENSION} pixels on the longer side.
     * Must be called on the server thread.
     */
    public static Sample sample(ServerLevel level, AreaData area) {
        if (level == null || area == null) {
            return new Sample(0, 0, new byte[0]);
        }
        BlockPos p1 = area.pos1();
        BlockPos p2 = area.pos2();
        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        int srcW = maxX - minX + 1;
        int srcH = maxZ - minZ + 1;
        if (srcW < 1 || srcH < 1) {
            return new Sample(0, 0, new byte[0]);
        }

        int[] out = ThumbnailGeometry.computeOutput(srcW, srcH, ThumbnailGeometry.MAX_DIMENSION);
        int outW = out[0];
        int outH = out[1];
        byte[] pixels = new byte[outW * outH];

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int oy = 0; oy < outH; oy++) {
            int z = ThumbnailGeometry.mapOutputToSource(oy, outH, minZ, srcH);
            for (int ox = 0; ox < outW; ox++) {
                int x = ThumbnailGeometry.mapOutputToSource(ox, outW, minX, srcW);
                pixels[oy * outW + ox] = (byte) sampleColumn(level, pos, x, z, minY, maxY);
            }
        }
        return new Sample(outW, outH, pixels);
    }

    private static int sampleColumn(ServerLevel level, BlockPos.MutableBlockPos pos,
                                    int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            MapColor color = state.getMapColor(level, pos);
            if (color == MapColor.NONE) {
                continue;
            }
            return color.getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
        }
        return EMPTY;
    }
}