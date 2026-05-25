package com.jrts.config;

import com.moandjiezana.toml.Toml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reads .toml files and returns typed configuration objects.
 * Delegates to toml4j for raw parsing, then maps to records.
 *
 * Single public entry point per config type.
 * All parsing errors thrown as ConfigParseException with line reference.
 */
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private final Path configDir;

    public ConfigLoader(Path configDir) {
        this.configDir = configDir;
        log.info("ConfigLoader initialized with configDir={}", configDir.toAbsolutePath());
    }

    /**
     * Load a single unit config from configDir/units/{unitName}.toml.
     */
    public UnitConfig loadUnitConfig(String unitName) {
        Path filePath = configDir.resolve("units").resolve(unitName + ".toml");
        log.info("Loading unit config: {}", filePath);
        return parseUnitConfig(readToml(filePath));
    }

    /**
     * Load a weapon config from configDir/weapons/{weaponName}.toml.
     */
    public WeaponConfig loadWeaponConfig(String weaponName) {
        Path filePath = configDir.resolve("weapons").resolve(weaponName + ".toml");
        log.info("Loading weapon config: {}", filePath);
        return parseWeaponConfig(readToml(filePath));
    }

    /**
     * Load a building config from configDir/buildings/{buildingName}.toml.
     */
    public BuildingConfig loadBuildingConfig(String buildingName) {
        Path filePath = configDir.resolve("buildings").resolve(buildingName + ".toml");
        log.info("Loading building config: {}", filePath);
        return parseBuildingConfig(readToml(filePath));
    }

    /**
     * Batch load all unit configs found in configDir/units/.
     */
    public Map<String, UnitConfig> loadAllUnitConfigs() {
        Path unitsDir = configDir.resolve("units");
        if (!Files.isDirectory(unitsDir)) {
            log.warn("Units directory not found: {}", unitsDir);
            return Collections.emptyMap();
        }

        Map<String, UnitConfig> configs = new LinkedHashMap<>();
        try {
            List<Path> files = Files.list(unitsDir)
                    .filter(p -> p.toString().endsWith(".toml"))
                    .collect(Collectors.toList());

            for (Path file : files) {
                try {
                    String name = file.getFileName().toString()
                            .replace(".toml", "");
                    configs.put(name, parseUnitConfig(readToml(file)));
                } catch (Exception e) {
                    log.error("Failed to load unit config: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to list unit configs", e);
        }

        log.info("Loaded {} unit configs", configs.size());
        return configs;
    }

    private Toml readToml(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return new Toml().read(reader);
        } catch (IOException e) {
            throw new ConfigParseException("Failed to read config file: " + path, e);
        }
    }

    private UnitConfig parseUnitConfig(Toml toml) {
        IdentitySection identity = parseIdentity(toml);
        StatsSection stats = parseStats(toml);
        List<String> prerequisites = toml.getList("prerequisites.prerequisite", Collections.emptyList());
        long buildingsRequired = safeGetInt(toml, "prerequisites.buildings_required", 0L);
        VeterancySection veterancy = parseVeterancy(toml);
        CombatSection combat = parseCombat(toml);
        AbilitiesSection abilities = parseAbilities(toml);
        PassengersSection passengers = parsePassengers(toml);
        AudioSection audio = parseAudio(toml);
        MovementSection movement = parseMovement(toml);
        TurretsSection turrets = parseTurrets(toml);
        BuildingInteractionsSection buildingInteractions = parseBuildingInteractions(toml);
        SpecialFlagsSection specialFlags = parseSpecialFlags(toml);

        return new UnitConfig(identity, stats, prerequisites, (int) buildingsRequired,
                veterancy, combat, abilities, passengers, audio, movement, turrets,
                buildingInteractions, specialFlags);
    }

    private IdentitySection parseIdentity(Toml toml) {
        return new IdentitySection(
                toml.getString("identity.name", ""),
                toml.getString("identity.display_name", ""),
                toml.getString("identity.category", ""),
                toml.getString("identity.owner", ""),
                toml.getBoolean("identity.nominal", false),
                toml.getBoolean("identity.insignificant", false),
                toml.getBoolean("identity.legal_target", true),
                toml.getBoolean("identity.selectable", true),
                toml.getBoolean("identity.radar_visible", true),
                toml.getBoolean("identity.radar_invisible", false),
                toml.getBoolean("identity.cloakable", false),
                toml.getBoolean("identity.cloak_stop", false),
                toml.getString("identity.type", "unit")
        );
    }

    private StatsSection parseStats(Toml toml) {
        return new StatsSection(
                safeGetInt(toml, "stats.strength", 0L),
                toml.getString("stats.armor", "none"),
                safeGetInt(toml, "stats.sight", 0L),
                safeGetFloat(toml, "stats.guard_range", 0.0),
                safeGetFloat(toml, "stats.speed", 0.0),
                safeGetFloat(toml, "stats.rot", 0.0),
                safeGetInt(toml, "stats.cost", 0L),
                safeGetInt(toml, "stats.points", 0L),
                safeGetInt(toml, "stats.tech_level", 0L),
                safeGetInt(toml, "stats.build_limit", -1L),
                safeGetFloat(toml, "stats.build_time", 0.0)
        );
    }

    private VeterancySection parseVeterancy(Toml toml) {
        return new VeterancySection(
                toml.getBoolean("veterancy.trainable", false),
                toml.getList("veterancy.veteran_abilities", Collections.emptyList()),
                toml.getList("veterancy.elite_abilities", Collections.emptyList())
        );
    }

    private CombatSection parseCombat(Toml toml) {
        return new CombatSection(
                toml.getString("combat.primary_weapon", ""),
                toml.getString("combat.secondary_weapon", ""),
                toml.getString("combat.elite_primary", ""),
                toml.getString("combat.elite_secondary", ""),
                safeGetInt(toml, "combat.ammo", -1L),
                safeGetFloat(toml, "combat.reload_time", 0.0),
                toml.getBoolean("combat.manual_reload", false),
                safeGetInt(toml, "combat.fire_angle", 64L),
                toml.getBoolean("combat.target_laser", false),
                toml.getBoolean("combat.deploy_to_fire", false),
                toml.getBoolean("combat.no_moving_fire", false)
        );
    }

    @SuppressWarnings("unchecked")
    private AbilitiesSection parseAbilities(Toml toml) {
        boolean deployEnabled = toml.getBoolean("abilities.deploy.enabled", false);
        float deployTime = safeGetFloat(toml, "abilities.deploy.deploy_time", 0.5);

        List<AbilitiesSection.DeployAction> onDeploy = Collections.emptyList();
        List<Map<String, Object>> deployActions = toml.getList("abilities.deploy.on_deploy");
        if (deployActions != null && !deployActions.isEmpty()) {
            onDeploy = deployActions.stream()
                    .map(m -> new AbilitiesSection.DeployAction(
                            (String) m.getOrDefault("type", ""),
                            (String) m.getOrDefault("weapon", ""),
                            (String) m.getOrDefault("stat", ""),
                            getFloatValue(m, "value", 0f)))
                    .collect(Collectors.toList());
        }

        boolean dockEnabled = toml.getBoolean("abilities.docking.enabled", false);
        String dockType = toml.getString("abilities.docking.dock_type", "");
        float unloadTime = safeGetFloat(toml, "abilities.docking.unload_time", 1.0);
        int resourceCapacity = safeGetInt(toml, "abilities.docking.resource_capacity", 0L);

        return new AbilitiesSection(
                new AbilitiesSection.DeploySection(deployEnabled, deployTime, onDeploy),
                new AbilitiesSection.DockingAbilitySection(dockEnabled, dockType, unloadTime, resourceCapacity)
        );
    }

    private PassengersSection parsePassengers(Toml toml) {
        return new PassengersSection(
                safeGetInt(toml, "passengers.passengers", 0L)
        );
    }

    private AudioSection parseAudio(Toml toml) {
        return new AudioSection(
                toml.getList("audio.voice_select", Collections.emptyList()),
                toml.getList("audio.voice_move", Collections.emptyList()),
                toml.getList("audio.voice_attack", Collections.emptyList()),
                toml.getList("audio.voice_die", Collections.emptyList()),
                toml.getList("audio.voice_feedback", Collections.emptyList()),
                toml.getList("audio.voice_comment", Collections.emptyList()),
                toml.getString("audio.crush_sound", "")
        );
    }

    private MovementSection parseMovement(Toml toml) {
        return new MovementSection(
                toml.getString("movement.locomotor", "wheels"),
                toml.getBoolean("movement.crushable", false),
                toml.getBoolean("movement.crusher", false),
                toml.getBoolean("movement.carries_crate", false)
        );
    }

    private TurretsSection parseTurrets(Toml toml) {
        return new TurretsSection(
                toml.getBoolean("turrets.turret", false),
                toml.getBoolean("turrets.turret_spins", false),
                safeGetFloatList(toml, "turrets.turret_rotation_yaw", Arrays.asList(-180f, 180f)),
                safeGetFloat(toml, "turrets.turret_rotation_speed", 5.0),
                safeGetFloatList(toml, "turrets.barrel_elevation_pitch", Arrays.asList(-5f, 45f)),
                safeGetFloat(toml, "turrets.barrel_speed", 4.0)
        );
    }

    private BuildingInteractionsSection parseBuildingInteractions(Toml toml) {
        return new BuildingInteractionsSection(
                toml.getString("building_interactions.dock", ""),
                toml.getString("building_interactions.free_unit", ""),
                toml.getBoolean("building_interactions.unit_reload", false),
                toml.getBoolean("building_interactions.unit_repair", false),
                toml.getBoolean("building_interactions.dock_unload", false)
        );
    }

    private SpecialFlagsSection parseSpecialFlags(Toml toml) {
        return new SpecialFlagsSection(
                toml.getBoolean("special_flags.harvester", false)
        );
    }

    private WeaponConfig parseWeaponConfig(Toml toml) {
        return new WeaponConfig(
                toml.getString("weapon.name", ""),
                toml.getString("weapon.type", ""),
                toml.getString("weapon.projectile_scene", ""),
                safeGetFloat(toml, "weapon.damage", 0.0),
                toml.getString("weapon.damage_type", ""),
                toml.getBoolean("weapon.area_damage", false),
                safeGetFloat(toml, "weapon.explosion_radius", 0.0),
                safeGetFloat(toml, "weapon.range", 0.0),
                safeGetFloat(toml, "weapon.cooldown", 0.0),
                safeGetFloat(toml, "weapon.min_range", 0.0),
                safeGetFloat(toml, "weapon.ballistic_arc", 0.0),
                safeGetFloat(toml, "weapon.accuracy", 1.0),
                safeGetFloat(toml, "weapon.scatter_radius", 0.0),
                safeGetFloat(toml, "weapon.particle_speed", 0.0),
                toml.getBoolean("weapon.shooting_correction", false),
                toml.getBoolean("weapon.homing", false)
        );
    }

    private BuildingConfig parseBuildingConfig(Toml toml) {
        BuildingIdentitySection identity = new BuildingIdentitySection(
                toml.getString("identity.name", ""),
                toml.getString("identity.type", "building"),
                toml.getString("identity.category", ""),
                toml.getString("identity.icon", ""),
                safeGetInt(toml, "identity.build_cost", 0L),
                safeGetFloat(toml, "identity.build_time", 0.0)
        );

        BuildingStatsSection stats = new BuildingStatsSection(
                safeGetInt(toml, "stats.health", 0L),
                toml.getString("stats.armor_type", "none"),
                safeGetInt(toml, "stats.sight_range", 0L),
                safeGetInt(toml, "stats.power_required", 0L),
                safeGetInt(toml, "stats.power_provided", 0L)
        );

        ProductionSection production = new ProductionSection(
                safeGetInt(toml, "production.queue_size", 0L),
                toml.getList("production.produces", Collections.emptyList())
        );

        WeaponConfig defenseWeapon = null;
        if (toml.contains("weapon.defense")) {
            defenseWeapon = new WeaponConfig(
                    toml.getString("weapon.defense.name", ""),
                    toml.getString("weapon.defense.type", ""),
                    "",
                    safeGetFloat(toml, "weapon.defense.damage", 0.0),
                    toml.getString("weapon.defense.damage_type", ""),
                    false,
                    0f,
                    safeGetFloat(toml, "weapon.defense.range", 0.0),
                    safeGetFloat(toml, "weapon.defense.cooldown", 0.0),
                    0f, 0f, 1f, 0f, 0f,
                    false, false
            );
        }

        BuildingDockingSection docking = new BuildingDockingSection(
                toml.getBoolean("docking.enabled", false),
                toml.getString("docking.dock_type", ""),
                safeGetInt(toml, "docking.unload_speed", 0L),
                toml.getBoolean("docking.harvester_queue", false)
        );

        return new BuildingConfig(identity, stats, production, defenseWeapon, docking);
    }

    @SuppressWarnings("unchecked")
    private static float getFloatValue(Map<String, Object> map, String key, float defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        return defaultValue;
    }

    private static float safeGetFloat(Toml toml, String key, double defaultValue) {
        try {
            Double d = toml.getDouble(key);
            if (d != null) return d.floatValue();
        } catch (ClassCastException ignored) {
        }
        try {
            Long l = toml.getLong(key);
            if (l != null) return l.floatValue();
        } catch (ClassCastException ignored) {
        }
        return (float) defaultValue;
    }

    private static int safeGetInt(Toml toml, String key, long defaultValue) {
        try {
            Long l = toml.getLong(key);
            if (l != null) return l.intValue();
        } catch (ClassCastException ignored) {
        }
        return (int) defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static List<Float> safeGetFloatList(Toml toml, String key, List<Float> defaultValue) {
        try {
            List<Object> rawList = toml.getList(key);
            if (rawList == null) return defaultValue;
            List<Float> result = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof Number num) {
                    result.add(num.floatValue());
                }
            }
            return result.isEmpty() ? defaultValue : result;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
