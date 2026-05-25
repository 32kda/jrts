package com.jrts.config;

import java.util.List;

/**
 * Movement section parsed from TOML [movement] block.
 */
public record MovementSection(
        String locomotor,
        boolean crushable,
        boolean crusher,
        boolean carriesCrate) {
}
