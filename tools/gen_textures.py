"""Generates every PNG used by the mod (run from the project root: python3 tools/gen_textures.py).
Tool heads and armor are drawn in greyscale on purpose: the game tints them per block."""
import os, random
from PIL import Image

RES = "src/main/resources/assets/blockarmory/textures"
os.makedirs(f"{RES}/item", exist_ok=True)

def save(img, rel):
    path = f"{RES}/{rel}"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)

def grey_shape(rows, size=16, edge_grey=95):
    """rows: list of strings, 'x' = filled. Returns RGBA image with outline + simple shading."""
    px = set()
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == 'x':
                px.add((x, y))
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for (x, y) in px:
        edge = any((x+dx, y+dy) not in px for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)))
        if edge:
            g = edge_grey
        else:
            g = int(235 - (x + y) * 3.2)       # lighter top-left, darker bottom-right
        img.putpixel((x, y), (g, g, g, 255))
    return img

def handle(points):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y) in points:
        img.putpixel((x, y), (112, 76, 38, 255))
        if x + 1 < 16 and img.getpixel((x+1, y))[3] == 0:
            img.putpixel((x+1, y), (74, 48, 22, 255))
    return img

diag = lambda x0, y0, n: [(x0 + i, y0 - i) for i in range(n)]

# ---------------------------------------------------------------- tools
HEADS = {
 "pickaxe": [
  "................",
  "........xxxxxx..",
  ".....xxxxxxxxxx.",
  "....xxx.....xxxx",
  "...xxx........xx",
  "...xx.........xx",
  "..............xx",
  "...............x",
 ],
 "axe": [
  "..........xxx...",
  ".........xxxxx..",
  "........xxxxxxx.",
  ".......xxxxxxxxx",
  "........xxxxxxx.",
  "........xxxxx...",
  ".........xxx....",
 ],
 "shovel": [
  "............xxx.",
  "...........xxxxx",
  "..........xxxxxx",
  "..........xxxxx.",
  "...........xxx..",
 ],
 "hoe": [
  "................",
  "........xxxxx...",
  ".......xxxxxxx..",
  ".......xxx..xxx.",
  ".......xx....xx.",
  ".............xx.",
 ],
}
for name, rows in HEADS.items():
    save(grey_shape(rows), f"item/{name}_head.png")
    save(handle(diag(2, 14, 11)), f"item/{name}_handle.png")

# sword: blade is the tinted part, grip + guard are the handle layer
sw = [[" "] * 16 for _ in range(16)]
for i in range(9):
    for dy in (0, 1, 2):
        y = 10 - i - 0 + dy - 2
        if 0 <= y < 16:
            sw[y][5 + i] = 'x'
for (x, y) in ((13, 0), (14, 0), (14, 1), (15, 0)):
    sw[y][x] = 'x'
save(grey_shape(["".join(r).replace(" ", ".") for r in sw], edge_grey=140), "item/sword_head.png")
grip = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for (x, y) in [(2, 13), (3, 12), (4, 11)]:
    grip.putpixel((x, y), (112, 76, 38, 255)); grip.putpixel((x+1, y), (74, 48, 22, 255))
for (x, y) in [(3, 10), (4, 10), (5, 11), (6, 12), (5, 10), (4, 9)]:
    grip.putpixel((x, y), (120, 120, 130, 255))
save(grip, "item/sword_handle.png")

# ---------------------------------------------------------------- armor icons
ARMOR = {
 "helmet": [
  "................",
  "................",
  "....xxxxxxxx....",
  "...xxxxxxxxxx...",
  "..xxxxxxxxxxxx..",
  "..xxxxxxxxxxxx..",
  "..xxxxxxxxxxxx..",
  "..xxxx....xxxx..",
  "..xxx......xxx..",
  "..xxx......xxx..",
 ],
 "chestplate": [
  "................",
  "..xxxx....xxxx..",
  ".xxxxxx..xxxxxx.",
  ".xxxxxxxxxxxxxx.",
  ".xxxxxxxxxxxxxx.",
  "..xxxxxxxxxxxx..",
  "...xxxxxxxxxx...",
  "...xxxxxxxxxx...",
  "...xxxxxxxxxx...",
  "...xxxxxxxxxx...",
  "...xxxxxxxxxx...",
 ],
 "leggings": [
  "................",
  "...xxxxxxxxxx...",
  "...xxxxxxxxxx...",
  "...xxxxxxxxxx...",
  "...xxxx..xxxx...",
  "...xxxx..xxxx...",
  "...xxxx..xxxx...",
  "...xxxx..xxxx...",
  "...xxxx..xxxx...",
  "...xxxx..xxxx...",
 ],
 "boots": [
  "................",
  "................",
  "................",
  "..xxxx....xxxx..",
  "..xxxx....xxxx..",
  "..xxxx....xxxx..",
  "..xxxx....xxxx..",
  ".xxxxx....xxxxx.",
  ".xxxxxx..xxxxxx.",
  ".xxxxxx..xxxxxx.",
 ],
}
for name, rows in ARMOR.items():
    save(grey_shape(rows), f"item/{name}.png")

# ---------------------------------------------------------------- worn armor layers (64x32, light grey so dye shows)
random.seed(7)
def armor_layer():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(64):
            g = 214 + random.randint(-14, 10)
            if x % 4 == 0 or y % 4 == 0:
                g -= 26
            img.putpixel((x, y), (g, g, g, 255))
    return img
save(armor_layer(), "entity/equipment/humanoid/block.png")
save(armor_layer(), "entity/equipment/humanoid_leggings/block.png")

# ---------------------------------------------------------------- uncrafting table textures
random.seed(11)
def planks(w=16, h=16, base=(166, 126, 72)):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    for y in range(h):
        for x in range(w):
            n = random.randint(-9, 9)
            c = tuple(max(0, min(255, v + n)) for v in base)
            if y % 4 == 3:
                c = tuple(int(v * 0.72) for v in c)
            img.putpixel((x, y), c + (255,))
    return img

def cobble(w=16, h=16):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    for y in range(h):
        for x in range(w):
            g = 122 + random.randint(-18, 18)
            if (x + (y // 4) * 3) % 6 == 0 or y % 4 == 0:
                g -= 36
            img.putpixel((x, y), (g, g, g, 255))
    return img

top = planks()
for i in range(16):                      # dark cross + frame, like a crafting grid
    for (x, y) in ((i, 0), (i, 15), (0, i), (15, i), (i, 7), (i, 8), (7, i), (8, i)):
        top.putpixel((x, y), (84, 58, 30, 255))
for (x0, y0) in ((2, 2), (10, 2), (2, 10), (10, 10)):
    for dx in range(4):
        for dy in range(4):
            top.putpixel((x0 + dx, y0 + dy), (120, 120, 120, 255) if (dx + dy) % 2 == 0 else (136, 136, 136, 255))
save(top, "block/uncrafting_table_top.png")
save(planks(), "block/uncrafting_table_bottom.png")

side = cobble()
for y in range(0, 5):                    # wooden band along the top of the sides
    for x in range(16):
        side.putpixel((x, y), planks().getpixel((x, y)))
for y in range(5, 16):                   # a couple of tool silhouettes
    pass
for (x, y) in [(3,7),(4,8),(5,9),(6,10),(7,11)] + [(11,7),(10,8),(9,9),(8,10)]:
    side.putpixel((x, y), (72, 48, 24, 255))
save(side, "block/uncrafting_table_side.png")
print("textures written")
