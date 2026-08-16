package com.jrts.scene;

import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A {@link TerrainHeightProvider} backed by a regular height grid.
 *
 * <p>Height is sampled with bilinear interpolation; normals and gradients use central
 * differences. Heights can originate from an inline float array or a grayscale PNG
 * (resampled onto the grid). A single-cell grid yields a flat terrain, so this class
 * supersedes {@link FlatTerrainHeightProvider} for map loading.
 */
public class HeightmapTerrainProvider implements TerrainHeightProvider {

    private static final Logger log = LoggerFactory.getLogger(HeightmapTerrainProvider.class);

    private final float minX;
    private final float maxX;
    private final float minZ;
    private final float maxZ;
    private final int width;
    private final int height;
    private final float cellSize;
    private final float[] heights;
    private final float waterLevel;

    public HeightmapTerrainProvider(float minX, float maxX, float minZ, float maxZ,
                                    int width, int height, float cellSize,
                                    float[] heights, float waterLevel) {
        if (heights == null || heights.length != width * height) {
            throw new IllegalArgumentException("heights length " + (heights == null ? 0 : heights.length)
                    + " does not match " + width + "x" + height + " grid");
        }
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.heights = heights;
        this.waterLevel = waterLevel;
        log.info("Created HeightmapTerrainProvider: {}x{} cells, cellSize={}, bounds=[{},{}]x[{},{}]",
                width, height, cellSize, minX, maxX, minZ, maxZ);
    }

    /**
     * Flat terrain of the given size (world units per side) at a constant height.
     */
    public static HeightmapTerrainProvider flat(float size, float height) {
        return new HeightmapTerrainProvider(-size / 2f, size / 2f, -size / 2f, size / 2f,
                1, 1, size, new float[]{height}, -Float.MAX_VALUE);
    }

    /**
     * Height grid from an inline row-major array. The map is square ({@code size} world
     * units) centered on the origin; the grid dimension is {@code ceil(size / cellSize)}.
     */
    public static HeightmapTerrainProvider fromHeights(float size, float cellSize,
                                                       float waterLevel, float[] heights) {
        int dim = (int) Math.ceil(size / cellSize);
        return new HeightmapTerrainProvider(-size / 2f, size / 2f, -size / 2f, size / 2f,
                dim, dim, cellSize, heights, waterLevel);
    }

    /**
     * Height grid resampled from a grayscale PNG. Each pixel's red channel (0..1) is scaled
     * by {@code verticalScale}; the image is stretched to cover the whole map.
     */
    public static HeightmapTerrainProvider fromPng(float size, float cellSize, float waterLevel,
                                                   Path pngPath, float verticalScale) {
        BufferedImage image;
        try {
            image = ImageIO.read(pngPath.toFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read heightmap: " + pngPath, e);
        }
        if (image == null) {
            throw new IllegalArgumentException("Unsupported heightmap image: " + pngPath);
        }

        int dim = (int) Math.ceil(size / cellSize);
        float[] heights = new float[dim * dim];
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                float u = (x + 0.5f) / dim;
                float v = (z + 0.5f) / dim;
                heights[z * dim + x] = sampleGray(image, u, v) * verticalScale;
            }
        }
        return new HeightmapTerrainProvider(-size / 2f, size / 2f, -size / 2f, size / 2f,
                dim, dim, cellSize, heights, waterLevel);
    }

    @Override
    public float getHeight(float worldX, float worldZ) {
        float gx = (worldX - minX) / cellSize - 0.5f;
        float gz = (worldZ - minZ) / cellSize - 0.5f;
        int x0 = (int) Math.floor(gx);
        int z0 = (int) Math.floor(gz);
        float tx = gx - x0;
        float tz = gz - z0;

        float h00 = heightAt(x0, z0);
        float h10 = heightAt(x0 + 1, z0);
        float h01 = heightAt(x0, z0 + 1);
        float h11 = heightAt(x0 + 1, z0 + 1);

        float hTop = h00 + (h10 - h00) * tx;
        float hBottom = h01 + (h11 - h01) * tx;
        return hTop + (hBottom - hTop) * tz;
    }

    @Override
    public Vector3f getNormal(float worldX, float worldZ) {
        float hL = getHeight(worldX - cellSize, worldZ);
        float hR = getHeight(worldX + cellSize, worldZ);
        float hD = getHeight(worldX, worldZ - cellSize);
        float hU = getHeight(worldX, worldZ + cellSize);
        Vector3f normal = new Vector3f(hL - hR, 2f * cellSize, hD - hU);
        if (normal.lengthSquared() == 0f) {
            return new Vector3f(Vector3f.UNIT_Y);
        }
        return normal.normalizeLocal();
    }

    @Override
    public boolean isWater(float worldX, float worldZ) {
        return getHeight(worldX, worldZ) <= waterLevel;
    }

    @Override
    public float getGradient(float worldX, float worldZ) {
        float hL = getHeight(worldX - cellSize, worldZ);
        float hR = getHeight(worldX + cellSize, worldZ);
        float hD = getHeight(worldX, worldZ - cellSize);
        float hU = getHeight(worldX, worldZ + cellSize);
        float gx = (hR - hL) / (2f * cellSize);
        float gz = (hU - hD) / (2f * cellSize);
        return (float) Math.sqrt(gx * gx + gz * gz);
    }

    @Override
    public boolean isInBounds(float worldX, float worldZ) {
        return worldX >= minX && worldX <= maxX && worldZ >= minZ && worldZ <= maxZ;
    }

    @Override
    public float getMapMinX() {
        return minX;
    }

    @Override
    public float getMapMaxX() {
        return maxX;
    }

    @Override
    public float getMapMinZ() {
        return minZ;
    }

    @Override
    public float getMapMaxZ() {
        return maxZ;
    }

    @Override
    public float getWaterLevel() {
        return waterLevel;
    }

    private float heightAt(int x, int z) {
        int cx = Math.max(0, Math.min(width - 1, x));
        int cz = Math.max(0, Math.min(height - 1, z));
        return heights[cz * width + cx];
    }

    private static float sampleGray(BufferedImage image, float u, float v) {
        float px = u * image.getWidth() - 0.5f;
        float py = v * image.getHeight() - 0.5f;
        int x0 = (int) Math.floor(px);
        int y0 = (int) Math.floor(py);
        float tx = px - x0;
        float ty = py - y0;

        float g00 = grayAt(image, x0, y0);
        float g10 = grayAt(image, x0 + 1, y0);
        float g01 = grayAt(image, x0, y0 + 1);
        float g11 = grayAt(image, x0 + 1, y0 + 1);

        float gTop = g00 + (g10 - g00) * tx;
        float gBottom = g01 + (g11 - g01) * tx;
        return gTop + (gBottom - gTop) * ty;
    }

    private static float grayAt(BufferedImage image, int x, int y) {
        int cx = Math.max(0, Math.min(image.getWidth() - 1, x));
        int cy = Math.max(0, Math.min(image.getHeight() - 1, y));
        return (image.getRGB(cx, cy) & 0xFF) / 255f;
    }
}
