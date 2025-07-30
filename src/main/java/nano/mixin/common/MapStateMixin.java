package nano.mixin.common;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.google.common.collect.Lists;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.item.map.MapState.PlayerUpdateTracker;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.WorldAccess;

@Mixin(MapState.class)
public class MapStateMixin
{
	@Shadow private final List<PlayerUpdateTracker> updateTrackers = Lists.newArrayList();
	
	/**
	 * Do not remove any player icons.
	 */
	@Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;contains(Ljava/util/function/Predicate;)Z"))
	private boolean containsRedirect(PlayerInventory inventory, Predicate<ItemStack> predicate)
	{
		return true;
	}
	
	/**
	 * Required to track players while in a frame.
	 */
	@Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isInFrame()Z"))
	private boolean isInFrameRedirect(ItemStack stack)
	{
		return false;
	}
	
	/**
	 * Do not render framed map icons because they look too similar to players on a green team.
	 */
	@Inject(method = "addDecoration", at = @At(value = "HEAD"), cancellable = true)
	private void addDecorationRedirect(RegistryEntry<MapDecorationType> type, @Nullable WorldAccess world, String key, double x, double z, double rotation, @Nullable Text text, CallbackInfo info)
	{
		if(type == MapDecorationTypes.FRAME)
			info.cancel();
	}

	/**
	 * Embed color information in the text field.
	 */
	@ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/map/MapState;addDecoration(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/world/WorldAccess;Ljava/lang/String;DDDLnet/minecraft/text/Text;)V"))
	private void addIconModifyArgs(Args args)
	{
		RegistryEntry<MapDecorationType> type = args.get(0);
		String name = args.get(2);
		int defaultColorIndex = 15;
		
		if(type == MapDecorationTypes.PLAYER)
		{
			for(PlayerUpdateTracker tracker : updateTrackers)
			{
				if(tracker.player.getName().getString().equals(name))
				{
					if(tracker.player.getScoreboardTeam() != null)
						args.set(6, Text.of("COLOR:" + String.valueOf(tracker.player.getTeamColorValue())));
					else
						args.set(6, Text.of("COLOR:" + String.valueOf(Formatting.byColorIndex(defaultColorIndex).getColorValue())));
					
					break;
				}
				
				defaultColorIndex++;
				
				if(defaultColorIndex > 15)
					defaultColorIndex = 0;
			}
		}
	}
}