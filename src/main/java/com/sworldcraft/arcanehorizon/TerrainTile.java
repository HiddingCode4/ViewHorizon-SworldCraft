package com.sworldcraft.arcanehorizon;

import java.util.Arrays;
import java.util.Map;

/** Immutable, full-column sampling. No independent boundary walls or LOD skirts. */
public final class TerrainTile {
    private final int chunkX,chunkZ,minHeight,maxHeight;
    private final int[] heights,colors;
    private final String[] materials;
    public TerrainTile(int chunkX,int chunkZ,int[] heights,String[] materials,int[] colors) {
        if(heights.length!=256||materials.length!=256||colors.length!=256)
            throw new IllegalArgumentException("256 columns required");
        this.chunkX=chunkX;this.chunkZ=chunkZ;
        this.heights=heights.clone();this.materials=materials.clone();this.colors=colors.clone();
        minHeight=Arrays.stream(this.heights).min().orElse(0);
        maxHeight=Arrays.stream(this.heights).max().orElse(0);
    }
    public int chunkX(){return chunkX;}
    public int chunkZ(){return chunkZ;}
    public int[] heights(){return heights.clone();}
    public String[] materials(){return materials.clone();}
    public int minHeight(){return minHeight;}
    public int maxHeight(){return maxHeight;}
    int height(int x,int z){return heights[z*16+x];}
    int color(int x,int z){return colors[z*16+x];}
    boolean present(int x,int z){
        String m=materials[z*16+x];
        return m!=null&&!m.endsWith(":air")&&!m.endsWith(":cave_air")&&!m.endsWith(":void_air");
    }
    public boolean sameSurface(int[] h,String[] m,int[] c){
        return Arrays.equals(heights,h)&&Arrays.equals(materials,m)&&Arrays.equals(colors,c);
    }
    // Compatibility for the previous planner source if it remains in an existing checkout.
    public TerrainMesh mesh(int step){return SurfaceMesher.build(this,Map.of(SurfaceMesher.key(chunkX,chunkZ),this),step);}
}
