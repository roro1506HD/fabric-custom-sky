package ovh.roro.customsky.mixin;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.SkyProperties.SkyType;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ovh.roro.customsky.internal.CustomSky;
import ovh.roro.customsky.util.Util;

@Debug(export = true)
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements SynchronousResourceReloadListener, AutoCloseable {

	@Shadow private ClientWorld world;

	@Shadow @Final private TextureManager textureManager;

	@Shadow @Final private MinecraftClient client;

	@Shadow protected abstract void renderEndSky(MatrixStack matrices);

	@Shadow @Nullable private VertexBuffer lightSkyBuffer;

	@Shadow @Final private VertexFormat skyVertexFormat;

	@Shadow @Final private static Identifier SUN;

	@Shadow @Final private static Identifier MOON_PHASES;

	@Shadow @Nullable private VertexBuffer starsBuffer;

	@Shadow @Nullable private VertexBuffer darkSkyBuffer;

	@Inject(method = "apply", at = @At("RETURN"))
	public void injectResourceReload(ResourceManager manager, CallbackInfo callbackInfo) {
		CustomSky.update();
	}

	/**
	 * @author roro1506HD
	 * @reason Had to modify a if statement
	 */
	@Overwrite
	public void renderSky(MatrixStack matrices, float tickDelta) {
		if (this.client.world.getSkyProperties().getSkyType() == SkyType.END) {
			this.renderEndSky(matrices);
		} else if (this.client.world.getSkyProperties().getSkyType() == SkyType.NORMAL) {
			RenderSystem.disableTexture();
			Vec3d vec3d = this.world.method_23777(this.client.gameRenderer.getCamera().getBlockPos(), tickDelta);
			float f = (float)vec3d.x;
			float g = (float)vec3d.y;
			float h = (float)vec3d.z;
			BackgroundRenderer.setFogBlack();
			BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
			RenderSystem.depthMask(false);
			RenderSystem.enableFog();
			RenderSystem.color3f(f, g, h);
			this.lightSkyBuffer.bind();
			this.skyVertexFormat.startDrawing(0L);
			this.lightSkyBuffer.draw(matrices.peek().getModel(), 7);
			VertexBuffer.unbind();
			this.skyVertexFormat.endDrawing();
			RenderSystem.disableFog();
			RenderSystem.disableAlphaTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			float[] fs = this.world.getSkyProperties().getSkyColor(this.world.getSkyAngle(tickDelta), tickDelta);
			float r;
			float s;
			float o;
			float p;
			float q;
			if (fs != null) {
				RenderSystem.disableTexture();
				RenderSystem.shadeModel(7425);
				matrices.push();
				matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(90.0F));
				r = MathHelper.sin(this.world.getSkyAngleRadians(tickDelta)) < 0.0F ? 180.0F : 0.0F;
				matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(r));
				matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
				float j = fs[0];
				s = fs[1];
				float l = fs[2];
				Matrix4f matrix4f = matrices.peek().getModel();
				bufferBuilder.begin(6, VertexFormats.POSITION_COLOR);
				bufferBuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(j, s, l, fs[3]).next();

				for(int n = 0; n <= 16; ++n) {
					o = (float)n * 6.2831855F / 16.0F;
					p = MathHelper.sin(o);
					q = MathHelper.cos(o);
					bufferBuilder.vertex(matrix4f, p * 120.0F, q * 120.0F, -q * 40.0F * fs[3]).color(fs[0], fs[1], fs[2], 0.0F).next();
				}

				bufferBuilder.end();
				BufferRenderer.draw(bufferBuilder);
				matrices.pop();
				RenderSystem.shadeModel(7424);
			}

			RenderSystem.enableTexture();
			RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE, SrcFactor.ONE, DstFactor.ZERO);
			matrices.push();
			r = 1.0F - this.world.getRainGradient(tickDelta);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, r);
			matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
			CustomSky.renderSky(this.world, this.textureManager, matrices, tickDelta);
			matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(this.world.getSkyAngle(tickDelta) * 360.0F));
			Matrix4f matrix4f2 = matrices.peek().getModel();
			s = 30.0F;
			this.textureManager.bindTexture(SUN);
			bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(matrix4f2, -s, 100.0F, -s).texture(0.0F, 0.0F).next();
			bufferBuilder.vertex(matrix4f2, s, 100.0F, -s).texture(1.0F, 0.0F).next();
			bufferBuilder.vertex(matrix4f2, s, 100.0F, s).texture(1.0F, 1.0F).next();
			bufferBuilder.vertex(matrix4f2, -s, 100.0F, s).texture(0.0F, 1.0F).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			s = 20.0F;
			this.textureManager.bindTexture(MOON_PHASES);
			int t = this.world.getMoonPhase();
			int u = t % 4;
			int v = t / 4 % 2;
			float w = (float)(u + 0) / 4.0F;
			o = (float)(v + 0) / 2.0F;
			p = (float)(u + 1) / 4.0F;
			q = (float)(v + 1) / 2.0F;
			bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(matrix4f2, -s, -100.0F, s).texture(p, q).next();
			bufferBuilder.vertex(matrix4f2, s, -100.0F, s).texture(w, q).next();
			bufferBuilder.vertex(matrix4f2, s, -100.0F, -s).texture(w, o).next();
			bufferBuilder.vertex(matrix4f2, -s, -100.0F, -s).texture(p, o).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			RenderSystem.disableTexture();
			float aa = this.world.method_23787(tickDelta) * r;
			if (aa > 0.0F && !CustomSky.hasSkyLayers(this.world)) {
				RenderSystem.color4f(aa, aa, aa, aa);
				this.starsBuffer.bind();
				this.skyVertexFormat.startDrawing(0L);
				this.starsBuffer.draw(matrices.peek().getModel(), 7);
				VertexBuffer.unbind();
				this.skyVertexFormat.endDrawing();
			}

			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
			RenderSystem.enableAlphaTest();
			RenderSystem.enableFog();
			matrices.pop();
			RenderSystem.disableTexture();
			RenderSystem.color3f(0.0F, 0.0F, 0.0F);
			double d = this.client.player.getCameraPosVec(tickDelta).y - this.world.getLevelProperties().getSkyDarknessHeight();
			if (d < 0.0D) {
				matrices.push();
				matrices.translate(0.0D, 12.0D, 0.0D);
				this.darkSkyBuffer.bind();
				this.skyVertexFormat.startDrawing(0L);
				this.darkSkyBuffer.draw(matrices.peek().getModel(), 7);
				VertexBuffer.unbind();
				this.skyVertexFormat.endDrawing();
				matrices.pop();
			}

			if (this.world.getSkyProperties().isAlternateSkyColor()) {
				RenderSystem.color3f(f * 0.2F + 0.04F, g * 0.2F + 0.04F, h * 0.6F + 0.1F);
			} else {
				RenderSystem.color3f(f, g, h);
			}

			RenderSystem.enableTexture();
			RenderSystem.depthMask(true);
			RenderSystem.disableFog();
		}
	}
}
