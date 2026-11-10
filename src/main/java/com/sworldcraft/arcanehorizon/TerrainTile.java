package com.sworldcraft.arcanehorizon;

/** Original surface representation: 16 columns per chunk, each covering 4x4 blocks.
 * This is a heightfield prototype: overhangs and caves are intentionally not represented.
 */
public record TerrainTile(int chunkX, int chunkZ, int[] heights, String[] materials) {
    public TerrainTile {
        if (heights.length != 16 || materials.length != 16)
            throw new IllegalArgumentException("A tile requires 16 surface cells");
        heights = heights.clone();
        materials = materials.clone();
    }
    @Override public int[] heights() { return heights.clone(); }
    @Override public String[] materials() { return materials.clone(); }
    public int minHeight() { return java.util.Arrays.stream(heights).min().orElse(0); }
    public int maxHeight() { return java.util.Arrays.stream(heights).max().orElse(0); }
}
