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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.entry.RegistryEntry;
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
		MinecraftClient client = MinecraftClient.getInstance();
		int layer = 0;
		
		// Ensure player icons are drawn on top of other icons.
		for(MapDecoration mapDecoration : state.getDecorations())
		{
			RegistryEntry<MapDecorationType> decorationType = mapDecoration.type();
			
			if(decorationType != MapDecorationTypes.PLAYER && decorationType != MapDecorationTypes.PLAYER_OFF_LIMITS && decorationType != MapDecorationTypes.PLAYER_OFF_MAP)
				layer++;
		}
		
		for(MapDecoration mapDecoration : state.getDecorations())
		{
			RegistryEntry<MapDecorationType> decorationType = mapDecoration.type();

			if(decorationType != MapDecorationTypes.PLAYER && decorationType != MapDecorationTypes.PLAYER_OFF_LIMITS && decorationType != MapDecorationTypes.PLAYER_OFF_MAP)
				continue;

			matrices.push();
			matrices.translate(0.0f + (float) mapDecoration.x() / 2.0f + 64.0f, 0.0f + (float) mapDecoration.z() / 2.0f + 64.0f, -0.02f);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (mapDecoration.rotation() * 360) / 16.0f));
			matrices.scale(4.0f, 4.0f, 3.0f);
			matrices.translate(-0.125f, 0.125f, 0.0f);
			Sprite sprite = client.getMapDecorationsAtlasManager().getSprite(mapDecoration);
            float minU = sprite.getMinU();
            float minV = sprite.getMinV();
            float maxU = sprite.getMaxU();
            float maxV = sprite.getMaxV();
			int color = mapDecoration.name().get() != null ? Integer.valueOf(mapDecoration.name().get().getString()) : 0xFFFFFF;
			int r = ColorHelper.Argb.getRed(color);
			int b = ColorHelper.Argb.getBlue(color);
			int g = ColorHelper.Argb.getGreen(color);
			Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
			VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(RenderLayer.getText(sprite.getAtlasId()));
			vertexConsumer2.vertex(matrix4f2, -1.0f, 1.0f, (float) layer * -0.001f).color(r, g, b, 255).texture(minU, minV).light(light);
			vertexConsumer2.vertex(matrix4f2, 1.0f, 1.0f, (float) layer * -0.001f).color(r, g, b, 255).texture(maxU, minV).light(light);
			vertexConsumer2.vertex(matrix4f2, 1.0f, -1.0f, (float) layer * -0.001f).color(r, g, b, 255).texture(maxU, maxV).light(light);
			vertexConsumer2.vertex(matrix4f2, -1.0f, -1.0f, (float) layer * -0.001f).color(r, g, b, 255).texture(minU, maxV).light(light);
			matrices.pop();
			layer++;
		}
	}
}