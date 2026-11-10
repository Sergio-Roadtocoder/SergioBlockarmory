package com.blockarmory;

/** The nine pieces of gear that can be made from a block, and their crafting patterns. */
public enum GearKind {
    PICKAXE("pickaxe", false, "BBB", ".S.", ".S."),
    AXE("axe", false, "BB", "BS", ".S"),
    SHOVEL("shovel", false, "B", "S", "S"),
    HOE("hoe", false, "BB", ".S", ".S"),
    SWORD("sword", false, "B", "B", "S"),
    HELMET("helmet", true, "BBB", "B.B"),
    CHESTPLATE("chestplate", true, "B.B", "BBB", "BBB"),
    LEGGINGS("leggings", true, "BBB", "B.B", "B.B"),
    BOOTS("boots", true, "B.B", "B.B");

    private final String id;
    private final boolean armor;
    private final String[] pattern;
    private final int blockCount;
    private final int stickCount;

    GearKind(String id, boolean armor, String... pattern) {
        this.id = id;
        this.armor = armor;
        this.pattern = pattern;
        int blocks = 0;
        int sticks = 0;
        for (String row : pattern) {
            for (char c : row.toCharArray()) {
                if (c == 'B') {
                    blocks++;
                } else if (c == 'S') {
                    sticks++;
                }
            }
        }
        this.blockCount = blocks;
        this.stickCount = sticks;
    }

    public String id() {
        return id;
    }

    public boolean armor() {
        return armor;
    }

    public String[] pattern() {
        return pattern;
    }

    public int blockCount() {
        return blockCount;
    }

    public int stickCount() {
        return stickCount;
    }
}
