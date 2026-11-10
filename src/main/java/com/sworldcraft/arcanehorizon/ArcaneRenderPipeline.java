package com.sworldcraft.arcanehorizon;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** Opaque color-only geometry: depth test and writes enabled, no vanilla distance fog.
 * Uses the game's standard position/color shader, without redistributing its source.
 */
@EventBusSubscriber(modid = ArcaneHorizon.ID, value = Dist.CLIENT)
public final class ArcaneRenderPipeline {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath(ArcaneHorizon.ID,"pipeline/terrain"))
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withVertexShader("core/position_color")
        .withFragmentShader("core/position_color")
        .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState.DEFAULT)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withCull(false)
        .build();
    static final RenderType TERRAIN = RenderType.create("arcanehorizon_terrain",
        RenderSetup.builder(PIPELINE).createRenderSetup());

    private ArcaneRenderPipeline() {}
    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }
}
