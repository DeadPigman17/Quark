/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 * 
 * Quark is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * 
 * File Created @ [Aug 10, 2016, 4:35:37 PM (GMT)]
 */
package vazkii.quark.tweaks.feature;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import vazkii.quark.base.module.Feature;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.decoration.feature.IronLadders;

public class DeployLaddersDown extends Feature {

	@SubscribeEvent
	public void onInteract(PlayerInteractEvent.RightClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		EnumHand hand = event.getHand();
		ItemStack stack = player.getHeldItem(hand);

		List<Item> items = new ArrayList();
		items.add(Item.getItemFromBlock(Blocks.LADDER));
		if(ModuleLoader.isFeatureEnabled(IronLadders.class))
			items.add(Item.getItemFromBlock(IronLadders.iron_ladder));
		
		if(!stack.isEmpty() && items.contains(stack.getItem())) {
			Block block = Block.getBlockFromItem(stack.getItem());
			World world = event.getWorld();
			BlockPos pos = event.getPos();
			while(world.getBlockState(pos).getBlock() == block) {
				BlockPos posDown = pos.down();
				IBlockState stateDown = world.getBlockState(posDown);

				if(stateDown.getBlock() == block)
					pos = posDown;
				else {
					if(stateDown.getBlock().isAir(stateDown, world, posDown)) {
						IBlockState copyState = world.getBlockState(pos);

						EnumFacing facing = copyState.getValue(BlockLadder.FACING);
						if(block.canPlaceBlockOnSide(world, posDown, facing)) {
							world.setBlockState(posDown, copyState);

							event.setCanceled(true);
							
							if(world.isRemote)
								player.swingArm(hand);
							
							if(!player.capabilities.isCreativeMode) {
								stack.shrink(1);
								
								if(stack.getCount() <= 0)
									player.setHeldItem(hand, ItemStack.EMPTY);
							}
						}
					}
					break;
				} 
			}
		}
	}

	@Override
	public boolean hasSubscriptions() {
		return true;
	}

}
