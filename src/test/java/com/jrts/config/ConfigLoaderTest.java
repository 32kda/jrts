package com.jrts.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    private ConfigLoader loader;

    @BeforeEach
    void setUp() {
        Path configDir = Path.of("src/test/resources/config");
        loader = new ConfigLoader(configDir);
    }

    @Test
    void loadsHeavyTankConfigCorrectly() {
        UnitConfig cfg = loader.loadUnitConfig("heavy_tank_example");
        assertEquals("HeavyTank", cfg.identity().name());
        assertEquals("Heavy Tank", cfg.identity().displayName());
        assertEquals("AFV", cfg.identity().category());
        assertEquals("Republic", cfg.identity().owner());
        assertTrue(cfg.identity().selectable());

        assertEquals(400, cfg.stats().strength());
        assertEquals("heavy", cfg.stats().armor());
        assertEquals(6, cfg.stats().sight());
        assertEquals(5.0f, cfg.stats().speed(), 0.001f);
        assertEquals(5, cfg.stats().rot(), 0.001f);
        assertEquals(1000, cfg.stats().cost());
        assertEquals(25, cfg.stats().points());
        assertEquals(7, cfg.stats().techLevel());

        assertEquals(2, cfg.prerequisites().size());
        assertTrue(cfg.prerequisites().contains("barracks"));
        assertEquals(2, cfg.buildingsRequired());

        assertTrue(cfg.veterancy().trainable());
        assertEquals(4, cfg.veterancy().veteranAbilities().size());
        assertEquals(3, cfg.veterancy().eliteAbilities().size());

        assertEquals("125mm_cannon", cfg.combat().primaryWeapon());
        assertEquals("coaxial_mg", cfg.combat().secondaryWeapon());
        assertEquals(-1, cfg.combat().ammo());

        assertEquals("tracks", cfg.movement().locomotor());
        assertTrue(cfg.movement().crusher());
        assertFalse(cfg.movement().crushable());

        assertTrue(cfg.turrets().turret());
        assertFalse(cfg.turrets().turretSpins());
        assertEquals(5.0f, cfg.turrets().turretRotationSpeed(), 0.001f);
        assertEquals(4.0f, cfg.turrets().barrelSpeed(), 0.001f);
        assertEquals(2, cfg.turrets().turretRotationYaw().size());
        assertEquals(2, cfg.turrets().barrelElevationPitch().size());

        assertEquals(-180f, cfg.turrets().turretRotationYaw().get(0), 0.001f);
        assertEquals(180f, cfg.turrets().turretRotationYaw().get(1), 0.001f);

        assertFalse(cfg.isStructure());
    }

    @Test
    void loadsWeaponConfigCorrectly() {
        WeaponConfig cfg = loader.loadWeaponConfig("weapon_example");
        assertEquals("155mm_howitzer", cfg.name());
        assertEquals("projectile", cfg.type());
        assertEquals(150.0f, cfg.damage(), 0.001f);
        assertEquals("explosive", cfg.damageType());
        assertTrue(cfg.areaDamage());
        assertEquals(3.0f, cfg.explosionRadius(), 0.001f);
        assertEquals(25.0f, cfg.range(), 0.001f);
        assertEquals(4.0f, cfg.cooldown(), 0.001f);
        assertEquals(5.0f, cfg.minRange(), 0.001f);
        assertEquals(0.95f, cfg.accuracy(), 0.001f);
    }

    @Test
    void loadsBuildingConfigCorrectly() {
        BuildingConfig cfg = loader.loadBuildingConfig("building_example");
        assertEquals("War Factory", cfg.identity().name());
        assertEquals("building", cfg.identity().type());
        assertEquals("production", cfg.identity().category());

        assertEquals(1200, cfg.stats().health());
        assertEquals("heavy", cfg.stats().armorType());
        assertEquals(4, cfg.stats().sightRange());

        assertEquals(5, cfg.production().queueSize());
        assertEquals(4, cfg.production().produces().size());
        assertTrue(cfg.production().produces().contains("heavy_tank"));

        assertNotNull(cfg.defenseWeapon());
        assertEquals("rocket_pod", cfg.defenseWeapon().name());
        assertEquals(45f, cfg.defenseWeapon().damage(), 0.001f);
        assertEquals(14f, cfg.defenseWeapon().range(), 0.001f);

        assertTrue(cfg.docking().enabled());
        assertEquals("refinery", cfg.docking().dockType());
    }

    @Test
    void missingFileThrowsConfigParseException() {
        assertThrows(ConfigParseException.class,
                () -> loader.loadUnitConfig("nonexistent_unit"));
    }

    @Test
    void batchLoadReturnsUnits() {
        var configs = loader.loadAllUnitConfigs();
        assertNotNull(configs);
        // At least the heavy_tank_example should be there
        // (if the directory structure matches)
    }
}
