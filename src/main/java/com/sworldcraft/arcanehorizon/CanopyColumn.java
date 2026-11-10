package com.sworldcraft.arcanehorizon;

/** A canopy observed in a real loaded column; heights are absolute world coordinates. */
public record CanopyColumn(int top,int bottom,int color,int light,boolean conifer,boolean snow) {
    public CanopyColumn {
        if(bottom>=top)throw new IllegalArgumentException("Canopy bottom must be below its top");
    }
}
