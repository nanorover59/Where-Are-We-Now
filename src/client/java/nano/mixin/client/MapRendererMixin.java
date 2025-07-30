package nano.mixin.client;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Colors;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
@Mixin(MapRenderer.class)
public class MapRendererMixin
{
	/**
	 * Draw custom player icons.
	 */
	@Inject(method = "draw", at = @At(value = "HEAD"), cancellable = true)
	private void drawInject(MapRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, boolean hidePlayerIcons, int light, CallbackInfo info)
	{
		MinecraftClient client = MinecraftClient.getInstance();
		Matrix4f matrix4f = matrices.peek().getPositionMatrix();
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(state.texture));
		vertexConsumer.vertex(matrix4f, 0.0F, 128.0F, -0.01F).color(Colors.WHITE).texture(0.0F, 1.0F).light(light);
		vertexConsumer.vertex(matrix4f, 128.0F, 128.0F, -0.01F).color(Colors.WHITE).texture(1.0F, 1.0F).light(light);
		vertexConsumer.vertex(matrix4f, 128.0F, 0.0F, -0.01F).color(Colors.WHITE).texture(1.0F, 0.0F).light(light);
		vertexConsumer.vertex(matrix4f, 0.0F, 0.0F, -0.01F).color(Colors.WHITE).texture(0.0F, 0.0F).light(light);
		int i = 0;

		for(MapRenderState.Decoration decoration : state.decorations)
		{
			matrices.push();
			matrices.translate(decoration.x / 2.0F + 64.0F, decoration.z / 2.0F + 64.0F, -0.02F);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(decoration.rotation * 360 / 16.0F));
			matrices.scale(4.0F, 4.0F, 3.0F);
			matrices.translate(-0.125F, 0.125F, 0.0F);
			Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
			Sprite sprite = decoration.sprite;
			
			if(sprite != null)
			{
				int color = Colors.WHITE;
				
				if(decoration.name != null && decoration.name.getString().contains("COLOR:"))
				{
					String[] split = decoration.name.getString().split(":");
					
					if(split.length > 1)
						color = Integer.valueOf(split[1]);
				}
				
				int r = ColorHelper.getRed(color);
				int b = ColorHelper.getBlue(color);
				int g = ColorHelper.getGreen(color);
				VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(RenderLayer.getText(sprite.getAtlasId()));
				vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, i * -0.001F).color(r, g, b, 255).texture(sprite.getMinU(), sprite.getMinV()).light(light);
				vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, i * -0.001F).color(r, g, b, 255).texture(sprite.getMaxU(), sprite.getMinV()).light(light);
				vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, i * -0.001F).color(r, g, b, 255).texture(sprite.getMaxU(), sprite.getMaxV()).light(light);
				vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, i * -0.001F).color(r, g, b, 255).texture(sprite.getMinU(), sprite.getMaxV()).light(light);
				matrices.pop();
			}

			if(decoration.name != null && !decoration.name.getString().contains("COLOR:"))
			{
				TextRenderer textRenderer = client.textRenderer;
				float f = textRenderer.getWidth(decoration.name);
				float g = MathHelper.clamp(25.0F / f, 0.0F, 6.0F / 9.0F);
				matrices.push();
				matrices.translate(decoration.x / 2.0F + 64.0F - f * g / 2.0F, decoration.z / 2.0F + 64.0F + 4.0F, -0.025F);
				matrices.scale(g, g, -1.0F);
				matrices.translate(0.0F, 0.0F, 0.1F);
				textRenderer.draw(decoration.name, 0.0F, 0.0F, -1, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, Integer.MIN_VALUE, light);
				matrices.pop();
			}

			i++;
		}
		
		info.cancel();
	}
}