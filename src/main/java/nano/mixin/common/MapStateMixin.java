package nano.mixin.common;

import java.util.List;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.google.common.collect.Lists;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.item.map.MapState.PlayerUpdateTracker;
import net.minecraft.text.Text;

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
	 * Do not render framed map icons because they look too similar to players on a green team.
	 */
	@Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isInFrame()Z"))
	private boolean isInFrameRedirect(ItemStack stack)
	{
		return false;
	}

	/**
	 * Embed team color information in the text field.
	 */
	@ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/map/MapState;addDecoration(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/world/WorldAccess;Ljava/lang/String;DDDLnet/minecraft/text/Text;)V", ordinal = 0))
	private void addIconModifyArgs(Args args)
	{
		String name = args.get(2);
		
		for(PlayerUpdateTracker tracker : updateTrackers)
		{
			if(tracker.player.getName().getString().equals(name))
			{
				args.set(6, Text.of(String.valueOf(tracker.player.getTeamColorValue())));
				break;
			}
		}
	}
}