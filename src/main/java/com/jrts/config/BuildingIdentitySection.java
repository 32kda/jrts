package com.jrts.config;

import java.util.List;

/**
 * Building identity section (different fields from Unit identity).
 */
public record BuildingIdentitySection(
        String name,
        String type,
        String category,
        String icon,
        int buildCost,
        float buildTime) {
}
