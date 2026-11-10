package com.sworldcraft.arcanehorizon;

/** Immutable surface geometry. Vertices use chunk-local X/Z and absolute world Y. */
public final class TerrainMesh {
    private final float[] positions;
    private final int[] colors;
    private final float minY,maxY;
    TerrainMesh(float[] positions,int[] colors) {
        if(positions.length!=colors.length*3 || colors.length%4!=0)
            throw new IllegalArgumentException("Invalid quad buffers");
        this.positions=positions;this.colors=colors;
        float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY;
        for(int i=1;i<positions.length;i+=3){lo=Math.min(lo,positions[i]);hi=Math.max(hi,positions[i]);}
        minY=colors.length==0?0:lo;maxY=colors.length==0?0:hi;
    }
    public int vertexCount(){return colors.length;}
    public float x(int v){return positions[v*3];}
    public float y(int v){return positions[v*3+1];}
    public float z(int v){return positions[v*3+2];}
    public int color(int v){return colors[v];}
    public float minY(){return minY;}
    public float maxY(){return maxY;}
}
