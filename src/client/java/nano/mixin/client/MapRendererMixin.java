package nano.mixin.client;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.render.MapRenderer$MapTexture")
public class MapRendererMixin
{
	@Shadow private MapState state;

	/**
	 * Do not allow vanilla rendering of player icons.
	 */
	@ModifyVariable(method = "draw", at = @At("HEAD"), ordinal = 0)
	private boolean hidePlayerIconsModifyVariable(boolean hidePlayerIcons)
	{
		return true;
	}

	/**
	 * Draw custom player icons.
	 */
	@Inject(method = "draw", at = @At(value = "TAIL"))
	private void drawInject(MatrixStack matrices, VertexConsumerProvider vertexConsumers, boolean hidePlayerIcons, int light, CallbackInfo info)
	{
		int layer = 0;
		
		// Ensure player icons are drawn on top of other icons.
		for(MapIcon mapIcon : state.getIcons())
		{
			byte iconType = mapIcon.getTypeId();
			
			if(iconType != 0 && iconType != 6 && iconType != 7)
				layer++;
		}
		
		for(MapIcon mapIcon : state.getIcons())
		{
			byte iconType = mapIcon.getTypeId();

			if(iconType != 0 && iconType != 6 && iconType != 7)
				continue;

			matrices.push();
			matrices.translate(0.0f + (float) mapIcon.x() / 2.0f + 64.0f, 0.0f + (float) mapIcon.z() / 2.0f + 64.0f, -0.02f);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (mapIcon.rotation() * 360) / 16.0f));
			matrices.scale(4.0f, 4.0f, 3.0f);
			matrices.translate(-0.125f, 0.125f, 0.0f);
			float e1 = (float) (iconType % 16 + 0) / 16.0f;
			float e2 = (float) (iconType / 16 + 0) / 16.0f;
			float e3 = (float) (iconType % 16 + 1) / 16.0f;
			float e4 = (float) (iconType / 16 + 1) / 16.0f;
			int color = mapIcon.text() != null ? Integer.valueOf(mapIcon.text().getString()) : 0xFFFFFF;
			int r = ColorHelper.Argb.getRed(color);
			int b = ColorHelper.Argb.getBlue(color);
			int g = ColorHelper.Argb.getGreen(color);
			Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
			VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(RenderLayer.getText(new Identifier("textures/map/map_icons.png")));
			vertexConsumer2.vertex(matrix4f2, -1.0f, 1.0f, (float) layer * -0.001f).color(r, g, b, 255).texture(e1, e2).light(light).next();
			vertexConsumer2.vertex(matrix4f2, 1.0f, 1.0f, (float) layer * -0.001f).color(r, g, b, 255).texture(e3, e2).light(light).next();
			vertexConsumer2.vertex(matrix4f2, 1.0f, -1.0f, (float) layer * -0.001f).color(r, g, b, 255).texture(e3, e4).light(light).next();
			vertexConsumer2.vertex(matrix4f2, -1.0f, -1.0f, (float) layer * -0.001f).color(r, g, b, 255).texture(e1, e4).light(light).next();
			matrices.pop();
			layer++;
		}
	}
}