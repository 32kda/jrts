package com.jrts.map;

/**
 * Terrain section of a map file. Bounds are implicit: the map is square, {@code size}
 * world units per side, centered on the origin.
 *
 * <p>Heights come from exactly one source:
 * <ul>
 *   <li>{@code heights} — an inline row-major float array ({@code dim} x {@code dim},
 *       where {@code dim = ceil(size / cellSize)})</li>
 *   <li>{@code heightmap} — an external grayscale PNG whose pixels are resampled onto the
 *       height grid; {@code verticalScale} multiplies the normalized (0..1) sample</li>
 *   <li>neither — a flat map at height 0</li>
 * </ul>
 */
public record MapTerrain(
        Float size,
        Float cellSize,
        Float waterLevel,
        float[] heights,
        String heightmap,
        Float verticalScale) {

    public static final float DEFAULT_SIZE = 1000f;
    public static final float DEFAULT_CELL_SIZE = 1f;
    public static final float DEFAULT_WATER_LEVEL = -1000f;
    public static final float DEFAULT_VERTICAL_SCALE = 1f;

    public MapTerrain {
        if (size == null) {
            size = DEFAULT_SIZE;
        }
        if (cellSize == null) {
            cellSize = DEFAULT_CELL_SIZE;
        }
        if (waterLevel == null) {
            waterLevel = DEFAULT_WATER_LEVEL;
        }
        if (verticalScale == null) {
            verticalScale = DEFAULT_VERTICAL_SCALE;
        }
    }

    /**
     * @return true if heights are provided inline
     */
    public boolean hasInlineHeights() {
        return heights != null && heights.length > 0;
    }

    /**
     * @return true if an external heightmap image is referenced
     */
    public boolean hasHeightmap() {
        return heightmap != null && !heightmap.isBlank();
    }

    /**
     * @return number of height-grid cells per side (square grid)
     */
    public int gridDimension() {
        return (int) Math.ceil(size / cellSize);
    }
}
