package com.jrts.tools.importer;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.gltf.GlbLoader;
import com.jme3.scene.plugins.gltf.GltfLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI entry point for the full build-time import pipeline.
 *
 * Usage:
 * java -cp ... com.jrts.tools.importer.BatchImporter
 *     --source assets/blender
 *     --intermediate assets/models/intermediate
 *     --output assets/models/final
 *
 * Stage 1: operates directly on .glb files (skips Blender invocation).
 * Full Blender CLI integration will be added when Blender is available.
 */
public class BatchImporter {

    private static final Logger log = LoggerFactory.getLogger(BatchImporter.class);

    public static void main(String[] args) {
        String sourceDir = "assets/blender";
        String intermediateDir = "assets/models/intermediate";
        String outputDir = "assets/models/final";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--source" -> sourceDir = args[++i];
                case "--intermediate" -> intermediateDir = args[++i];
                case "--output" -> outputDir = args[++i];
            }
        }

        log.info("BatchImporter starting");
        log.info("  Source:      {}", sourceDir);
        log.info("  Intermediate: {}", intermediateDir);
        log.info("  Output:       {}", outputDir);

        try {
            Path sourcePath = Path.of(sourceDir);
            Path intermediatePath = Path.of(intermediateDir);
            Path outputPath = Path.of(outputDir);

            Files.createDirectories(intermediatePath);
            Files.createDirectories(outputPath);

            List<String> glbFiles = findGlbFiles(sourcePath, intermediatePath);

            if (glbFiles.isEmpty()) {
                log.warn("No .glb files found in source or intermediate directories");
                log.info("Stage 1 mode: .blend → .glb conversion requires Blender CLI.");
                log.info("Place .glb files in {} to import them.", intermediateDir);
                return;
            }

            int succeeded = 0;
            int warnings = 0;
            int errors = 0;

            GltfImporter gltfImporter = new GltfImporter();
            EmptyNodeResolver resolver = new EmptyNodeResolver();
            CollisionBaker baker = new CollisionBaker();
            M3oExporter exporter = new M3oExporter();

            for (String glbFile : glbFiles) {
                try {
                    Path glbPath = Path.of(glbFile);
                    log.info("Processing: {}", glbPath.getFileName());

                    com.jme3.asset.AssetManager assetManager = createAssetManager();
                    Node rootNode = gltfImporter.importGlb(glbPath, assetManager);

                    List<String> validationWarnings = gltfImporter.validate(rootNode);
                    if (!validationWarnings.isEmpty()) {
                        warnings += validationWarnings.size();
                        validationWarnings.forEach(w -> log.warn("  Validation: {}", w));
                    }

                    ModelManifest manifest = resolver.resolve(rootNode);

                    ModelManifest.CollisionShapeData collision = baker.bakeAABB(rootNode);
                    manifest.setCollision(collision);

                    String modelName = glbPath.getFileName().toString().replace(".glb", "");
                    manifest.setModelName(modelName);

                    Path outputFile = outputPath.resolve(modelName + ".m3o");
                    exporter.exportM3o(rootNode, manifest, outputFile);

                    succeeded++;
                    log.info("  OK: {}", outputFile.getFileName());
                } catch (ImportException e) {
                    log.error("  ERROR: {}", e.getMessage(), e);
                    errors++;
                } catch (Exception e) {
                    log.error("  ERROR processing {}: {}", glbFile, e.getMessage(), e);
                    errors++;
                }
            }

            log.info("==============================");
            log.info("Batch import complete:");
            log.info("  Succeeded: {}", succeeded);
            log.info("  Warnings:  {}", warnings);
            log.info("  Errors:    {}", errors);

            if (errors > 0) {
                System.exit(2);
            } else if (warnings > 0) {
                System.exit(1);
            }
        } catch (Exception e) {
            log.error("Batch import failed", e);
            System.exit(2);
        }
    }

    private static List<String> findGlbFiles(Path sourcePath, Path intermediatePath) {
        List<String> glbFiles = new ArrayList<>();
        try {
            findGlbRecursive(sourcePath, glbFiles);
            findGlbRecursive(intermediatePath, glbFiles);
        } catch (Exception e) {
            log.warn("Error finding .glb files: {}", e.getMessage());
        }
        return glbFiles;
    }

    private static void findGlbRecursive(Path dir, List<String> result) throws Exception {
        if (!Files.isDirectory(dir)) {
            return;
        }
        Files.list(dir).forEach(p -> {
            String name = p.getFileName().toString().toLowerCase();
            if (name.endsWith(".glb")) {
                result.add(p.toString());
            } else if (Files.isDirectory(p) && !name.startsWith(".")) {
                try {
                    findGlbRecursive(p, result);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private static com.jme3.asset.AssetManager createAssetManager() {
        com.jme3.system.JmeSystem.setLowPermissions(false);
        DesktopAssetManager desktopAssetManager = new DesktopAssetManager();
        desktopAssetManager.registerLocator("/", com.jme3.asset.plugins.ClasspathLocator.class);
        desktopAssetManager.registerLocator("", FileLocator.class);
        desktopAssetManager.registerLoader(com.jme3.material.plugins.J3MLoader.class, "j3md", "j3m");
        desktopAssetManager.registerLoader(com.jme3.texture.plugins.AWTLoader.class, "png", "jpg", "jpeg", "gif", "bmp");
        desktopAssetManager.registerLoader(GlbLoader.class, "glb");
        desktopAssetManager.registerLoader(GltfLoader.class, "gltf");
        return desktopAssetManager;
    }
}
