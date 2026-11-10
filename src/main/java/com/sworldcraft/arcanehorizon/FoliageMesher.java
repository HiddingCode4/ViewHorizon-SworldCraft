package com.sworldcraft.arcanehorizon;

import java.util.ArrayList;

/** Coarse closed crowns derived only from observed leaf columns, not random trees.
 * Fixed 4-block world cells provide stable cluster positions when chunks load/unload.
 */
public final class FoliageMesher {
    private FoliageMesher() {}
    public static TerrainMesh build(TerrainTile tile) { return build(tile,4); }
    public static TerrainMesh build(TerrainTile tile,int cell) {
        if(cell!=4&&cell!=8)throw new IllegalArgumentException("Canopy cell must be 4 or 8");
        var b=new Builder();
        for(int z0=0;z0<16;z0+=cell)for(int x0=0;x0<16;x0+=cell) {
            int count=0,top=Integer.MIN_VALUE,bottom=Integer.MAX_VALUE,ground=0,conifers=0,snow=0;
            long red=0,green=0,blue=0;int light=0;
            float sx=0,sz=0;
            for(int z=z0;z<z0+cell;z++)for(int x=x0;x<x0+cell;x++) {
                CanopyColumn c=tile.canopy(x,z);if(c==null)continue;
                count++;sx+=x+0.5f;sz+=z+0.5f;ground+=tile.height(x,z)+1;
                top=Math.max(top,c.top());bottom=Math.min(bottom,c.bottom());
                red+=(c.color()>>>16)&255;green+=(c.color()>>>8)&255;blue+=c.color()&255;
                light=(Math.max(light>>>4,c.light()>>>4)<<4)|Math.max(light&15,c.light()&15);if(c.conifer())conifers++;if(c.snow())snow++;
            }
            if(count<2)continue;
            float x=sx/count,z=sz/count,base=ground/(float)count;
            float crownBase=Math.max(base+0.5f,Math.max(bottom,top-7));
            if(crownBase>=top)continue;
            float radius=Math.min(cell*0.58f,0.65f+(float)Math.sqrt(count)*0.4f);
            int color=0xff000000|((int)(red/count)<<16)|((int)(green/count)<<8)|(int)(blue/count);
            // A short supporting stem avoids detached crowns. This is a cluster
            // approximation; it does not assert that a trunk exists at this exact center.
            b.box(x-0.22f,base,z-0.22f,x+0.22f,crownBase+0.5f,z+0.22f,0xff69523a,light);
            if(conifers*2>=count) {
                float height=top-crownBase;
                b.frustum(x,z,crownBase,crownBase+height*0.5f,radius,radius*0.7f,color,light);
                b.frustum(x,z,crownBase+height*0.45f,top,radius*0.78f,0.12f,color,light);
            } else {
                float mid=crownBase+(top-crownBase)*0.55f;
                b.frustum(x,z,crownBase,mid,radius*0.65f,radius,color,light);
                b.frustum(x,z,mid,top,radius,radius*0.55f,color,light);
            }
            if(snow*2>=count)b.box(x-radius*0.5f,top,z-radius*0.5f,
                x+radius*0.5f,top+0.15f,z+radius*0.5f,0xffe5e9ed,light);
        }
        return b.finish();
    }
    private static final class Builder {
        final ArrayList<Float> xyz=new ArrayList<>();final ArrayList<Integer> colors=new ArrayList<>(),lights=new ArrayList<>();
        void quad(int color,int light,float... points){for(float v:points)xyz.add(v);for(int i=0;i<4;i++){colors.add(color);lights.add(light);}}
        void box(float x0,float y0,float z0,float x1,float y1,float z1,int c,int l){
            quad(c,l,x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0);
            quad(c,l,x0,y0,z1,x0,y0,z0,x1,y0,z0,x1,y0,z1);
            quad(c,l,x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0);
            quad(c,l,x1,y0,z1,x1,y1,z1,x0,y1,z1,x0,y0,z1);
            quad(c,l,x0,y0,z1,x0,y1,z1,x0,y1,z0,x0,y0,z0);
            quad(c,l,x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1);
        }
        void frustum(float x,float z,float y0,float y1,float r0,float r1,int color,int light){
            // Eight sides: a readable silhouette without textured alpha geometry.
            for(int side=0;side<8;side++) {
                double a=side*Math.PI/4,b=(side+1)*Math.PI/4;
                float ax=(float)Math.cos(a),az=(float)Math.sin(a),bx=(float)Math.cos(b),bz=(float)Math.sin(b);
                quad(RenderColor.adjust(color,0.72f+0.2f*(side%3)/2,1),light,
                    x+ax*r0,y0,z+az*r0,x+ax*r1,y1,z+az*r1,
                    x+bx*r1,y1,z+bz*r1,x+bx*r0,y0,z+bz*r0);
            }
            // Caps built from pairs of rim segments; duplicated center produces a valid triangle in a quad.
            for(int side=0;side<8;side++) {
                double a=side*Math.PI/4,b=(side+1)*Math.PI/4;
                quad(color,light,x,y1,z,x+(float)Math.cos(a)*r1,y1,z+(float)Math.sin(a)*r1,
                    x+(float)Math.cos(b)*r1,y1,z+(float)Math.sin(b)*r1,x,y1,z);
                quad(color,light,x,y0,z,x+(float)Math.cos(b)*r0,y0,z+(float)Math.sin(b)*r0,
                    x+(float)Math.cos(a)*r0,y0,z+(float)Math.sin(a)*r0,x,y0,z);
            }
        }
        TerrainMesh finish(){
            float[] p=new float[xyz.size()];int[] c=new int[colors.size()],l=new int[lights.size()];
            for(int i=0;i<p.length;i++)p[i]=xyz.get(i);
            for(int i=0;i<c.length;i++){c[i]=colors.get(i);l[i]=lights.get(i);}
            return new TerrainMesh(p,c,l);
        }
    }
}
