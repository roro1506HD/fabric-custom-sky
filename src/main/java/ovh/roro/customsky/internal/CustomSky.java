package ovh.roro.customsky.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.apache.commons.io.IOUtils;
import ovh.roro.customsky.util.Blender;
import ovh.roro.customsky.util.Util;

public class CustomSky {

    private static CustomSkyLayer[][] WORLD_SKY_LAYERS;

    public static void reset() {
        CustomSky.WORLD_SKY_LAYERS = null;
    }

    public static void update() {
        CustomSky.reset();
        CustomSky.WORLD_SKY_LAYERS = readCustomSkies();
    }

    private static CustomSkyLayer[][] readCustomSkies() {
        CustomSkyLayer[][] skyLayers = new CustomSkyLayer[10][0];
        String prefix = "optifine/sky/world";
        int lastWorldId = -1;

        for (int w = 0; w < skyLayers.length; w++) {
            String worldPrefix = prefix + w;
            List<CustomSkyLayer> layerList = new ArrayList<>();

            for (int i = 0; i < 1000; i++) {
                String path = worldPrefix + "/sky" + i + ".properties";
                int countMissing = 0;
                InputStream inputStream = null;

                try {
                    Identifier identifier = new Identifier(path);
                    inputStream = Util.getResourceStream(identifier);

                    if (inputStream == null && ++countMissing > 10) {
                        break;
                    }

                    Properties properties = new Properties();

                    properties.load(inputStream);

                    String defaultSource = i + ".png";
                    CustomSkyLayer layer = new CustomSkyLayer(identifier.toString(), properties, defaultSource);

                    if (layer.isValid(path)) {
                        String sourcePath = layer.getSource();

                        if (!sourcePath.endsWith(".png")) {
                            sourcePath = sourcePath + ".png";
                        }

                        identifier = new Identifier(sourcePath);
                        AbstractTexture texture = Util.getTexture(identifier);

                        if (texture == null) {
                            Util.warn("CustomSkyLoader", "Texture not found: " + identifier);
                        } else {
                            layer.setTextureId(texture.getGlId());
                            layerList.add(layer);
                        }
                    }
                } catch (FileNotFoundException ignored) {
                    if (++countMissing > 10) {
                        break;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        IOUtils.closeQuietly(inputStream);
                    }
                }
            }

            if (layerList.size() > 0) {
                CustomSkyLayer[] layers = layerList.toArray(new CustomSkyLayer[0]);

                skyLayers[w] = layers;
                lastWorldId = w;
            }
        }

        if (lastWorldId < 0) {
            return null;
        }

        int worldCount = lastWorldId + 1;
        CustomSkyLayer[][] layersTrim = new CustomSkyLayer[worldCount][0];

        System.arraycopy(skyLayers, 0, layersTrim, 0, layersTrim.length);

        return layersTrim;
    }

    public static void renderSky(World world, TextureManager textureManager, MatrixStack matrixStack, float partialTicks) {
        if (CustomSky.WORLD_SKY_LAYERS == null) {
            return;
        }

        int dimensionId = Util.getDimensionId(world);

        if (dimensionId < 0 || dimensionId >= CustomSky.WORLD_SKY_LAYERS.length) {
            return;
        }

        CustomSkyLayer[] layers = CustomSky.WORLD_SKY_LAYERS[dimensionId];

        if (layers == null) {
            return;
        }

        int timeOfDay = (int) world.getTimeOfDay();
        float celestialAngle = world.getSkyAngle(partialTicks);
        float rainStrength = world.getRainGradient(partialTicks);
        float thunderStrength = world.getThunderGradient(partialTicks);

        if (rainStrength > 0.0F) {
            thunderStrength /= rainStrength;
        }

        for (CustomSkyLayer layer : layers) {
            if (layer.isActive(world, timeOfDay)) {
                layer.render(world, matrixStack, timeOfDay, celestialAngle, rainStrength, thunderStrength);
            }
        }

        Blender.clearBlend(1.0F - rainStrength); // rainBrightness
    }

    public static boolean hasSkyLayers(World world) {
        if (CustomSky.WORLD_SKY_LAYERS == null) {
            return false;
        }

        int dimensionId = Util.getDimensionId(world);

        if (dimensionId < 0 || dimensionId >= CustomSky.WORLD_SKY_LAYERS.length) {
            return false;
        }

        CustomSkyLayer[] layers = CustomSky.WORLD_SKY_LAYERS[dimensionId];

        return layers != null && layers.length > 0;
    }
}
