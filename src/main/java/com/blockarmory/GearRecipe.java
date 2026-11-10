package com.blockarmory;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * One recipe per kind of gear (pickaxe, helmet, ...). It accepts the vanilla pattern
 * with ANY block that has gear, and the result depends on which block was used.
 */
public class GearRecipe extends CustomRecipe {
    private final GearKind kind;

    public GearRecipe(CraftingBookCategory category, GearKind kind) {
        super(category);
        this.kind = kind;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findBlock(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Block block = findBlock(input);
        if (block == null) {
            return ItemStack.EMPTY;
        }
        Item result = BlockArmory.GEAR.get(kind).get(block);
        return result == null ? ItemStack.EMPTY : new ItemStack(result);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return BlockArmory.SERIALIZERS.get(kind);
    }

    private Block findBlock(CraftingInput input) {
        String[] pattern = kind.pattern();
        if (input.width() != pattern[0].length() || input.height() != pattern.length) {
            return null;
        }
        Block found = tryPattern(input, pattern, false);
        if (found == null) {
            found = tryPattern(input, pattern, true); // mirrored (axe / hoe)
        }
        return found;
    }

    private Block tryPattern(CraftingInput input, String[] pattern, boolean mirror) {
        int width = pattern[0].length();
        Block block = null;
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < width; x++) {
                char wanted = pattern[y].charAt(mirror ? width - 1 - x : x);
                ItemStack stack = input.getItem(x + y * width);
                switch (wanted) {
                    case '.' -> {
                        if (!stack.isEmpty()) {
                            return null;
                        }
                    }
                    case 'S' -> {
                        if (!stack.is(Items.STICK)) {
                            return null;
                        }
                    }
                    default -> {
                        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                            return null;
                        }
                        Block candidate = blockItem.getBlock();
                        if (!BlockArmory.GEAR.get(kind).containsKey(candidate)) {
                            return null;
                        }
                        if (block == null) {
                            block = candidate;
                        } else if (block != candidate) {
                            return null;
                        }
                    }
                }
            }
        }
        return block;
    }
}
