package com.jrts.rendering;

import com.jrts.tools.importer.ModelManifest;
import com.google.gson.Gson;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Runtime loader for .m3o model files.
 * Reads the custom binary format: JSON manifest header + JME spatial payload.
 */
public class ModelLoader {

    private static final Logger log = LoggerFactory.getLogger(ModelLoader.class);

    private static final int MAGIC = 0x4D334F00;

    private static final Gson GSON = new Gson();

    /**
     * Load a .m3o model file and return the spatial tree with manifest.
     *
     * @param m3oPath path to .m3o file
     * @return loaded model with spatial and manifest
     * @throws IOException on read failure or invalid format
     */
    public LoadedModel loadM3o(Path m3oPath) throws IOException {
        log.info("Loading .m3o: {}", m3oPath);

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(m3oPath.toFile())))) {

            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IOException(String.format(
                        "Invalid .m3o magic: expected 0x%08X, got 0x%08X", MAGIC, magic));
            }

            int version = in.readInt();
            if (version != 1) {
                throw new IOException("Unsupported .m3o version: " + version);
            }

            int manifestLength = in.readInt();
            byte[] manifestBytes = new byte[manifestLength];
            in.readFully(manifestBytes);
            String manifestJson = new String(manifestBytes, StandardCharsets.UTF_8);
            ModelManifest manifest = GSON.fromJson(manifestJson, ModelManifest.class);

            byte[] remainingBytes = in.readAllBytes();

            Node spatial;
            BinaryImporter binaryImporter = BinaryImporter.getInstance();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(remainingBytes)) {
                spatial = (Node) binaryImporter.load(bais);
            }

            log.info("Loaded .m3o: {} (model={}, category={})",
                    m3oPath, manifest.getModelName(), manifest.getCategory());

            return new LoadedModel(spatial, manifest);
        }
    }

    /**
     * Convenience method using string path.
     */
    public LoadedModel loadM3o(String path) throws IOException {
        return loadM3o(Path.of(path));
    }
}
