package com.jrts.tools.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Stage 1 of the import pipeline.
 * Reads a .glb (glTF binary) file using a custom GLB parser
 * (JME 3.6.1's GltfLoader only supports text-based .gltf, not binary .glb).
 *
 * Converts GLB to temporary GLTF+BIN files, loads them with JME's GltfLoader,
 * then applies material fallback for any missing textures.
 *
 * This is a BUILD-TIME tool. It does NOT run at game runtime.
 */
public class GltfImporter {

    private static final Logger log = LoggerFactory.getLogger(GltfImporter.class);

    private static final int GLB_MAGIC = 0x46546C67; // "glTF"
    private static final int CHUNK_TYPE_JSON = 0x4E4F534A; // "JSON"
    private static final int CHUNK_TYPE_BIN = 0x004E4942; // "BIN\0"

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

        String modelName = glbPath.getFileName().toString().replace(".glb", "");
        Path parentDir = glbPath.getParent();
        Path gltfPath = parentDir.resolve(modelName + "_tmp.gltf");
        Path binPath = parentDir.resolve(modelName + "_tmp.bin");

        try {
//            convertGlbToGltf(glbPath, gltfPath, binPath);
//
//            String parentDirStr = parentDir.toString();
//            String gltfFileName = gltfPath.getFileName().toString();
//            assetManager.registerLocator(parentDirStr, com.jme3.asset.plugins.FileLocator.class);

            Spatial loaded = assetManager.loadModel(glbPath.toString());
            if (loaded == null) {
                throw new ImportException("AssetManager returned null for: " + glbPath);
            }

            if (!(loaded instanceof Node rootNode)) {
                throw new ImportException("Expected Node root, got: " + loaded.getClass().getName());
            }

            log.info("Successfully loaded .glb via GLTF conversion: {} ({} children)", glbPath, rootNode.getChildren().size());

            applyMaterialFallback(rootNode, glbPath);

            return rootNode;
        } catch (ImportException e) {
            throw e;
        } catch (Exception e) {
            throw new ImportException("Failed to import .glb: " + glbPath, e);
        } finally {
            tryDelete(gltfPath);
            tryDelete(binPath);
        }
    }

    private void convertGlbToGltf(Path glbPath, Path gltfPath, Path binPath) throws IOException {
        byte[] glbData = Files.readAllBytes(glbPath);

        if (glbData.length < 12) {
            throw new IOException("GLB file too small: " + glbPath);
        }

        int magic = readUint32LE(glbData, 0);
        if (magic != GLB_MAGIC) {
            throw new IOException("Not a valid GLB file (bad magic): " + glbPath);
        }

        int version = readUint32LE(glbData, 4);
        int totalLength = readUint32LE(glbData, 8);

        if (version != 2) {
            throw new IOException("Unsupported GLB version: " + version);
        }

        int offset = 12;
        byte[] jsonBytes = null;
        byte[] binBytes = null;

        while (offset < totalLength) {
            if (offset + 8 > glbData.length) {
                break;
            }
            int chunkLength = readUint32LE(glbData, offset);
            int chunkType = readUint32LE(glbData, offset + 4);
            offset += 8;

            if (chunkType == CHUNK_TYPE_JSON) {
                jsonBytes = new byte[chunkLength];
                System.arraycopy(glbData, offset, jsonBytes, 0, chunkLength);
            } else if (chunkType == CHUNK_TYPE_BIN) {
                binBytes = new byte[chunkLength];
                System.arraycopy(glbData, offset, binBytes, 0, chunkLength);
            }
            offset += chunkLength;
        }

        if (jsonBytes == null) {
            throw new IOException("No JSON chunk found in GLB: " + glbPath);
        }

        String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);

        if (binBytes != null) {
            String b64data = java.util.Base64.getEncoder().encodeToString(binBytes);
            String dataUri = "data:application/octet-stream;base64," + b64data;
            jsonStr = patchBufferUri(jsonStr, dataUri);
        }

        Files.writeString(gltfPath, jsonStr, StandardCharsets.UTF_8);
    }

    private String patchBufferUri(String jsonStr, String dataUri) {
        try {
            JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
            JsonArray buffers = root.getAsJsonArray("buffers");
            if (buffers != null && buffers.size() > 0) {
                JsonObject firstBuffer = buffers.get(0).getAsJsonObject();
                if (firstBuffer.get("uri") == null) {
                    firstBuffer.addProperty("uri", dataUri);
                }
            }
            return root.toString();
        } catch (Exception e) {
            log.warn("Failed to patch buffer URI, using original JSON: {}", e.getMessage());
            return jsonStr;
        }
    }

    private static int readUint32LE(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    private void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private void applyMaterialFallback(Node rootNode, Path glbPath) {
        String modelName = glbPath.getFileName().toString().replace(".glb", "");
        Path parentDir = glbPath.getParent();
        Path siblingPng = parentDir.resolve(modelName + ".png");

        int fixedCount = 0;
        List<Geometry> geometries = collectGeometries(rootNode);
        for (Geometry geom : geometries) {
            Material mat = geom.getMaterial();
            if (mat != null) {
                MatParamTexture param = mat.getTextureParam("BaseColorMap");
                if (param == null) {
                    param = mat.getTextureParam("DiffuseMap");
                }
                if (param == null) {
                    param = mat.getTextureParam("ColorMap");
                }
                if (param != null && param.getTextureValue() != null) {
                    continue;
                }
            }

            if (Files.exists(siblingPng)) {
                Material newMat = createMaterialFromTexture(siblingPng);
                if (newMat != null) {
                    geom.setMaterial(newMat);
                    fixedCount++;
                }
            } else {
                Material redMat = createRedMaterial();
                if (redMat != null) {
                    geom.setMaterial(redMat);
                    fixedCount++;
                }
            }
        }

        if (fixedCount > 0) {
            log.info("Applied material fallback to {} geometries", fixedCount);
        }
    }

    private Material createMaterialFromTexture(Path pngPath) {
        try {
            String pngFileName = pngPath.getFileName().toString();

            DesktopAssetManagerWrapper wrapper = new DesktopAssetManagerWrapper(pngPath.getParent().toString());

            com.jme3.texture.Image image = loadImage(pngPath);
            com.jme3.texture.Texture2D texture = new com.jme3.texture.Texture2D(image);
            texture.setMagFilter(com.jme3.texture.Texture.MagFilter.Bilinear);
            texture.setMinFilter(com.jme3.texture.Texture.MinFilter.Trilinear);
            texture.setWrap(com.jme3.texture.Texture.WrapMode.Repeat);

            Material mat = new Material(wrapper.delegate, "Common/MatDefs/Light/Lighting.j3md");
            mat.setTexture("DiffuseMap", texture);
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", ColorRGBA.White);
            mat.setColor("Ambient", ColorRGBA.White);
            mat.setColor("Specular", ColorRGBA.White);
            mat.setFloat("Shininess", 16f);
            return mat;
        } catch (Exception e) {
            log.warn("Could not load texture from {}: {}", pngPath, e.getMessage());
            return createRedMaterial();
        }
    }

    private Material createRedMaterial() {
        try {
            DesktopAssetManagerWrapper wrapper = new DesktopAssetManagerWrapper(
                    System.getProperty("user.dir"));
            Material mat = new Material(wrapper.delegate, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Red);
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            return mat;
        } catch (Exception e) {
            log.warn("Could not create Unshaded material: {}", e.getMessage());
        }

        try {
            DesktopAssetManagerWrapper wrapper = new DesktopAssetManagerWrapper(
                    System.getProperty("user.dir"));
            Material mat = new Material(wrapper.delegate, "Common/MatDefs/Light/Lighting.j3md");
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", ColorRGBA.Red);
            mat.setColor("Ambient", ColorRGBA.Red);
            mat.setColor("Specular", ColorRGBA.Red);
            mat.setFloat("Shininess", 16f);
            return mat;
        } catch (Exception e) {
            log.warn("Could not create any JME material: {}", e.getMessage());
        }
        return null;
    }

    private com.jme3.texture.Image loadImage(Path path) throws IOException {
        BufferedImage bi = ImageIO.read(path.toFile());
        if (bi == null) {
            throw new IOException("Could not read image: " + path);
        }

        int width = bi.getWidth();
        int height = bi.getHeight();
        boolean hasAlpha = bi.getColorModel().hasAlpha();

        int[] pixels = new int[width * height];
        bi.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buf = com.jme3.util.BufferUtils.createByteBuffer(width * height * 4);
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buf.put((byte) ((pixel >> 16) & 0xFF));
                buf.put((byte) ((pixel >> 8) & 0xFF));
                buf.put((byte) (pixel & 0xFF));
                buf.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buf.flip();

        com.jme3.texture.Image.Format fmt = hasAlpha
                ? com.jme3.texture.Image.Format.RGBA8
                : com.jme3.texture.Image.Format.RGB8;

        return new com.jme3.texture.Image(fmt, width, height, buf, null,
                com.jme3.texture.image.ColorSpace.sRGB);
    }

    /**
     * Validates the loaded node tree.
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

    private List<Geometry> collectGeometries(Node node) {
        List<Geometry> result = new ArrayList<>();
        collectGeometriesRecursive(node, result);
        return result;
    }

    private void collectGeometriesRecursive(Node node, List<Geometry> result) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry geom) {
                result.add(geom);
            }
            if (child instanceof Node childNode) {
                collectGeometriesRecursive(childNode, result);
            }
        }
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

    static class DesktopAssetManagerWrapper {
        final com.jme3.asset.DesktopAssetManager delegate;

        DesktopAssetManagerWrapper(String parentDir) {
            com.jme3.system.JmeSystem.setLowPermissions(false);
            delegate = new com.jme3.asset.DesktopAssetManager();
            delegate.registerLocator("/", com.jme3.asset.plugins.ClasspathLocator.class);
            delegate.registerLocator(parentDir, com.jme3.asset.plugins.FileLocator.class);
            delegate.registerLoader(com.jme3.material.plugins.J3MLoader.class, "j3md", "j3m");
            delegate.registerLoader(com.jme3.texture.plugins.AWTLoader.class, "png", "jpg", "jpeg", "gif", "bmp");
        }
    }
}
