package com.jrts.config;

import java.util.List;

/**
 * Veterancy section parsed from TOML [veterancy] block.
 */
public record VeterancySection(
        boolean trainable,
        List<String> veteranAbilities,
        List<String> eliteAbilities) {
}
