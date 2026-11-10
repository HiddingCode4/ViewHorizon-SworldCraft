package com.sworldcraft.arcanehorizon;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Seam and mesh regression checks for the continuous-surface renderer. */
public final class TerrainMeshChecks {
    public static void main(String[] args) {
        for(int step:new int[]{4,8})for(int base:new int[]{-2,0,8}) {
            Map<Long,TerrainTile> terrain=new HashMap<>();
            for(int z=base-1;z<=base+2;z++)for(int x=base-1;x<=base+2;x++) {
                TerrainTile tile=tile(x,z,64);terrain.put(SurfaceMesher.key(x,z),tile);
            }
            TerrainTile a=terrain.get(SurfaceMesher.key(base,base));
            TerrainTile east=terrain.get(SurfaceMesher.key(base+1,base));
            TerrainTile south=terrain.get(SurfaceMesher.key(base,base+1));
            TerrainMesh am=SurfaceMesher.build(a,terrain,step),em=SurfaceMesher.build(east,terrain,step),sm=SurfaceMesher.build(south,terrain,step);
            check(edge(am,true,16).equals(edge(em,true,0)),"East/west shared border cracked");
            check(edge(am,false,16).equals(edge(sm,false,0)),"North/south shared border cracked");
            check(am.vertexCount()==(16/step)*(16/step)*4,"Incomplete surface coverage");
            for(int v=0;v<am.vertexCount();v+=4) {
                float area=Math.abs((am.x(v+2)-am.x(v))*(am.z(v+2)-am.z(v)));
                check(area==step*step,"Degenerate or vertical artificial face");
                for(int j=0;j<4;j++)check(Float.isFinite(am.y(v+j)),"Invalid surface height");
            }
        }
        TerrainTile center=tile(0,0,64);
        Map<Long,TerrainTile> terrain=new HashMap<>();terrain.put(SurfaceMesher.key(0,0),center);
        SurfaceCache cache=new SurfaceCache();TerrainMesh before=cache.mesh(center,terrain,4);
        check(cache.mesh(center,terrain,4)==before,"Unchanged mesh rebuilt");
        terrain.put(SurfaceMesher.key(1,0),tile(1,0,120));
        TerrainMesh after=cache.mesh(center,terrain,4);
        check(before!=after,"New neighbor did not invalidate seam mesh");
        check(!edge(before,true,16).equals(edge(after,true,16)),"Neighbor heights ignored");
        int[] h=new int[256],c=new int[256];String[] m=new String[256];
        Arrays.fill(m,"minecraft:air");TerrainTile empty=new TerrainTile(5,5,h,m,c);
        check(SurfaceMesher.build(empty,Map.of(SurfaceMesher.key(5,5),empty),4).vertexCount()==0,"Air generated surfaces");
        check(RenderColor.adjust(0xffffffff,1,0.4f)==0xffffffff,"White color changed hue");
        int[] copy=center.heights();copy[0]=999;check(center.maxHeight()<999,"Tile is mutable");
        System.out.println("Surface checks passed: shared borders, negative coordinates, complete footprints, no vertical skirts, neighbor invalidation, cache reuse, air, colors, immutability.");
    }
    private static TerrainTile tile(int x,int z,int base) {
        int[] h=new int[256],c=new int[256];String[] m=new String[256];
        for(int iz=0;iz<16;iz++)for(int ix=0;ix<16;ix++) {
            int i=iz*16+ix;h[i]=base+Math.floorMod(x*16+ix+2*(z*16+iz),11);
            c[i]=0xff69804f;m[i]="minecraft:grass_block";
        }
        return new TerrainTile(x,z,h,m,c);
    }
    private static Map<Float,Float> edge(TerrainMesh mesh,boolean xAxis,float coordinate) {
        Map<Float,Float> values=new HashMap<>();
        for(int v=0;v<mesh.vertexCount();v++)if((xAxis?mesh.x(v):mesh.z(v))==coordinate) {
            float along=xAxis?mesh.z(v):mesh.x(v);Float previous=values.put(along,mesh.y(v));
            check(previous==null||previous==mesh.y(v),"Inconsistent vertex within mesh");
        }
        return values;
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
