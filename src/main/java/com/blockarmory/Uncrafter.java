package com.blockarmory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The uncrafting logic: turns an item back into the ingredients of the recipe that makes it. */
public final class Uncrafter {
    private static final CraftingInput EMPTY_INPUT =
            CraftingInput.of(3, 3, Collections.nCopies(9, ItemStack.EMPTY));

    private Uncrafter() {
    }

    private record Plan(int consume, Map<Item, Integer> outputs) {
    }

    public static void uncraft(ServerLevel level, BlockPos pos, Player player, ItemStack held) {
        Plan plan = plan(level, held);
        if (plan == null) {
            player.displayClientMessage(Component.translatable("message.blockarmory.cannot_uncraft"), true);
            return;
        }

        held.shrink(plan.consume());
        for (ItemStack stack : toStacks(plan.outputs())) {
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false); // inventory full: drop what's left
            }
        }
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
    }

    private static Plan plan(ServerLevel level, ItemStack held) {
        // Damaged items give back proportionally less.
        float ratio = 1.0F;
        if (held.isDamageableItem() && held.getMaxDamage() > 0) {
            ratio = Math.max(0.0F, 1.0F - (float) held.getDamageValue() / (float) held.getMaxDamage());
        }

        // 1) Gear made by this mod: gives the blocks (and sticks) back.
        BlockArmory.GearInfo gear = BlockArmory.GEAR_INFO.get(held.getItem());
        if (gear != null) {
            Map<Item, Integer> out = new LinkedHashMap<>();
            out.put(gear.blockItem(), Math.max(1, Math.round(gear.kind().blockCount() * ratio)));
            int sticks = Math.round(gear.kind().stickCount() * ratio);
            if (sticks > 0) {
                out.put(Items.STICK, sticks);
            }
            return new Plan(1, out);
        }

        // 2) Everything else: find a normal crafting recipe that makes this item.
        Item item = held.getItem();
        CraftingRecipe best = null;
        int bestCount = 0;
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) {
                continue;
            }
            if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) {
                continue;
            }
            ItemStack result;
            try {
                result = recipe.assemble(EMPTY_INPUT, level.registryAccess());
            } catch (RuntimeException e) {
                continue;
            }
            if (result.isEmpty() || !result.is(item)) {
                continue;
            }
            int count = result.getCount();
            if (count > held.getCount() || count <= bestCount) {
                continue; // not enough items, or a worse match than one we already have
            }
            if (recipe.placementInfo().ingredients().isEmpty()) {
                continue;
            }
            best = recipe;
            bestCount = count;
        }
        if (best == null) {
            return null;
        }

        Map<Item, Integer> out = new LinkedHashMap<>();
        for (Ingredient ingredient : best.placementInfo().ingredients()) {
            Optional<Holder<Item>> first = ingredient.items().findFirst();
            if (first.isPresent()) {
                out.merge(first.get().value(), 1, Integer::sum);
            }
        }
        if (out.isEmpty()) {
            return null;
        }

        if (ratio < 1.0F) {
            Map<Item, Integer> scaled = new LinkedHashMap<>();
            for (Map.Entry<Item, Integer> e : out.entrySet()) {
                int n = Math.round(e.getValue() * ratio);
                if (n > 0) {
                    scaled.put(e.getKey(), n);
                }
            }
            if (scaled.isEmpty()) {
                Map.Entry<Item, Integer> firstEntry = out.entrySet().iterator().next();
                scaled.put(firstEntry.getKey(), 1);
            }
            out = scaled;
        }
        return new Plan(bestCount, out);
    }

    private static List<ItemStack> toStacks(Map<Item, Integer> outputs) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : outputs.entrySet()) {
            int remaining = entry.getValue();
            int max = new ItemStack(entry.getKey()).getMaxStackSize();
            while (remaining > 0) {
                int n = Math.min(max, remaining);
                stacks.add(new ItemStack(entry.getKey(), n));
                remaining -= n;
            }
        }
        return stacks;
    }
}
