package com.sworldcraft.arcanehorizon;

import java.util.HashMap;
import java.util.Map;

/** Rebuild a mesh when its own or any neighboring tile changes. */
public final class SurfaceCache {
    private final Map<Long,Cached> cached=new HashMap<>();
    public void clear(){cached.clear();}
    public void retain(Map<Long,TerrainTile> terrain){cached.keySet().retainAll(terrain.keySet());}
    public TerrainMesh mesh(TerrainTile tile,Map<Long,TerrainTile> terrain,int step) {
        long key=SurfaceMesher.key(tile.chunkX(),tile.chunkZ());
        TerrainTile[] neighbors=new TerrainTile[9];int i=0;
        for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++)
            neighbors[i++]=terrain.get(SurfaceMesher.key(tile.chunkX()+x,tile.chunkZ()+z));
        Cached old=cached.get(key);
        if(old!=null&&old.step==step&&java.util.Arrays.equals(old.neighbors,neighbors))return old.mesh;
        TerrainMesh mesh=SurfaceMesher.build(tile,terrain,step);
        cached.put(key,new Cached(step,neighbors,mesh));return mesh;
    }
    private record Cached(int step,TerrainTile[] neighbors,TerrainMesh mesh){}
}
