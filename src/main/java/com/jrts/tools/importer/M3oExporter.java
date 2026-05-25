package com.jrts.tools.importer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Stage 4 (final) of the import pipeline.
 * Serializes a loaded Node + ModelManifest to a .m3o binary file.
 *
 * .m3o format (Stage 1 version):
 * Offset | Size  | Field
 * -------|-------|------------------------------
 * 0      | 4     | Magic: 'M' '3' 'O' 0x00
 * 4      | 4     | Version (uint32 LE): 1
 * 8      | 4     | Manifest JSON byte length (uint32 LE)
 * 12     | N     | Manifest JSON (UTF-8, Gson-serialized)
 * 12+N   | M     | JME BinaryExporter payload (spatial + meshes + materials)
 */
public class M3oExporter {

    private static final Logger log = LoggerFactory.getLogger(M3oExporter.class);

    private static final int MAGIC = 0x4D334F00; // "M3O\0"
    private static final int VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * @param node       JME spatial tree to serialize
     * @param manifest   structured metadata
     * @param outputPath destination .m3o file
     */
    public void exportM3o(Node node, ModelManifest manifest, Path outputPath) throws IOException {
        log.info("Exporting .m3o to: {}", outputPath);

        byte[] manifestBytes = GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream jmeBaos = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(node, jmeBaos);
        byte[] jmeBytes = jmeBaos.toByteArray();

        Path parent = outputPath.getParent();
        if (parent != null) {
            parent.toFile().mkdirs();
        }

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputPath.toFile())))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(manifestBytes.length);
            out.write(manifestBytes);
            out.write(jmeBytes);
        }

        log.info("Exported .m3o: {} (manifest={} bytes, jme={} bytes)",
                outputPath, manifestBytes.length, jmeBytes.length);
    }
}
