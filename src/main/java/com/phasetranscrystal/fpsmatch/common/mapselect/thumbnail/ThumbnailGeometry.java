package com.phasetranscrystal.fpsmatch.common.mapselect.thumbnail;

public final class ThumbnailGeometry {
    public static final int MAX_DIMENSION = 128;

    private ThumbnailGeometry() {
    }

    /**
     * Compute the downsampled output size in pixels for a source area of {@code srcW} x {@code srcH}
     * blocks. The longer side is capped at {@code maxDim}; the aspect ratio is preserved and sources
     * are never upscaled.
     *
     * @return {@code [width, height]} with both dimensions at least 1, or {@code [0, 0]} if the
     *         source is degenerate.
     */
    public static int[] computeOutput(int srcW, int srcH, int maxDim) {
        if (maxDim < 1) {
            throw new IllegalArgumentException("maxDim must be positive");
        }
        if (srcW < 1 || srcH < 1) {
            return new int[]{0, 0};
        }
        int longer = Math.max(srcW, srcH);
        if (longer <= maxDim) {
            return new int[]{srcW, srcH};
        }
        int w = Math.max(1, (int) Math.round((double) srcW * maxDim / longer));
        int h = Math.max(1, (int) Math.round((double) srcH * maxDim / longer));
        return new int[]{w, h};
    }

    /**
     * Nearest-neighbour source coordinate (inclusive) for a downsampled output coordinate.
     */
    public static int mapOutputToSource(int outCoord, int outSize, int srcMin, int srcSize) {
        if (outSize <= 0 || srcSize <= 0) {
            return srcMin;
        }
        return srcMin + (int) ((long) outCoord * srcSize / outSize);
    }

    public record Rect(int x, int y, int width, int height) {
    }

    /**
     * Aspect-fit rectangle that centers an {@code imgW} x {@code imgH} image inside a
     * {@code boxW} x {@code boxH} box without distortion.
     */
    public static Rect aspectFit(int imgW, int imgH, int boxW, int boxH) {
        if (imgW <= 0 || imgH <= 0 || boxW <= 0 || boxH <= 0) {
            return new Rect(0, 0, Math.max(0, boxW), Math.max(0, boxH));
        }
        double scale = Math.min((double) boxW / imgW, (double) boxH / imgH);
        int w = Math.max(1, (int) Math.round(imgW * scale));
        int h = Math.max(1, (int) Math.round(imgH * scale));
        return new Rect((boxW - w) / 2, (boxH - h) / 2, w, h);
    }
}