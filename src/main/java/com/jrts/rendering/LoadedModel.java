package com.jrts.rendering;

import com.jrts.tools.importer.ModelManifest;
import com.jme3.scene.Node;

/**
 * Result of loading a .m3o model at runtime.
 * Bundles the JME spatial tree with structured metadata.
 */
public record LoadedModel(
        Node spatial,
        ModelManifest manifest) {
}
