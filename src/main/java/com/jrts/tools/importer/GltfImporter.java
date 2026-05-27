package com.jrts.tools.importer;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 1 of the import pipeline.
 * Reads a .glb (glTF binary) file using JME's GltfLoader
 * and returns a flat Node tree with attached Geometry.
 *
 * This is a BUILD-TIME tool. It does NOT run at game runtime.
 */
public class GltfImporter {

    private static final Logger log = LoggerFactory.getLogger(GltfImporter.class);

    /**
     * @param glbPath      path to .glb file
     * @param assetManager JME AssetManager (configured for build-time paths)
     * @return root Node of the loaded scene
     * @throws ImportException on any failure
     */
    public Node importGlb(Path glbPath, AssetManager assetManager) throws ImportException {
        log.info("Importing .glb: {}", glbPath);

        if (!glbPath.toFile().exists()) {
            throw new ImportException("GLB file not found: " + glbPath);
        }

        try {
            Spatial loaded = assetManager.loadModel(glbPath.toString());
            if (loaded == null) {
                throw new ImportException("AssetManager returned null for: " + glbPath);
            }

            if (!(loaded instanceof Node rootNode)) {
                throw new ImportException("Expected Node root, got: " + loaded.getClass().getName());
            }

            log.info("Successfully loaded .glb: {} ({} children)", glbPath, rootNode.getChildren().size());
            return rootNode;
        } catch (Exception e) {
            throw new ImportException("Failed to import .glb: " + glbPath, e);
        }
    }

    /**
     * Validates the loaded node tree:
     * - At least one Geometry descendant (not just empties)
     *
     * @return list of validation warnings (non-fatal); throws on fatal issues
     */
    public List<String> validate(Node rootNode) throws ImportException {
        List<String> warnings = new ArrayList<>();

        if (rootNode == null) {
            throw new ImportException("Root node is null");
        }

        int geometryCount = countGeometries(rootNode);
        if (geometryCount == 0) {
            throw new ImportException("Scene has no meshes (only empty nodes)");
        }

        int nodeCount = countNodes(rootNode);
        log.info("Validation: {} nodes, {} geometries", nodeCount, geometryCount);

        return warnings;
    }

    private int countGeometries(Node node) {
        int count = 0;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                count++;
            }
            if (child instanceof Node childNode) {
                count += countGeometries(childNode);
            }
        }
        return count;
    }

    private int countNodes(Node node) {
        int count = 1;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Node childNode) {
                count += countNodes(childNode);
            }
        }
        return count;
    }
}
