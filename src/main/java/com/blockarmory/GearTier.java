package com.blockarmory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * How strong the gear made from a block is. The tier is picked from the block's hardness
 * (with a few hand-picked exceptions for metal blocks), so tougher blocks make better gear.
 */
public enum GearTier {
    //            durability speed dmg ench  mining-limit tag                           armorDur  helm chest legs boots  tough kb   equip sound
    CRUDE(          60,  2.0F, 0.0F,  6, BlockTags.INCORRECT_FOR_WOODEN_TOOL,    4, 1, 2, 1, 1, 0.0F, 0.0F, SoundEvents.ARMOR_EQUIP_LEATHER),
    STONE(         160,  4.0F, 1.0F,  5, BlockTags.INCORRECT_FOR_STONE_TOOL,    12, 1, 4, 3, 1, 0.0F, 0.0F, SoundEvents.ARMOR_EQUIP_CHAIN),
    IRON(          300,  6.0F, 2.0F, 12, BlockTags.INCORRECT_FOR_IRON_TOOL,     15, 2, 6, 5, 2, 0.0F, 0.0F, SoundEvents.ARMOR_EQUIP_IRON),
    GOLD(           70, 12.0F, 0.0F, 25, BlockTags.INCORRECT_FOR_WOODEN_TOOL,    8, 2, 5, 3, 1, 0.0F, 0.0F, SoundEvents.ARMOR_EQUIP_GOLD),
    DIAMOND(      1200,  8.0F, 3.0F, 10, BlockTags.INCORRECT_FOR_DIAMOND_TOOL,  33, 3, 8, 6, 3, 2.0F, 0.0F, SoundEvents.ARMOR_EQUIP_DIAMOND),
    NETHERITE(    2300,  9.0F, 4.0F, 15, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 38, 3, 8, 6, 3, 3.0F, 0.1F, SoundEvents.ARMOR_EQUIP_NETHERITE),
    MYTHIC(       4000, 12.0F, 6.0F, 20, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 55, 4, 9, 7, 4, 4.0F, 0.2F, SoundEvents.ARMOR_EQUIP_NETHERITE);

    private final ToolMaterial toolMaterial;
    private final ArmorMaterial armorMaterial;
    private final int armorBaseDurability;

    GearTier(int toolDurability, float speed, float damageBonus, int enchantability,
             TagKey<Block> incorrectBlocks, int armorDurability,
             int helmet, int chest, int legs, int boots,
             float toughness, float knockbackResistance, Holder<SoundEvent> equipSound) {
        this.toolMaterial = new ToolMaterial(
                incorrectBlocks, toolDurability, speed, damageBonus, enchantability, BlockArmory.REPAIR_TAG);
        this.armorBaseDurability = armorDurability;
        this.armorMaterial = new ArmorMaterial(
                armorDurability,
                Map.of(
                        ArmorType.HELMET, helmet,
                        ArmorType.CHESTPLATE, chest,
                        ArmorType.LEGGINGS, legs,
                        ArmorType.BOOTS, boots
                ),
                enchantability,
                equipSound,
                toughness,
                knockbackResistance,
                BlockArmory.REPAIR_TAG,
                BlockArmory.ARMOR_ASSET
        );
    }

    public ToolMaterial toolMaterial() {
        return toolMaterial;
    }

    public ArmorMaterial armorMaterial() {
        return armorMaterial;
    }

    public int armorBaseDurability() {
        return armorBaseDurability;
    }

    /** Picks the tier for a block from its registry path and hardness. */
    public static GearTier forBlock(Block block, String path) {
        if (path.equals("iron_block") || path.contains("copper")) {
            return IRON;
        }
        if (path.contains("gold_block")) {
            return GOLD;
        }
        float hardness = block.defaultBlockState().getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (hardness < 0.0F) {
            return MYTHIC;       // unbreakable blocks (bedrock, command blocks, ...)
        }
        if (hardness >= 15.0F) {
            return NETHERITE;    // obsidian, ancient debris, ender chest, ...
        }
        if (hardness >= 4.5F) {
            return DIAMOND;      // diamond/emerald block, anvils, ...
        }
        if (hardness >= 2.5F) {
            return IRON;         // ores, deepslate, end stone, ...
        }
        if (hardness >= 1.0F) {
            return STONE;        // stone, planks, logs, bricks, ...
        }
        return CRUDE;            // dirt, sand, leaves, wool, ...
    }
}
