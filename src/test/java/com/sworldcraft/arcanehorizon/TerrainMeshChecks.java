package com.sworldcraft.arcanehorizon;

import java.util.Arrays;

public final class TerrainMeshChecks {
    public static void main(String[] args) {
        int[] heights=new int[256],colors=new int[256];String[] materials=new String[256];
        Arrays.fill(heights,64);Arrays.fill(colors,0xff448866);Arrays.fill(materials,"minecraft:grass_block");
        for(int step:new int[]{2,4,8}) {
            TerrainMesh flat=TerrainMesh.build(heights,materials,colors,step);
            int width=16/step, tops=0;float area=0;
            for(int v=0;v<flat.vertexCount();v+=4) {
                boolean top=true;
                for(int i=0;i<4;i++) {
                    if(flat.y(v+i)!=65)top=false;
                    check(flat.x(v+i)>=0&&flat.x(v+i)<=16,"X out of tile");
                    check(flat.z(v+i)>=0&&flat.z(v+i)<=16,"Z out of tile");
                    check((flat.color(v+i)>>>24)==255,"Non-opaque surface");
                }
                if(top){tops++;area+=step*step;check(flat.color(v)==0xff448866,"Captured color lost");}
            }
            check(tops==width*width,"Missing top cells");
            check(area==256,"LOD does not cover full chunk footprint");
            check(flat.vertexCount()==(width*width+4*width)*4,"Unexpected internal walls on flat terrain");
        }
        heights[6*16+6]=80;
        TerrainMesh peak=TerrainMesh.build(heights,materials,colors,2);
        check(peak.vertexCount()==(64+32+4)*4,"Missing four peak walls");
        TerrainTile tile=new TerrainTile(-2,3,heights,materials,colors);
        heights[6*16+6]=0;materials[6*16+6]="minecraft:air";colors[0]=0;
        check(tile.maxHeight()==80,"Input mutation affected tile");
        int[] copy=tile.heights();copy[6*16+6]=0;check(tile.maxHeight()==80,"Accessor mutation affected tile");
        Arrays.fill(materials,"minecraft:air");
        check(TerrainMesh.build(heights,materials,colors,2).vertexCount()==0,"Air generated geometry");
        int desaturated=RenderColor.adjust(0xff20e050,1,0);
        check(((desaturated>>>16)&255)==((desaturated>>>8)&255)
            && ((desaturated>>>8)&255)==(desaturated&255),"Saturation zero must produce gray");
        check(RenderColor.adjust(0xffffffff,1,0.4f)==0xffffffff,"Neutral white became colored");
        check((RenderColor.surface(0xff619961,"minecraft:spruce_leaves")&0x00ffffff)<0x619961,
            "Foliage texture modulation missing");
        var entries=new java.util.ArrayList<RenderPlanner.Entry>();
        for(int i=0;i<600;i++)entries.add(new RenderPlanner.Entry(tile,2,i));
        var plan=RenderPlanner.fit(entries,100000);
        check(plan.stream().mapToInt(e -> e.mesh().vertexCount()).sum()<=100000,"Feasible budget was exceeded");
        check(plan.size()==600,"Budget removed terrain tiles");
        for(int i=0;i<plan.size();i++)check(plan.get(i).distanceSquared()==i,"Tile order / identity changed");
        var impossible=RenderPlanner.fit(entries,1);
        check(impossible.size()==600,"Impossible budget created coverage gaps");
        check(impossible.stream().allMatch(e -> e.step()==8),"Coarsest fallback not selected");
        System.out.println("Terrain checks passed: 3 LODs, complete footprint, walls, colors, bounds, opacity, air, immutability, color controls, coverage-preserving budget.");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
