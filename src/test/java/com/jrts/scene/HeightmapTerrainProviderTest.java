package com.jrts.scene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HeightmapTerrainProviderTest {

    @Test
    void flatReturnsConstantHeightAndBounds() {
        HeightmapTerrainProvider terrain = HeightmapTerrainProvider.flat(100f, 0f);
        assertEquals(0f, terrain.getHeight(10, -20), 0.001f);
        assertEquals(-50f, terrain.getMapMinX(), 0.001f);
        assertEquals(50f, terrain.getMapMaxX(), 0.001f);
        assertTrue(terrain.isInBounds(0, 0));
        assertFalse(terrain.isInBounds(60, 0));
    }

    @Test
    void fromHeightsInterpolatesBilinearly() {
        // 2x2 grid: heights arranged so center bilinear = average.
        float[] heights = {0f, 10f, 10f, 20f};
        HeightmapTerrainProvider terrain =
                HeightmapTerrainProvider.fromHeights(20f, 10f, -100f, heights);

        assertEquals(0f, terrain.getHeight(-10, -10), 0.001f);
        assertEquals(20f, terrain.getHeight(10, 10), 0.001f);
        assertEquals(10f, terrain.getHeight(0, 0), 0.001f);
    }

    @Test
    void waterIsBelowWaterLevel() {
        HeightmapTerrainProvider terrain = HeightmapTerrainProvider.flat(100f, 0f);
        HeightmapTerrainProvider water =
                HeightmapTerrainProvider.fromHeights(20f, 10f, 5f, new float[]{0f, 0f, 0f, 0f});
        assertTrue(water.isWater(0, 0));
        assertFalse(terrain.isWater(0, 0));
    }

    @Test
    void gradientIsZeroOnFlatAndNonZeroOnSlope() {
        HeightmapTerrainProvider flat = HeightmapTerrainProvider.flat(100f, 0f);
        assertEquals(0f, flat.getGradient(0, 0), 0.001f);

        // 2x2 grid, height rises 10 over 10 world units along X.
        HeightmapTerrainProvider slope =
                HeightmapTerrainProvider.fromHeights(20f, 10f, -100f, new float[]{0f, 10f, 0f, 10f});
        assertTrue(slope.getGradient(0, 0) > 0.4f);
    }

    @Test
    void normalPointsUpOnFlat() {
        HeightmapTerrainProvider terrain = HeightmapTerrainProvider.flat(100f, 0f);
        var normal = terrain.getNormal(0, 0);
        assertEquals(0f, normal.x, 0.001f);
        assertEquals(1f, normal.y, 0.001f);
        assertEquals(0f, normal.z, 0.001f);
    }

    @Test
    void fromPngResamplesGrayscale(@TempDir Path tempDir) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x000000);
        image.setRGB(1, 0, 0xFFFFFF);
        image.setRGB(0, 1, 0x000000);
        image.setRGB(1, 1, 0xFFFFFF);
        Path png = tempDir.resolve("heightmap.png");
        ImageIO.write(image, "png", png.toFile());

        HeightmapTerrainProvider terrain =
                HeightmapTerrainProvider.fromPng(20f, 10f, -100f, png, 10f);

        assertEquals(0f, terrain.getHeight(-10, -10), 0.5f);
        assertEquals(10f, terrain.getHeight(10, 10), 0.5f);
        assertEquals(5f, terrain.getHeight(0, 0), 0.5f);
    }

    @Test
    void rejectsMismatchedHeightArray() {
        assertThrows(IllegalArgumentException.class,
                () -> HeightmapTerrainProvider.fromHeights(20f, 10f, -100f, new float[]{0f, 1f, 2f}));
    }
}
