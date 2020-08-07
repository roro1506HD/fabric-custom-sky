package ovh.roro.customsky.internal;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.ArrayUtils;
import ovh.roro.customsky.config.RangeIntList;
import ovh.roro.customsky.util.Blender;
import ovh.roro.customsky.util.Parser;
import ovh.roro.customsky.util.SmoothFloat;
import ovh.roro.customsky.util.Util;

public class CustomSkyLayer {

    private static final float[] DEFAULT_AXIS = {1.0F, 0.0F, 0.0F};
    private static final String WEATHER_CLEAR = "clear";
    private static final String WEATHER_RAIN = "rain";
    private static final String WEATHER_THUNDER = "thunder";

    private final String identifier;

    private String source;
    private int startFadeIn;
    private int endFadeIn;
    private int startFadeOut;
    private int endFadeOut;
    private int blend;
    private boolean rotate;
    private float speed;
    private float[] axis;
    private RangeIntList days;
    private int daysLoop;
    private boolean weatherClear;
    private boolean weatherRain;
    private boolean weatherThunder;
    private Biome[] biomes;
    private RangeIntList heights;
    private float transition;
    private SmoothFloat smoothPositionBrightness;
    private int textureId;
    private World lastWorld;

    public CustomSkyLayer(String identifier, Properties properties, String defaultSource) {
        this.identifier = identifier;
        this.source = null;
        this.startFadeIn = -1;
        this.endFadeIn = -1;
        this.startFadeOut = -1;
        this.endFadeOut = -1;
        this.blend = 1;
        this.rotate = false;
        this.speed = 1.0F;
        this.axis = CustomSkyLayer.DEFAULT_AXIS;
        this.days = null;
        this.daysLoop = 8;
        this.weatherClear = true;
        this.weatherRain = false;
        this.weatherThunder = false;
        this.biomes = null;
        this.heights = null;
        this.transition = 1.0F;
        this.smoothPositionBrightness = null;
        this.textureId = -1;
        this.lastWorld = null;

        this.source = properties.getProperty("source", defaultSource);
        this.startFadeIn = this.parseTime(properties.getProperty("startFadeIn"));
        this.endFadeIn = this.parseTime(properties.getProperty("endFadeIn"));
        this.startFadeOut = this.parseTime(properties.getProperty("startFadeOut"));
        this.endFadeOut = this.parseTime(properties.getProperty("endFadeOut"));
        this.blend = Blender.parseBlend(properties.getProperty("blend"));
        this.rotate = this.parseBoolean(properties.getProperty("rotate"), true);
        this.speed = this.parseFloat(properties.getProperty("speed"), 1.0f);
        this.axis = this.parseAxis(properties.getProperty("axis"), CustomSkyLayer.DEFAULT_AXIS);
        this.days = Parser.parseRangeIntList(properties.getProperty("days"));
        this.daysLoop = Parser.parseInt(properties.getProperty("daysLoop"), 8);
        Set<String> weatherList = this.parseWeatherList(properties.getProperty("weather", "clear"));
        this.weatherClear = weatherList.contains("clear");
        this.weatherRain = weatherList.contains("rain");
        this.weatherThunder = weatherList.contains("thunder");
        this.biomes = Parser.parseBiomes(properties.getProperty("biomes"));
        this.heights = Parser.parseRangeIntList(properties.getProperty("heights"));
        this.transition = this.parseFloat(properties.getProperty("transition"), 1.0f);

        Util.LOGGER.info(this.toString());
    }

    private Set<String> parseWeatherList(String input) {
        List<String> allowedInputs = Arrays.asList("clear", "rain", "thunder");
        Set<String> foundInputs = new HashSet<>();

        for (String token : Util.tokenize(input, " ")) {
            if (!allowedInputs.contains(token)) {
                Util.warn(this.identifier, "Unknown weather: " + token);
            } else {
                foundInputs.add(token);
            }
        }

        return foundInputs;
    }

