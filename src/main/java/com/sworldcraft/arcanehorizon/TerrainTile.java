package com.sworldcraft.arcanehorizon;

import java.util.Arrays;

/** Immutable block-resolution surface, with three prebuilt levels of detail. */
public final class TerrainTile {
    private final int chunkX, chunkZ, minHeight, maxHeight;
    private final int[] heights, colors;
    private final String[] materials;
    private final TerrainMesh fine, medium, coarse;
    public TerrainTile(int chunkX,int chunkZ,int[] heights,String[] materials,int[] colors) {
        if(heights.length!=256 || materials.length!=256 || colors.length!=256)
            throw new IllegalArgumentException("256 columns required");
        this.chunkX=chunkX; this.chunkZ=chunkZ;
        this.heights=heights.clone();this.materials=materials.clone();this.colors=colors.clone();
        minHeight=Arrays.stream(this.heights).min().orElse(0);
        maxHeight=Arrays.stream(this.heights).max().orElse(0);
        fine=TerrainMesh.build(this.heights,this.materials,this.colors,2);
        medium=TerrainMesh.build(this.heights,this.materials,this.colors,4);
        coarse=TerrainMesh.build(this.heights,this.materials,this.colors,8);
    }
    public int chunkX(){return chunkX;}
    public int chunkZ(){return chunkZ;}
    public int[] heights(){return heights.clone();}
    public String[] materials(){return materials.clone();}
    public int minHeight(){return minHeight;}
    public int maxHeight(){return maxHeight;}
    public TerrainMesh mesh(int step){return step==2?fine:step==4?medium:coarse;}
    public boolean sameSurface(int[] h,String[] m,int[] c){
        return Arrays.equals(heights,h)&&Arrays.equals(materials,m)&&Arrays.equals(colors,c);
    }
}
