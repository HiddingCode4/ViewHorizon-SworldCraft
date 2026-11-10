package com.sworldcraft.arcanehorizon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(modid = ArcaneHorizon.ID, value = Dist.CLIENT)
public final class ArcaneRenderer {
    private static final int SOFT_VERTEX_BUDGET = 240_000;
    private static final ContextKey<List<RenderPlanner.Entry>> FRAME = new ContextKey<>(
        Identifier.fromNamespaceAndPath(ArcaneHorizon.ID,"terrain_frame"));

    private ArcaneRenderer() {}

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        // Access live world data only during extraction. Drawing uses this immutable snapshot.
        List<RenderPlanner.Entry> visible = new ArrayList<>();
        if (ArcaneClient.renderEnabled) {
            Vec3 camera = event.getCamera().position();
            double maxDistance = Math.min(ArcaneClient.renderRange * 16.0,
                Math.max(0.0,event.getRenderState().cameraRenderState.depthFar - 32.0));
            double maxSquared = maxDistance * maxDistance;
            int cameraX=Math.floorDiv((int)Math.floor(camera.x),16);
            int cameraZ=Math.floorDiv((int)Math.floor(camera.z),16);
            int inner=Math.max(0,Minecraft.getInstance().options.getEffectiveRenderDistance()-ArcaneClient.overlap);
            for (TerrainTile tile : ArcaneClient.snapshot(event.getLevel())) {
                double dx = tile.chunkX()*16.0 + 8 - camera.x;
                double dz = tile.chunkZ()*16.0 + 8 - camera.z;
                if (dx*dx + dz*dz > maxSquared) continue;
                int chunkDistance=Math.max(Math.abs(tile.chunkX()-cameraX),Math.abs(tile.chunkZ()-cameraZ));
                boolean loaded=event.getLevel().getChunkSource().getChunk(
                    tile.chunkX(),tile.chunkZ(),ChunkStatus.FULL,false)!=null;
                // Keep a two-chunk fallback band around vanilla's visibility / fog edge.
                if(!ArcaneClient.preview && loaded && chunkDistance<=inner)continue;
                AABB bounds=new AABB(tile.chunkX()*16.0,tile.minHeight()-16,tile.chunkZ()*16.0,
                    tile.chunkX()*16.0+16,tile.maxHeight()+1,tile.chunkZ()*16.0+16);
                if(ArcaneClient.culling && !event.getFrustum().isVisible(bounds))continue;
                int step=chunkDistance<=Math.max(10,inner+2)?2:chunkDistance<=20?4:8;
                if(tile.mesh(step).vertexCount()!=0)visible.add(new RenderPlanner.Entry(tile,step,dx*dx+dz*dz));
            }
            visible.sort(Comparator.comparingDouble(RenderPlanner.Entry::distanceSquared));
            // Preserve all candidate tiles: coarsen geometry instead of cutting the list.
            visible = RenderPlanner.fit(visible,SOFT_VERTEX_BUDGET);
        }
        ArcaneClient.visibleTiles = visible.size();
        ArcaneClient.renderedVertices=visible.stream().mapToInt(e -> e.mesh().vertexCount()).sum();
        event.getRenderState().setRenderData(FRAME,List.copyOf(visible));
    }

    @SubscribeEvent
    public static void submit(SubmitCustomGeometryEvent event) {
        List<RenderPlanner.Entry> tiles = event.getLevelRenderState().getRenderData(FRAME);
        if (tiles == null || tiles.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        var poses = event.getPoseStack();
        float brightness=ArcaneClient.brightness,saturation=ArcaneClient.saturation;
        // Immutable per-frame values; camera subtraction is done in double precision.
        event.getSubmitNodeCollector().submitCustomGeometry(poses, ArcaneRenderPipeline.TERRAIN, (pose,buffer) -> {
            for (RenderPlanner.Entry draw : tiles) {
                TerrainTile tile=draw.tile();
                TerrainMesh mesh = draw.mesh();
                float ox = (float)(tile.chunkX()*16.0-camera.x);
                float oz = (float)(tile.chunkZ()*16.0-camera.z);
                for (int v=0; v<mesh.vertexCount(); v++) {
                    buffer.addVertex(pose, ox+mesh.x(v), (float)(mesh.y(v)-camera.y), oz+mesh.z(v))
                        .setColor(RenderColor.adjust(mesh.color(v),brightness,saturation));
                }
            }
        });
    }
}
