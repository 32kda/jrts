package com.jrts.config;

import java.util.List;

/**
 * Production section for buildings.
 */
public record ProductionSection(
        int queueSize,
        List<String> produces) {
}
