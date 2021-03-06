/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 *
 * Quark is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 *
 * File Created @ [20/03/2016, 03:15:51 (GMT)]
 */
package vazkii.quark.tweaks.feature;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import vazkii.arl.util.RecipeHandler;
import vazkii.quark.base.module.Feature;

public class StairsMakeMore extends Feature {

	int targetSize;
	int originalSize;
	boolean reversionRecipe;

	@Override
	public void setupConfig() {
		targetSize = loadPropInt("Target stack size (must be a divisor of 24 if 'Reversion recipe' is enabled)", "", 8);
		originalSize = loadPropInt("Vanilla stack size", "The stack size for the vanilla stair recipe, used for automatically detecting stair recipes", 4);
		reversionRecipe = loadPropBool("Add stairs to blocks recipe", "", true);
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
		List<IRecipe> recipeList = new ArrayList(CraftingManager.getInstance().getRecipeList());
		for(IRecipe recipe : recipeList) {
			ItemStack output = recipe.getRecipeOutput();
			if(!output.isEmpty() && output.getCount() == originalSize) {
				Item outputItem = output.getItem();
				Block outputBlock = Block.getBlockFromItem(outputItem);
				if(outputBlock != null && outputBlock instanceof BlockStairs) {
					output.setCount(targetSize);

					if(recipe instanceof ShapedRecipes || recipe instanceof ShapedOreRecipe) {
						Object[] recipeItems;
						if(recipe instanceof ShapedRecipes)
							recipeItems = ((ShapedRecipes) recipe).recipeItems;
						else recipeItems = ((ShapedOreRecipe) recipe).getInput();

						ItemStack outStack = ItemStack.EMPTY;
						int inputItems = 0;

						for(Object recipeItem2 : recipeItems) {
							Object recipeItem = recipeItem2;
							if(recipeItem instanceof List) {
								List<ItemStack> ores = (List<ItemStack>) recipeItem;
								if(!ores.isEmpty())
									recipeItem = ores.get(0);
							}

							if(recipeItem != null) {
								ItemStack recipeStack = (ItemStack) recipeItem;
								if(outStack.isEmpty())
									outStack = recipeStack;
								
								if(ItemStack.areItemsEqual(outStack, recipeStack))
									inputItems++;
								else {
									outStack = ItemStack.EMPTY;
									break;
								}
							}
						}

						if(reversionRecipe && !outStack.isEmpty() && inputItems == 6) {
							ItemStack outCopy = outStack.copy();
							if(outCopy.getItemDamage() == OreDictionary.WILDCARD_VALUE)
								outCopy.setItemDamage(0);

							outCopy.setCount(24 / targetSize);
							ItemStack in = output.copy();
							RecipeHandler.addShapelessOreDictRecipe(outCopy, in, in, in, in);
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean requiresMinecraftRestartToEnable() {
		return true;
	}

}
