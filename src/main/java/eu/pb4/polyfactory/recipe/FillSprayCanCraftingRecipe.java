package eu.pb4.polyfactory.recipe;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.DyeSprayItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.recipe.press.PressRecipe;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.World;

public record FillSprayCanCraftingRecipe(CraftingRecipeCategory category) implements CraftingRecipe {
    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        ItemStack can = ItemStack.EMPTY;
        int dye = -1;
        int count = 0;
        for (var stack : inventory.getHeldStacks()) {
            if (stack.isOf(FactoryItems.SPRAY_CAN)) {
                if (can.isEmpty()) {
                    can = stack;
                } else {
                    return false;
                }
            } else if (stack.isIn(ConventionalItemTags.DYES)) {
                count++;
                if (dye == -1) {
                    dye = DyeColorExtra.getColor(stack);
                } else if (dye != DyeColorExtra.getColor(stack)) {
                    return false;
                }
            } else if (!stack.isEmpty()) {
                return false;
            }

        }

        return !can.isEmpty() && dye != -1
                && (DyeSprayItem.getUses(can) == 0 || (DyeSprayItem.getUses(can) + 8 * count <= DyeSprayItem.MAX_USES && ColoredItem.getColor(can) == dye));
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        ItemStack can = ItemStack.EMPTY;
        int dye = -1;
        int dyeCount = 0;
        for (var stack : inventory.getHeldStacks()) {
            if (stack.isOf(FactoryItems.SPRAY_CAN)) {
                can = stack.copy();
            } else if (stack.isIn(ConventionalItemTags.DYES)) {
                dyeCount++;
                if (dye == -1) {
                    dye = DyeColorExtra.getColor(stack);
                }
            }
        }

        ColoredItem.setColor(can, dye);
        DyeSprayItem.setUses(can, DyeSprayItem.getUses(can) + 8 * dyeCount);

        return can;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResult(DynamicRegistryManager registryManager) {
        return FactoryItems.SPRAY_CAN.getDefaultStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_FILL_SPRAY_CAN;
    }

    @Override
    public CraftingRecipeCategory getCategory() {
        return category;
    }
}
