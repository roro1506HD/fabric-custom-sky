package ovh.roro.customsky.util;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import ovh.roro.customsky.config.RangeInt;
import ovh.roro.customsky.config.RangeIntList;

public class Parser {

    private static Map<Identifier, Biome> BIOMES_COMPACT;

    public static int parseInt(String input, int defaultValue) {
        try {
            return Integer.parseInt(input);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static float parseFloat(String input, float defaultValue) {
        try {
            return Float.parseFloat(input);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static RangeInt parseRangeInt(String input) {
        if (input == null) {
            return null;
        }

        if (input.indexOf('-') >= 0) {
            String[] tokens = Util.tokenize(input, "-");

            if (tokens.length != 2) {
                Util.warn("Parser", "Invalid range: " + input);
                return null;
            }

            int min = Parser.parseInt(tokens[0], -1);
            int max = Parser.parseInt(tokens[1], -1);

            if (min < 0 || max < 0) {
                Util.warn("Parser", "Invalid range: " + input);
                return null;
            }

            return new RangeInt(min, max);
        } else {
            int value = Parser.parseInt(input, -1);

            if (value < 0) {
                Util.warn("Parser", "Invalid range: " + input);
                return null;
            }

            return new RangeInt(value, value);
        }
    }

    public static RangeIntList parseRangeIntList(String input) {
        if (input == null) {
            return null;
        }

        RangeIntList list = new RangeIntList();
        String[] tokens = Util.tokenize(input, " ,");

        for (String token : tokens) {
            RangeInt range = Parser.parseRangeInt(token);

            if (range == null) {
                return null;
            }

            list.addRange(range);
        }

        return list;
    }

    public static Biome[] parseBiomes(String input) {
        if (input == null) {
            return null;
        }

        input = input.trim();

        boolean negative = false;

        if (input.startsWith("!")) {
            negative = true;
            input = input.substring(1);
        }

        String[] biomeNames = Util.tokenize(input, " ");
        List<Biome> biomes = new ArrayList<>();

        for (String biomeName : biomeNames) {
            Biome biome = Parser.findBiome(biomeName);

            if (biome == null) {
                Util.warn("Parser", "Biome not found: " + biomeName);
            } else {
                biomes.add(biome);
            }
        }

        if (negative) {
            List<Biome> allBiomes = Lists.newArrayList(Registry.BIOME.iterator());

            allBiomes.removeAll(biomes);

            biomes = allBiomes;
        }

        return biomes.toArray(new Biome[0]);
    }

    public static Biome findBiome(String biomeName) {
        biomeName = biomeName.toLowerCase();

        Identifier identifier = new Identifier(biomeName);
        Biome biome = Registry.BIOME.get(identifier);

        if (biome != null) {
            return biome;
        }

        String biomeNameCompact = biomeName.replace(" ", "").replace("_", "");
        identifier = new Identifier(biomeNameCompact);

        if (Parser.BIOMES_COMPACT == null) {
            Parser.BIOMES_COMPACT = new HashMap<>();

            for (Identifier id : Registry.BIOME.getIds()) {
                Parser.BIOMES_COMPACT.put(new Identifier(id.getNamespace(), id.getPath().replace(" ", "").replace("_", "")), Registry.BIOME.get(id));
            }
        }

        return Parser.BIOMES_COMPACT.get(identifier);
    }
}
