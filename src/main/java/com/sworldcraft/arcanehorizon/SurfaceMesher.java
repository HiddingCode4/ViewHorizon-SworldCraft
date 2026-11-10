package com.sworldcraft.arcanehorizon;

import java.util.Arrays;
import java.util.Map;

/** Shared world-space corner sampling: adjacent known tiles compute exactly the
 * same border heights. Geometry contains only surface quads, never vertical skirts.
 */
public final class SurfaceMesher {
    private SurfaceMesher() {}
    public static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    public static TerrainMesh build(TerrainTile tile,Map<Long,TerrainTile> terrain,int step) {
        if(step!=2&&step!=4&&step!=8)throw new IllegalArgumentException("Invalid grid step");
        int width=16/step;
        float[] xyz=new float[width*width*4*3];int[] colors=new int[width*width*4];int vertices=0;
        int wx=tile.chunkX()*16,wz=tile.chunkZ()*16;
        // Cached within this mesh: each shared corner is calculated once.
        float[][] corners=new float[width+1][width+1];
        for(int z=0;z<=width;z++)for(int x=0;x<=width;x++)
            corners[z][x]=corner(terrain,wx+x*step,wz+z*step,step);
        for(int z=0;z<width;z++)for(int x=0;x<width;x++) {
            long r=0,g=0,b=0;int count=0;
            for(int dz=0;dz<step;dz++)for(int dx=0;dx<step;dx++) {
                int lx=x*step+dx,lz=z*step+dz;
                if(!tile.present(lx,lz))continue;
                int color=tile.color(lx,lz);r+=(color>>>16)&255;g+=(color>>>8)&255;b+=color&255;count++;
            }
            if(count==0)continue;
            float h00=corners[z][x],h01=corners[z+1][x],h11=corners[z+1][x+1],h10=corners[z][x+1];
            if(!Float.isFinite(h00)||!Float.isFinite(h01)||!Float.isFinite(h11)||!Float.isFinite(h10))continue;
            int color=0xff000000|((int)(r/count)<<16)|((int)(g/count)<<8)|(int)(b/count);
            int at=vertices*3;
            float[] quad={x*step,h00,z*step, x*step,h01,(z+1)*step,
                (x+1)*step,h11,(z+1)*step, (x+1)*step,h10,z*step};
            System.arraycopy(quad,0,xyz,at,12);Arrays.fill(colors,vertices,vertices+4,color);vertices+=4;
        }
        return new TerrainMesh(Arrays.copyOf(xyz,vertices*3),Arrays.copyOf(colors,vertices));
    }
    private static float corner(Map<Long,TerrainTile> terrain,int wx,int wz,int step) {
        long total=0;int count=0;
        // Symmetric, global footprint. Both sides of a border include the same
        // columns, including at negative world coordinates and four-chunk junctions.
        int half=step/2;
        for(int dz=-half;dz<half;dz++)for(int dx=-half;dx<half;dx++) {
            int x=wx+dx,z=wz+dz;
            TerrainTile t=terrain.get(key(Math.floorDiv(x,16),Math.floorDiv(z,16)));
            int lx=Math.floorMod(x,16),lz=Math.floorMod(z,16);
            if(t!=null&&t.present(lx,lz)){total+=t.height(lx,lz)+1;count++;}
        }
        return count==0?Float.NaN:(float)total/count;
    }
}