    private int parseTime(String input) {
        if (input == null) {
            return -1;
        }

        String[] tokens = Util.tokenize(input, ":");

        if (tokens.length != 2) {
            Util.warn(this.identifier, "Invalid time: " + input);
            return -1;
        }

        int hours = Parser.parseInt(tokens[0], -1);
        int minutes = Parser.parseInt(tokens[1], -1);

        if (hours < 0 || hours > 24 || minutes < 0 || minutes > 59) {
            Util.warn(this.identifier, "Invalid time: " + input);
            return -1;
        }

        hours -= 6;

        if (hours < 0) {
            hours += 24;
        }

        return hours * 1000 + (int) (minutes / 60.0D * 1000);
    }

    private boolean parseBoolean(String input, boolean defaultValue) {
        if (input == null) {
            return defaultValue;
        }

        if (input.toLowerCase().equals("true")) {
            return true;
        }

        if (input.toLowerCase().equals("false")) {
            return false;
        }

        Util.warn(this.identifier, "Unknown boolean: " + input);
        return defaultValue;
    }

    private float parseFloat(String input, float defaultValue) {
        if (input == null) {
            return defaultValue;
        }

        float parsed = Parser.parseFloat(input, Float.MIN_VALUE);
        if (parsed == Float.MIN_VALUE) {
            Util.warn(this.identifier, "Invalid float: " + input);
            return defaultValue;
        }

        return parsed;
    }

    private float[] parseAxis(String input, float[] defaultValue) {
        if (input == null) {
            return defaultValue;
        }

        String[] tokens = Util.tokenize(input, " ");

        if (tokens.length != 3) {
            Util.warn(this.identifier, "Invalid axis: " + input);
            return defaultValue;
        }

        float[] floats = new float[3];
        float tempFloat;

        for (int i = 0; i < tokens.length; i++) {
            tempFloat = floats[i] = Parser.parseFloat(tokens[i], Float.MIN_VALUE);

            if (tempFloat == Float.MIN_VALUE) {
                Util.warn(this.identifier, "Invalid axis: " + input);
                return defaultValue;
            }
        }

        float x = floats[0];
        float y = floats[1];
        float z = floats[2];

        if (x * x + y * y + z * z < 0.00001f) {
            Util.warn(this.identifier, "Invalid axis values: " + input);
            return defaultValue;
        }

        return new float[]{x, y, z};
    }

    public boolean isValid(String path) {
        if (this.source == null) {
            Util.warn(this.identifier, "No source texture: " + path);
            return false;
        }

        this.source = Util.fixResourcePath(this.source, Util.getBasePath(path));

        if (this.startFadeIn < 0 || this.endFadeIn < 0 || this.endFadeOut < 0) {
            Util.warn(this.identifier, "Invalid times, required are: startFadeIn, endFadeIn and endFadeOut");
            return false;
        }

        int timeFadeIn = this.normalizeTime(this.endFadeIn - this.startFadeIn);

        if (this.startFadeOut < 0) {
            this.startFadeOut = this.normalizeTime(this.endFadeOut - timeFadeIn);

            if (this.timeBetween(this.startFadeOut, this.startFadeIn, this.endFadeIn)) {
                this.startFadeOut = this.endFadeIn;
            }
        }

        int timeOn = this.normalizeTime(this.startFadeOut - this.endFadeIn);
        int timeFadeOut = this.normalizeTime(this.endFadeOut - this.startFadeOut);
        int timeOff = this.normalizeTime(this.startFadeIn - this.endFadeOut);
        int timeSum = timeFadeIn + timeOn + timeFadeOut + timeOff;

        if (timeSum != 24000) {
            Util.warn(this.identifier, "Invalid fadeIn/fadeOut times, sum is not 24h (24000): " + timeSum);
            return false;
        }

        if (this.speed < 0.0F) {
            Util.warn(this.identifier, "Invalid speed: " + this.speed);
            return false;
        }

        if (this.daysLoop <= 0) {
            Util.warn(this.identifier, "Invalid daysLoop: " + this.daysLoop);
            return false;
        }

        return true;
    }

