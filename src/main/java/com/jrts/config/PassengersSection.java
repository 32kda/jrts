package com.jrts.config;

import java.util.List;

/**
 * Passengers section parsed from TOML [passengers] block.
 */
public record PassengersSection(
        int passengers) {
}
