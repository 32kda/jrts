package com.jrts.config;

/**
 * Special flags section parsed from TOML [special_flags] block.
 */
public record SpecialFlagsSection(
        boolean harvester) {
}
