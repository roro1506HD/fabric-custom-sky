package ovh.roro.customsky.util;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;

public class Blender {

    public static final int BLEND_ALPHA = 0;
    public static final int BLEND_ADD = 1;
    public static final int BLEND_SUBTRACT = 2;
    public static final int BLEND_MULTIPLY = 3;
    public static final int BLEND_DODGE = 4;
    public static final int BLEND_BURN = 5;
    public static final int BLEND_SCREEN = 6;
    public static final int BLEND_OVERLAY = 7;
    public static final int BLEND_REPLACE = 8;
    public static final int BLEND_DEFAULT = 1;

    public static int parseBlend(String input) {
        if (input == null) {
            return Blender.BLEND_DEFAULT;
        }

        input = input.toLowerCase().trim();
        switch (input) {
            case "alpha":
                return Blender.BLEND_ALPHA;
            case "add":
                return Blender.BLEND_ADD;
            case "subtract":
                return Blender.BLEND_SUBTRACT;
            case "multiply":
                return Blender.BLEND_MULTIPLY;
            case "dodge":
                return Blender.BLEND_DODGE;
            case "burn":
                return Blender.BLEND_BURN;
            case "screen":
                return Blender.BLEND_SCREEN;
            case "overlay":
                return Blender.BLEND_OVERLAY;
            case "replace":
                return Blender.BLEND_REPLACE;
        }

        Util.warn("Blender", "Unknown blend: " + input);
        return Blender.BLEND_DEFAULT;
    }

    public static void setupBlend(int blend, float brightness) {
        switch (blend) {
            case BLEND_ALPHA:
                GlStateManager.disableAlphaTest();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, brightness);
                break;
            case BLEND_ADD:
                GlStateManager.disableAlphaTest();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, brightness);
                break;
            case BLEND_SUBTRACT:
                GlStateManager.disableAlphaTest();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
                GlStateManager.color4f(brightness, brightness, brightness, 1.0F);
                break;
            case BLEND_MULTIPLY:
                GlStateManager.disableAlphaTest();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.color4f(brightness, brightness, brightness, brightness);
                break;
            case BLEND_DODGE:
                GlStateManager.disableAlphaTest();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
                GlStateManager.color4f(brightness, brightness, brightness, 1.0F);
                break;
            case BLEND_BURN:
                GlStateManager.disableAlphaTest();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
                GlStateManager.color4f(brightness, brightness, brightness, 1.0F);
                break;
            case BLEND_SCREEN:
                GlStateManager.disableAlphaTest();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);
                GlStateManager.color4f(brightness, brightness, brightness, 1.0F);
                break;
            case BLEND_OVERLAY:
                GlStateManager.disableAlphaTest();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
                GlStateManager.color4f(brightness, brightness, brightness, 1.0F);
                break;
            case BLEND_REPLACE:
                GlStateManager.enableAlphaTest();
                GlStateManager.disableBlend();
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, brightness);
                break;
        }
    }

    public static void clearBlend(float rainBrightness) {
        GlStateManager.disableAlphaTest();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, rainBrightness);
    }
}
