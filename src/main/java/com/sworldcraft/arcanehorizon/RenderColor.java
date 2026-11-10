package com.sworldcraft.arcanehorizon;

/** Original color calibration, independent of any other mod's implementation. */
public final class RenderColor {
    private RenderColor() {}
    public static int surface(int rgb,String material) {
        // A raw biome tint is much brighter than a tint multiplied by a leaf texture.
        // These factors approximate that missing texture modulation; not texture sampling.
        float factor=material.contains("leaves")?0.55f:
            material.equals("minecraft:grass_block")?0.78f:1.0f;
        return adjust(rgb,factor,1.0f);
    }
    public static int adjust(int rgb,float brightness,float saturation) {
        float r=(rgb>>>16)&255,g=(rgb>>>8)&255,b=rgb&255;
        float gray=0.2126f*r+0.7152f*g+0.0722f*b;
        int rr=clamp((gray+(r-gray)*saturation)*brightness);
        int gg=clamp((gray+(g-gray)*saturation)*brightness);
        int bb=clamp((gray+(b-gray)*saturation)*brightness);
        return 0xff000000|rr<<16|gg<<8|bb;
    }
    private static int clamp(float value){return Math.max(0,Math.min(255,Math.round(value)));}
}
