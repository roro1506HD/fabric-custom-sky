package ovh.roro.customsky.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.ClientResourcePackProfile;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ovh.roro.customsky.CustomSkyMod;

public class Util {

    public static final Logger LOGGER = LogManager.getLogger(CustomSkyMod.class);

    @SuppressWarnings("unchecked")
    public static <T> T[] addToArray(T[] array, T toAdd) {
        if (array == null) {
            throw new NullPointerException("Array must not be null");
        }

        int oldLength = array.length;
        int newLength = oldLength + 1;
        T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), newLength);

        System.arraycopy(array, 0, newArray, 0, oldLength);
        newArray[oldLength] = toAdd;

        return newArray;
    }

    public static String[] tokenize(String input, String delimiter) {
        StringTokenizer tokenizer = new StringTokenizer(input, delimiter);
        List<String> list = new ArrayList<>();

        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }

        return list.toArray(new String[0]);
    }

    public static Identifier fixResourceLocation(Identifier identifier, String basePath) {
        if (!identifier.getNamespace().equals("minecraft")) {
            return identifier;
        }

        String path = identifier.getPath();
        String pathFixed = Util.fixResourcePath(path, basePath);

        if (!pathFixed.equals(path)) {
            identifier = new Identifier(identifier.getNamespace(), pathFixed);
        }

        return identifier;
    }

    public static String fixResourcePath(String path, String basePath) {
        String assetsMinecraft = "assets/minecraft/";

        if (path.startsWith(assetsMinecraft)) {
            return path.substring(assetsMinecraft.length());
        }

        if (path.startsWith("./")) {
            path = path.substring(2);

            if (!basePath.endsWith("/")) {
                basePath += "/";
            }

            return basePath + path;
        }

        if (path.startsWith("/~")) {
            path = path.substring(1);
        }

        String optifine = "optifine/";

        if (path.startsWith("~/")) {
            return optifine + path.substring(2);
        }

        if (path.startsWith("/")) {
            return optifine + path.substring(1);
        }

        return path;
    }

    public static String getBasePath(String path) {
        int pos = path.lastIndexOf(47);

        if (pos < 0) {
            return "";
        }

        return path.substring(0, pos);
    }

    public static void warn(String context, String message) {
        Util.LOGGER.warn("[CustomSky] [" + context + "] " + message);
    }

    public static int getDimensionId(World world) {
        if (world == null) {
            return -2;
        }

        return Util.getDimensionId(world.getDimensionRegistryKey());
    }

    public static int getDimensionId(RegistryKey<DimensionType> dimension) {
        if (dimension == DimensionType.THE_NETHER_REGISTRY_KEY) {
            return -1;
        }

        if (dimension == DimensionType.OVERWORLD_REGISTRY_KEY) {
            return 0;
        }

        if (dimension == DimensionType.THE_END_REGISTRY_KEY) {
            return 1;
        }

        return -2;
    }

    public static InputStream getResourceStream(Identifier identifier) throws IOException {
        return Util.getResourceStream(MinecraftClient.getInstance().getResourceManager(), identifier);
    }

    public static InputStream getResourceStream(ResourceManager resourceManager, Identifier identifier) throws IOException {
        Resource resource = resourceManager.getResource(identifier);

        if (resource == null) {
            return null;
        }

        return resource.getInputStream();
    }

    public static AbstractTexture getTexture(Identifier identifier) {
        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        AbstractTexture texture = textureManager.getTexture(identifier);

        if (texture != null) {
            return texture;
        }

        if (getDefiningResourcePack(identifier) == null) {
            return null;
        }

        texture = new ResourceTexture(identifier);
        textureManager.registerTexture(identifier, texture);

        return texture;
    }

    public static ResourcePack getDefiningResourcePack(Identifier identifier) {
        ResourcePackManager<ClientResourcePackProfile> resourcePackManager = MinecraftClient.getInstance().getResourcePackManager();
        Collection<ClientResourcePackProfile> enabledProfiles = resourcePackManager.getEnabledProfiles();
        List<ClientResourcePackProfile> enabledProfilesList = (List<ClientResourcePackProfile>) enabledProfiles;

        for (int i = enabledProfilesList.size() - 1; i >= 0; i--) {
            ResourcePack resourcePack = enabledProfilesList.get(i).createResourcePack();

            if (resourcePack.contains(ResourceType.CLIENT_RESOURCES, identifier)) {
                return resourcePack;
            }
        }

        return null;
    }
}