    public void render(World world, MatrixStack matrixStack, int timeOfDay, float celestialAngle, float rainStrength, float thunderStrength) {
        float positionBrightness = getPositionBrightness(world);
        float weatherBrightness = getWeatherBrightness(rainStrength, thunderStrength);
        float fadeBrightness = getFadeBrightness(timeOfDay);
        float brightness = positionBrightness * weatherBrightness * fadeBrightness;

        brightness = MathHelper.clamp(brightness, 0.0F, 1.0F);

        //Util.LOGGER.info("[" + this.identifier + "] positionBrightness: " + positionBrightness + " weatherBrightness: " + weatherBrightness + " fadeBrightness: " + fadeBrightness + " brightness: " + brightness);

        if (brightness < 0.0001f) {
            return;
        }

        GlStateManager.bindTexture(this.textureId);
        Blender.setupBlend(this.blend, brightness);
        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(matrixStack.peek().getModel());

        if (this.rotate) {
            float angleDayStart = 0.0F;

            if (this.speed != Math.round(this.speed)) {
                long worldDay = (world.getTimeOfDay() + 18000L) / 24000L;
                double anglePerDay = this.speed % 1.0F;
                double angleDayNow = worldDay * anglePerDay;
                angleDayStart = (float) (angleDayNow % 1.0F);
            }

            GlStateManager.rotatef(360.0F * (angleDayStart + celestialAngle * this.speed), this.axis[0], this.axis[1], this.axis[2]);
        }

        Tessellator tessellator = Tessellator.getInstance();

        GlStateManager.rotatef(90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotatef(-90.0F, 0.0F, 0.0F, 1.0F);
        this.renderSide(tessellator, 4);
        GlStateManager.pushMatrix();
        GlStateManager.rotatef(90.0F, 1.0F, 0.0F, 0.0F);
        this.renderSide(tessellator, 1);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.rotatef(-90.0F, 1.0F, 0.0F, 0.0F);
        this.renderSide(tessellator, 0);
        GlStateManager.popMatrix();
        GlStateManager.rotatef(90.0F, 0.0F, 0.0F, 1.0F);
        this.renderSide(tessellator, 5);
        GlStateManager.rotatef(90.0F, 0.0F, 0.0F, 1.0F);
        this.renderSide(tessellator, 2);
        GlStateManager.rotatef(90.0F, 0.0F, 0.0F, 1.0F);
        this.renderSide(tessellator, 3);
        GlStateManager.popMatrix();
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void renderSide(Tessellator tessellator, int side) {
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        float tx = side % 3 / 3.0F;
        float ty = side / 3 / 2.0F;

        bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(-100.0D, -100.0D, -100.0D).texture(tx, ty).next();
        bufferBuilder.vertex(-100.0D, -100.0D, 100.0D).texture(tx, ty + 0.5F).next();
        bufferBuilder.vertex(100.0D, -100.0D, 100.0D).texture(tx + 0.33333334F, ty + 0.5F).next();
        bufferBuilder.vertex(100.0D, -100.0D, -100.0D).texture(tx + 0.33333334F, ty).next();
        tessellator.draw();
    }

    private float getPositionBrightness(World world) {
        if (this.biomes == null && this.heights == null) {
            return 1.0F;
        }

        float positionBrightness = this.getPositionBrightnessRaw(world);

        if (this.smoothPositionBrightness == null) {
            this.smoothPositionBrightness = new SmoothFloat(positionBrightness, this.transition);
        }

        return this.smoothPositionBrightness.getSmoothValue(positionBrightness);
    }

    private float getPositionBrightnessRaw(World world) {
        Entity renderViewEntity = MinecraftClient.getInstance().getCameraEntity();

        if (renderViewEntity == null) {
            return 0.0F;
        }

        BlockPos pos = renderViewEntity.getBlockPos();

        if (this.biomes != null) {
            Biome biome = world.getBiome(pos);

            if (biome == null) {
                return 0.0F;
            }

            if (!ArrayUtils.contains(this.biomes, biome)) {
                return 0.0F;
            }
        }

        if (this.heights != null && !this.heights.isInRange(pos.getY())) {
            return 0.0F;
        }

        return 1.0F;
    }

    private float getWeatherBrightness(float rainStrength, float thunderStrength) {
        float clearBrightness = 1.0F - rainStrength;
        float rainBrightness = rainStrength - thunderStrength;
        float thunderBrightness = thunderStrength;
        float weatherBrightness = 0.0F;

        if (this.weatherClear) {
            weatherBrightness += clearBrightness;
        }

        if (this.weatherRain) {
            weatherBrightness += rainBrightness;
        }

        if (this.weatherThunder) {
            weatherBrightness += thunderBrightness;
        }

        return MathHelper.clamp(weatherBrightness, 0.0F, 1.0F);
    }

    private float getFadeBrightness(int timeOfDay) {
        if (this.timeBetween(timeOfDay, this.startFadeIn, this.endFadeIn)) {
            int timeFadeIn = this.normalizeTime(this.endFadeIn - this.startFadeIn);
            int timeDiff = this.normalizeTime(timeOfDay - this.startFadeIn);

            //Util.LOGGER.info("timeFadeIn: " + timeFadeIn + " timeDiff: " + timeDiff);

            return timeDiff / (float) timeFadeIn;
        }

        if (this.timeBetween(timeOfDay, this.endFadeIn, this.startFadeOut)) {
            return 1.0F;
        }

        if (this.timeBetween(timeOfDay, this.startFadeOut, this.endFadeOut)) {
            int timeFadeOut = this.normalizeTime(this.endFadeOut - this.startFadeOut);
            int timeDiff = this.normalizeTime(timeOfDay - this.startFadeOut);

            //Util.LOGGER.info("timeFadeOut: " + timeFadeOut + " timeDiff: " + timeDiff);

            return 1.0F - timeDiff / (float) timeFadeOut;
        }

        return 0.0F;
    }

    private int normalizeTime(int time) {
        while (time >= 24000) {
            time -= 24000;
        }

        while (time < 0) {
            time += 24000;
        }

        return time;
    }

    private boolean timeBetween(int timeOfDay, int timeStart, int timeEnd) {
        if (timeStart <= timeEnd) {
            return timeOfDay >= timeStart && timeOfDay <= timeEnd;
        }

        return timeOfDay >= timeStart || timeOfDay <= timeEnd;
    }

    public boolean isActive(World world, int timeOfDay) {
        if (!world.equals(this.lastWorld)) {
            this.lastWorld = world;
            this.smoothPositionBrightness = null;
        }

        if (this.timeBetween(timeOfDay, this.endFadeOut, this.startFadeIn)) {
            return false;
        }

        if (this.days != null) {
            long time = world.getTimeOfDay();
            long timeShift;

            for (timeShift = time - this.startFadeIn; timeShift < 0L; timeShift += 24000 * this.daysLoop)
                ;

            int day = (int) (timeShift / 24000L);
            int dayOfLoop = day % this.daysLoop;

            return this.days.isInRange(dayOfLoop);
        }

        return true;
    }

    public String getSource() {
        return this.source;
    }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
    }

    @Override
    public String toString() {
        return "CustomSkyLayer{" +
                "source='" + this.source + '\'' +
                ", startFadeIn=" + this.startFadeIn +
                ", endFadeIn=" + this.endFadeIn +
                ", startFadeOut=" + this.startFadeOut +
                ", endFadeOut=" + this.endFadeOut +
                ", blend=" + this.blend +
                ", rotate=" + this.rotate +
                ", speed=" + this.speed +
                ", axis=" + Arrays.toString(this.axis) +
                ", days=" + this.days +
                ", daysLoop=" + this.daysLoop +
                ", weatherClear=" + this.weatherClear +
                ", weatherRain=" + this.weatherRain +
                ", weatherThunder=" + this.weatherThunder +
                ", biomes=" + Arrays.toString(this.biomes) +
                ", heights=" + this.heights +
                ", transition=" + this.transition +
                ", smoothPositionBrightness=" + this.smoothPositionBrightness +
                ", textureId=" + this.textureId +
                ", lastWorld=" + this.lastWorld +
                '}';
    }
}
