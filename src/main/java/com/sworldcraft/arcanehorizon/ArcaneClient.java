package com.sworldcraft.arcanehorizon;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = ArcaneHorizon.ID, value = Dist.CLIENT)
public final class ArcaneClient {
    private static final int LIMIT = 8192;
    private static final int RADIUS = 8;
    private static final int WIDTH = RADIUS * 2 + 1;
    private static final Map<Long, TerrainTile> TILES = new LinkedHashMap<>();
    private static ClientLevel currentLevel;
    private static int cursor;
    private static boolean enabled = true;
    private static long samples;

    private ArcaneClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != currentLevel) {
            TILES.clear(); cursor = 0; samples = 0; currentLevel = mc.level;
        }
        if (!enabled || mc.level == null || mc.player == null || mc.isPaused()) return;
        // Main-thread reads only. One chunk per tick, never request generation.
        int cx = mc.player.chunkPosition().x + cursor % WIDTH - RADIUS;
        int cz = mc.player.chunkPosition().z + cursor / WIDTH - RADIUS;
        cursor = (cursor + 1) % (WIDTH * WIDTH);
        LevelChunk loaded = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
        if (loaded == null) return;
        TerrainTile tile = sample(loaded);
        long key = ((long) cx << 32) ^ (cz & 0xffffffffL);
        TILES.remove(key);
        TILES.put(key, tile);
        if (TILES.size() > LIMIT) TILES.remove(TILES.keySet().iterator().next());
        samples++;
    }

    private static TerrainTile sample(LevelChunk chunk) {
        int[] heights = new int[16];
        String[] materials = new String[16];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
            int index = z * 4 + x;
            int best = Integer.MIN_VALUE;
            int bestX = x * 4, bestZ = z * 4;
            for (int dz = 0; dz < 4; dz++) for (int dx = 0; dx < 4; dx++) {
                int lx = x * 4 + dx, lz = z * 4 + dz;
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
                if (y > best) { best = y; bestX = lx; bestZ = lz; }
            }
            heights[index] = best;
            pos.set(chunk.getPos().getMinBlockX() + bestX, best,
                    chunk.getPos().getMinBlockZ() + bestZ);
            materials[index] = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).toString();
        }
        return new TerrainTile(chunk.getPos().x, chunk.getPos().z, heights, materials);
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arcane")
            .executes(ctx -> message("Arcane Horizon: /arcane status | sample | toggle | clear"))
            .then(Commands.literal("status").executes(ctx -> message(
                "Arcane Horizon 0.1.0-alpha | Capture: " + enabled + " | Tuiles: "
                + TILES.size() + "/" + LIMIT + " | Cellules: " + TILES.size() * 16
                + " | Releves: " + samples + " | Rendu lointain: pas encore implemente")))
            .then(Commands.literal("toggle").executes(ctx -> {
                enabled = !enabled; return message("Capture du terrain: " + enabled);
            }))
            .then(Commands.literal("clear").executes(ctx -> {
                TILES.clear(); samples = 0; return message("Cache de terrain vide.");
            }))
            .then(Commands.literal("sample").executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return message("Entre dans un monde pour tester.");
                int cx = mc.player.chunkPosition().x, cz = mc.player.chunkPosition().z;
                TerrainTile tile = TILES.get(((long) cx << 32) ^ (cz & 0xffffffffL));
                if (tile == null) return message("Chunk pas encore releve: attends 15 secondes.");
                return message("Chunk " + cx + ", " + cz + " | 16 cellules | Y: "
                    + tile.minHeight() + " a " + tile.maxHeight()
                    + " | Premier materiau: " + tile.materials()[0]);
            })));
    }

    private static int message(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(text), false);
        else ArcaneHorizon.LOG.info(text);
        return 1;
    }
}
