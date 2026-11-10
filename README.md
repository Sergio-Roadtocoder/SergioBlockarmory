# Block Armory (Fabric, Minecraft 1.21.11)

Every block in the game can be crafted into a pickaxe, axe, shovel, hoe, sword, helmet,
chestplate, leggings and boots. An **Uncrafting Table** turns items back into their ingredients.

## Build the .jar

**Option A - no install, from your phone/PC browser (GitHub Actions)**
1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the *Actions* tab -> *Build mod* -> wait for the green check.
3. Open the finished run and download the **blockarmory-jar** artifact. The jar inside is the mod.

**Option B - on your computer**
1. Install JDK 21 and Gradle 9.2+ (or run `gradle wrapper` once to create the wrapper).
2. In this folder run: `gradle build`
3. The mod is `build/libs/blockarmory-1.0.0.jar`.

Install: put the jar and the **Fabric API** jar (1.21.11) in your `mods` folder, with Fabric Loader 0.18+.

## How it works

### Crafting (same shapes as vanilla, but with ANY block)
| Item | Pattern (B = same block, S = stick) |
|---|---|
| Pickaxe | `BBB / .S. / .S.` |
| Axe | `BB / BS / .S` (mirrored works) |
| Shovel | `B / S / S` |
| Hoe | `BB / .S / .S` (mirrored works) |
| Sword | `B / B / S` |
| Helmet | `BBB / B.B` |
| Chestplate | `B.B / BBB / BBB` |
| Leggings | `BBB / B.B / B.B` |
| Boots | `B.B / B.B` |

Planks, cobblestone, blackstone and cobbled deepslate already make vanilla tools, so those blocks
only get the armor set (otherwise two recipes would compete for the same pattern).

### Strength (picked from the block's hardness)
| Tier | Blocks (examples) | Tools: durability / speed / dmg bonus | Armor (helm/chest/legs/boots) |
|---|---|---|---|
| Crude | dirt, sand, leaves, wool | 60 / 2 / +0 | 1/2/1/1 |
| Stone | stone, planks, logs, bricks | 160 / 4 / +1 | 1/4/3/1 |
| Iron | ores, deepslate, end stone, iron & copper blocks | 300 / 6 / +2 | 2/6/5/2 |
| Gold | gold blocks | 70 / 12 / +0, enchantability 25 | 2/5/3/1 |
| Diamond | diamond/emerald block, anvil | 1200 / 8 / +3 | 3/8/6/3, toughness 2 |
| Netherite | obsidian, ancient debris, netherite block | 2300 / 9 / +4 | 3/8/6/3, toughness 3, 10% KB resist |
| Mythic | unbreakable blocks (bedrock, command block...) | 4000 / 12 / +6 | 4/9/7/4, toughness 4, 20% KB resist |

Every item is tinted with the block's map colour, so a lapis pickaxe is blue and a netherrack helmet is red.
All of it is in the creative tab **Block Armory**.

### Uncrafting Table
Recipe: 2 cobblestone + 2 planks of any type, in any arrangement (shapeless).
Right-click it while holding an item: one craft's worth of the item is consumed and its ingredients are given back
(dropped if your inventory is full). Works for this mod's gear and for any vanilla/modded shaped or shapeless
crafting recipe. Damaged items return proportionally less. Sneak + right-click to place blocks against the table.
