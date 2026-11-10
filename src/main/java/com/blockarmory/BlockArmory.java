package com.blockarmory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BlockArmory implements ModInitializer {
    public static final String MOD_ID = "blockarmory";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** All block gear shares one (tintable) worn-armor texture: assets/blockarmory/equipment/block.json */
    public static final ResourceKey<EquipmentAsset> ARMOR_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("block"));

    /** Nothing is in this tag by default; it only exists because tool/armor materials need a repair tag. */
    public static final TagKey<Item> REPAIR_TAG =
            TagKey.create(BuiltInRegistries.ITEM.key(), id("block_gear_repair"));

    /** For every kind of gear: block -> the gear item made from that block. */
    public static final Map<GearKind, Map<Block, Item>> GEAR = new EnumMap<>(GearKind.class);
    /** Reverse lookup used by the Uncrafting Table. */
    public static final Map<Item, GearInfo> GEAR_INFO = new HashMap<>();
    public static final Map<GearKind, RecipeSerializer<GearRecipe>> SERIALIZERS = new EnumMap<>(GearKind.class);

    public static Block UNCRAFTING_TABLE;

    public record GearInfo(GearKind kind, Item blockItem) {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        registerGear();
        registerUncraftingTable();
        registerRecipes();
        registerCreativeTabs();
    }

    // ------------------------------------------------------------------ gear

    private static void registerGear() {
        for (GearKind kind : GearKind.values()) {
            GEAR.put(kind, new LinkedHashMap<>());
        }

        // Snapshot the block registry first, so nothing we add can interfere with the loop.
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            blocks.add(block);
        }

        int blocksWithGear = 0;
        for (Block block : blocks) {
            Item blockItem = block.asItem();
            if (blockItem == Items.AIR || !(blockItem instanceof BlockItem bi) || bi.getBlock() != block) {
                continue; // blocks without an item (water, fire, wall torches, ...) can't be crafted with
            }

            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            String suffix = blockId.getNamespace().equals("minecraft")
                    ? blockId.getPath()
                    : blockId.getNamespace() + "_" + blockId.getPath();

            GearTier tier = GearTier.forBlock(block, blockId.getPath());
            int color = colorOf(block);
            boolean vanillaMakesTools = hasVanillaTools(blockId.getPath());

            for (GearKind kind : GearKind.values()) {
                // Planks / cobblestone / blackstone / cobbled deepslate already make (vanilla) tools,
                // so only armor is added for them. This avoids two recipes fighting over the same pattern.
                if (!kind.armor() && vanillaMakesTools) {
                    continue;
                }
                Item item = createGear(kind, suffix, block, tier, color);
                GEAR.get(kind).put(block, item);
                GEAR_INFO.put(item, new GearInfo(kind, blockItem));
            }
            blocksWithGear++;
        }
        LOGGER.info("Block Armory: created gear for {} blocks ({} items)", blocksWithGear, GEAR_INFO.size());
    }

    private static boolean hasVanillaTools(String path) {
        return path.endsWith("_planks")
                || path.equals("cobblestone")
                || path.equals("blackstone")
                || path.equals("cobbled_deepslate");
    }

    private static Item createGear(GearKind kind, String suffix, Block block, GearTier tier, int color) {
        Item.Properties props = new Item.Properties()
                .component(DataComponents.ITEM_MODEL, id(kind.id()))
                .component(DataComponents.ITEM_NAME,
                        Component.translatable("item." + MOD_ID + "." + kind.id(), block.getName()))
                .component(DataComponents.DYED_COLOR, new DyedItemColor(color));

        ToolMaterial tools = tier.toolMaterial();
        Function<Item.Properties, Item> factory = Item::new;

        switch (kind) {
            case PICKAXE -> props = props.pickaxe(tools, 1.0F, -2.8F);
            case SWORD -> props = props.sword(tools, 3.0F, -2.4F);
            case AXE -> factory = p -> new AxeItem(tools, 6.0F, -3.1F, p);
            case SHOVEL -> factory = p -> new ShovelItem(tools, 1.5F, -3.0F, p);
            case HOE -> factory = p -> new HoeItem(tools, -2.0F, -1.0F, p);
            case HELMET -> props = armor(props, tier, ArmorType.HELMET);
            case CHESTPLATE -> props = armor(props, tier, ArmorType.CHESTPLATE);
            case LEGGINGS -> props = armor(props, tier, ArmorType.LEGGINGS);
            case BOOTS -> props = armor(props, tier, ArmorType.BOOTS);
        }
        return register(kind.id() + "_" + suffix, factory, props);
    }

    private static Item.Properties armor(Item.Properties props, GearTier tier, ArmorType type) {
        return props.humanoidArmor(tier.armorMaterial(), type)
                .durability(type.getDurability(tier.armorBaseDurability()));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        Item item = factory.apply(props.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    /** The tint colour of a block's gear: the block's map colour (what you see on a map). */
    private static int colorOf(Block block) {
        MapColor mapColor = block.defaultBlockState().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        int rgb = mapColor.col & 0xFFFFFF;
        if (rgb == 0) {
            rgb = 0x9A9A9A;
        }
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        // very dark colours would make the item invisible, so lift them a little
        int max = Math.max(r, Math.max(g, b));
        if (max < 80) {
            int lift = 80 - max;
            r = Math.min(255, r + lift);
            g = Math.min(255, g + lift);
            b = Math.min(255, b + lift);
        }
        return (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------- uncrafting table

    private static void registerUncraftingTable() {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id("uncrafting_table"));
        UNCRAFTING_TABLE = Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new UncraftingTableBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(2.5F)
                        .sound(SoundType.WOOD)
                        .setId(blockKey))
        );

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id("uncrafting_table"));
        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(UNCRAFTING_TABLE, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())
        );
    }

    // ---------------------------------------------------------------- recipes

    private static void registerRecipes() {
        for (GearKind kind : GearKind.values()) {
            RecipeSerializer<GearRecipe> serializer =
                    new CustomRecipe.Serializer<>(category -> new GearRecipe(category, kind));
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("gear_" + kind.id()), serializer);
            SERIALIZERS.put(kind, serializer);
        }
    }

    // --------------------------------------------------------- creative tabs

    private static void registerCreativeTabs() {
        Item icon = GEAR.get(GearKind.PICKAXE).get(Blocks.DIAMOND_BLOCK);
        if (icon == null) {
            icon = Items.IRON_PICKAXE;
        }
        final Item tabIcon = icon;

        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("gear"));
        CreativeModeTab tab = FabricItemGroup.builder()
                .title(Component.translatable("itemGroup." + MOD_ID + ".gear"))
                .icon(() -> new ItemStack(tabIcon))
                .displayItems((parameters, output) -> {
                    output.accept(UNCRAFTING_TABLE);
                    for (GearKind kind : GearKind.values()) {
                        for (Item item : GEAR.get(kind).values()) {
                            output.accept(item);
                        }
                    }
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, tab);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(UNCRAFTING_TABLE));
    }
}
