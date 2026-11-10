package com.sworldcraft.arcanehorizon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    private static final ContextKey<List<DrawTile>> FRAME = new ContextKey<>(
        Identifier.fromNamespaceAndPath(ArcaneHorizon.ID,"terrain_frame"));

    private static final SurfaceCache MESHES=new SurfaceCache();
    private static Object levelIdentity;
    private ArcaneRenderer() {}

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        // Access live world data only during extraction. Drawing uses this immutable snapshot.
        List<DrawTile> visible = new ArrayList<>();
        if (ArcaneClient.renderEnabled) {
            Vec3 camera = event.getCamera().position();
            double maxDistance = Math.min(ArcaneClient.renderRange * 16.0,
                Math.max(0.0,event.getRenderState().cameraRenderState.depthFar - 32.0));
            double maxSquared = maxDistance * maxDistance;
            int cameraX=Math.floorDiv((int)Math.floor(camera.x),16);
            int cameraZ=Math.floorDiv((int)Math.floor(camera.z),16);
            Map<Long,TerrainTile> terrain=new HashMap<>();
            for(TerrainTile tile:ArcaneClient.snapshot(event.getLevel()))
                terrain.put(SurfaceMesher.key(tile.chunkX(),tile.chunkZ()),tile);
            if(levelIdentity!=event.getLevel()){MESHES.clear();levelIdentity=event.getLevel();}
            MESHES.retain(terrain);
            // Uniform global spacing prevents mixed-LOD cracks in this clean-surface mode.
            int step=terrain.size()*64>SOFT_VERTEX_BUDGET?8:4;
            int inner=Math.max(0,Minecraft.getInstance().options.getEffectiveRenderDistance()-ArcaneClient.overlap);
            for (TerrainTile tile : terrain.values()) {
                double dx = tile.chunkX()*16.0 + 8 - camera.x;
                double dz = tile.chunkZ()*16.0 + 8 - camera.z;
                if (dx*dx + dz*dz > maxSquared) continue;
                int chunkDistance=Math.max(Math.abs(tile.chunkX()-cameraX),Math.abs(tile.chunkZ()-cameraZ));
                boolean loaded=event.getLevel().getChunkSource().getChunk(
                    tile.chunkX(),tile.chunkZ(),ChunkStatus.FULL,false)!=null;
                // Keep a two-chunk fallback band around vanilla's visibility / fog edge.
                if(!ArcaneClient.preview && loaded && chunkDistance<=inner)continue;
                TerrainMesh mesh=MESHES.mesh(tile,terrain,step);
                if(mesh.vertexCount()==0)continue;
                AABB bounds=new AABB(tile.chunkX()*16.0,mesh.minY()-1,tile.chunkZ()*16.0,
                    tile.chunkX()*16.0+16,mesh.maxY()+1,tile.chunkZ()*16.0+16);
                if(ArcaneClient.culling&&!event.getFrustum().isVisible(bounds))continue;
                visible.add(new DrawTile(tile,mesh));
            }
        }
        ArcaneClient.visibleTiles = visible.size();
        ArcaneClient.renderedVertices=visible.stream().mapToInt(e -> e.mesh().vertexCount()).sum();
        event.getRenderState().setRenderData(FRAME,List.copyOf(visible));
    }

    @SubscribeEvent
    public static void submit(SubmitCustomGeometryEvent event) {
        List<DrawTile> tiles = event.getLevelRenderState().getRenderData(FRAME);
        if (tiles == null || tiles.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        var poses = event.getPoseStack();
        float brightness=ArcaneClient.brightness,saturation=ArcaneClient.saturation;
        // Immutable per-frame values; camera subtraction is done in double precision.
        event.getSubmitNodeCollector().submitCustomGeometry(poses, ArcaneRenderPipeline.TERRAIN, (pose,buffer) -> {
            for (DrawTile draw : tiles) {
                TerrainTile tile=draw.tile();
                TerrainMesh mesh = draw.mesh();
                float ox = (float)(tile.chunkX()*16.0-camera.x);
                float oz = (float)(tile.chunkZ()*16.0-camera.z);
                for (int v=0; v<mesh.vertexCount(); v++) {
                    buffer.addVertex(pose, ox+mesh.x(v), (float)(mesh.y(v)-camera.y), oz+mesh.z(v))
                        .setColor(RenderColor.adjust(mesh.color(v),brightness,saturation))
                        .setUv2(mesh.blockLight(v)<<4,mesh.skyLight(v)<<4);
                }
            }
        });
    }
    private record DrawTile(TerrainTile tile,TerrainMesh mesh){}
}
