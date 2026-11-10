package com.sworldcraft.arcanehorizon;

import java.util.Arrays;

/** CPU-only opaque surface mesh. Coordinates are local to the chunk, Y is world height.
 * No Minecraft or GPU objects are held here, so snapshots can cross render phases safely.
 */
public final class TerrainMesh {
    private final float[] positions;
    private final int[] colors;

    private TerrainMesh(float[] positions, int[] colors) {
        this.positions = positions;
        this.colors = colors;
    }

    public int vertexCount() { return colors.length; }
    public float x(int vertex) { return positions[vertex * 3]; }
    public float y(int vertex) { return positions[vertex * 3 + 1]; }
    public float z(int vertex) { return positions[vertex * 3 + 2]; }
    public int color(int vertex) { return colors[vertex]; }

    public static TerrainMesh build(int[] heights, String[] materials, int[] colors, int step) {
        if (heights.length != 256 || materials.length != 256 || colors.length != 256)
            throw new IllegalArgumentException("256 surface columns required");
        if (step != 2 && step != 4 && step != 8) throw new IllegalArgumentException("LOD step must be 2, 4 or 8");
        int width = 16 / step;
        int[] reduced = new int[width*width];
        int[] reducedColors = new int[width*width];
        String[] reducedMaterials = new String[width*width];
        for(int z=0;z<width;z++) for(int x=0;x<width;x++) {
            int best=Integer.MIN_VALUE, bestIndex=z*step*16+x*step;
            long r=0,g=0,b=0;int count=0;
            for(int dz=0;dz<step;dz++) for(int dx=0;dx<step;dx++) {
                int i=(z*step+dz)*16+x*step+dx;
                if(isAir(materials[i]))continue;
                if(heights[i]>best){best=heights[i];bestIndex=i;}
                r+=(colors[i]>>>16)&255;g+=(colors[i]>>>8)&255;b+=colors[i]&255;count++;
            }
            int i=z*width+x;
            reduced[i]=count==0?0:best;
            reducedMaterials[i]=count==0?"minecraft:air":materials[bestIndex];
            reducedColors[i]=count==0?0:0xff000000 | ((int)(r/count)<<16) | ((int)(g/count)<<8) | (int)(b/count);
        }
        heights=reduced; materials=reducedMaterials; colors=reducedColors;
        int floor=Arrays.stream(heights).min().orElse(0)-16;
        Builder out = new Builder(width*width*5*4);
        for (int z = 0; z < width; z++) for (int x = 0; x < width; x++) {
            int index = z * width + x;
            if (isAir(materials[index])) continue;
            float x0 = x * step, x1 = x0 + step, z0 = z * step, z1 = z0 + step;
            float top = heights[index] + 1;
            int color = colors[index];
            out.quad(color, x0,top,z0, x0,top,z1, x1,top,z1, x1,top,z0);
            // Short outer skirts conceal tile edges; internal walls stop at the adjacent surface.
            float west = neighborHeight(heights, materials, x-1,z,top,width,floor);
            float east = neighborHeight(heights, materials, x+1,z,top,width,floor);
            float north = neighborHeight(heights, materials, x,z-1,top,width,floor);
            float south = neighborHeight(heights, materials, x,z+1,top,width,floor);
            if (west < top) out.quad(shade(color,0.72f), x0,west,z0, x0,west,z1, x0,top,z1, x0,top,z0);
            if (east < top) out.quad(shade(color,0.85f), x1,east,z1, x1,east,z0, x1,top,z0, x1,top,z1);
            if (north < top) out.quad(shade(color,0.65f), x1,north,z0, x0,north,z0, x0,top,z0, x1,top,z0);
            if (south < top) out.quad(shade(color,0.8f), x0,south,z1, x1,south,z1, x1,top,z1, x0,top,z1);
        }
        return out.finish();
    }

    private static boolean isAir(String name) {
        return name == null || name.endsWith(":air") || name.endsWith(":cave_air") || name.endsWith(":void_air");
    }

    private static float neighborHeight(int[] heights, String[] materials, int x, int z,
                                        float top, int width, int floor) {
        if (x < 0 || x >= width || z < 0 || z >= width || isAir(materials[z*width+x])) return floor;
        return heights[z*width+x] + 1;
    }

    private static int shade(int color, float scale) {
        int r = (int)(((color >>> 16) & 255) * scale);
        int g = (int)(((color >>> 8) & 255) * scale);
        int b = (int)((color & 255) * scale);
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    private static final class Builder {
        private final float[] vertices;
        private final int[] colors;
        Builder(int capacity) { vertices=new float[capacity*3]; colors=new int[capacity]; }
        private int count;
        void quad(int color, float... coords) {
            System.arraycopy(coords, 0, vertices, count * 3, 12);
            Arrays.fill(colors, count, count + 4, color);
            count += 4;
        }
        TerrainMesh finish() {
            return new TerrainMesh(Arrays.copyOf(vertices,count*3), Arrays.copyOf(colors,count));
        }
    }
}
