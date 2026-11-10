package com.sworldcraft.arcanehorizon;

import java.util.ArrayList;
import java.util.List;

/** Reduce detail to meet a soft vertex budget, without removing terrain tiles.
 * Input must be ordered from nearest to farthest. Coverage wins if even the
 * coarsest available representation exceeds the budget.
 */
public final class RenderPlanner {
    private RenderPlanner() {}
    public record Entry(TerrainTile tile,int step,double distanceSquared) {
        public TerrainMesh mesh(){return tile.mesh(step);}
    }
    public static List<Entry> fit(List<Entry> ordered,int vertexBudget) {
        var result=new ArrayList<>(ordered);
        long count=0;
        for(var e:result)count+=e.mesh().vertexCount();
        for(int pass=0;pass<2 && count>vertexBudget;pass++) {
            for(int i=result.size()-1;i>=0 && count>vertexBudget;i--) {
                Entry old=result.get(i);
                if(old.step()>=8)continue;
                Entry reduced=new Entry(old.tile(),old.step()*2,old.distanceSquared());
                count+=reduced.mesh().vertexCount()-old.mesh().vertexCount();
                result.set(i,reduced);
            }
        }
        return List.copyOf(result);
    }
}
