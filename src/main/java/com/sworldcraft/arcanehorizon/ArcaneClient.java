package com.sworldcraft.arcanehorizon;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayDeque;
import net.minecraft.client.renderer.BiomeColors;
import net.neoforged.neoforge.event.level.ChunkEvent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = ArcaneHorizon.ID, value = Dist.CLIENT)
public final class ArcaneClient {
    private static final int LIMIT = 8192;
    private static final ArrayDeque<LevelChunk> PENDING = new ArrayDeque<>();
    private static final int MAX_PENDING = 2048;
    private static final Map<Long, TerrainTile> TILES = new LinkedHashMap<>();
    private static ClientLevel currentLevel;
    private static int cursor;
    private static boolean enabled = true;
    private static long samples;
    static boolean renderEnabled = true;
    static boolean preview = false;
    static int renderRange = 32;
    static volatile int visibleTiles;
    static float brightness = 0.95f;
    static float saturation = 0.85f;
    static int overlap = 3;
    static boolean culling = true;
    static volatile int renderedVertices;
    static List<TerrainTile> snapshot(ClientLevel level) {
        return level == currentLevel ? List.copyOf(TILES.values()) : List.of();
    }

    private ArcaneClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != currentLevel) {
            TILES.clear(); PENDING.clear(); cursor = 0; samples = 0; currentLevel = mc.level;
        }
        if (!enabled || mc.level == null || mc.player == null || mc.isPaused()) return;
        // Main thread only; prioritize newly received / departing chunks, then refresh.
        long deadline=System.nanoTime()+3_000_000;
        int radius=Math.max(2,Math.min(32,mc.options.getEffectiveRenderDistance()+2));
        int width=radius*2+1;
        for(int work=0;work<4;work++) {
            LevelChunk chunk=PENDING.pollFirst();
            if(chunk!=null) {
                if(chunk.getLevel()==mc.level) capture(chunk);
            } else {
                cursor%=width*width;
                int cx=mc.player.chunkPosition().x()+cursor%width-radius;
                int cz=mc.player.chunkPosition().z()+cursor/width-radius;
                cursor=(cursor+1)%(width*width);
                LevelChunk loaded=mc.level.getChunkSource().getChunk(cx,cz,ChunkStatus.FULL,false);
                if(loaded!=null)capture(loaded);
            }
            if(System.nanoTime()>=deadline)break;
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) { enqueue(event.getChunk()); }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) { enqueue(event.getChunk()); }

    private static void enqueue(LevelChunk chunk) {
        if(!(chunk.getLevel() instanceof ClientLevel level))return;
        Minecraft.getInstance().execute(() -> {
            if(!enabled || level!=Minecraft.getInstance().level || level!=currentLevel)return;
            if(PENDING.size()>=MAX_PENDING)PENDING.removeFirst();
            PENDING.addLast(chunk);
        });
    }

    private static void capture(LevelChunk chunk) {
        if(!(chunk.getLevel() instanceof ClientLevel level))return;
        int[] heights=new int[256], colors=new int[256], lights=new int[256];
        String[] materials=new String[256];
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
            int i=z*16+x;
            int y=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
            heights[i]=y;
            pos.set(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);
            var state=chunk.getBlockState(pos);
            String name=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            materials[i]=name;
            int rgb=state.getMapColor(level,pos).col;
            // Surface colors, with biome tint where it has a clear meaning.
            if(name.equals("minecraft:grass_block") || name.equals("minecraft:short_grass")
                || name.equals("minecraft:tall_grass"))rgb=BiomeColors.getAverageGrassColor(level,pos);
            else if(name.equals("minecraft:spruce_leaves"))rgb=0x619961;
            else if(name.equals("minecraft:birch_leaves"))rgb=0x80a755;
            else if(name.contains("leaves") && !name.contains("cherry") && !name.contains("pale_oak"))
                rgb=BiomeColors.getAverageFoliageColor(level,pos);
            else if(name.equals("minecraft:water"))rgb=BiomeColors.getAverageWaterColor(level,pos);
            colors[i]=RenderColor.surface(rgb==0 && !state.isAir()?0x8a8a8a:rgb&0xffffff,name);
            pos.set(pos.getX(),y+1,pos.getZ());
            lights[i]=(level.getBrightness(LightLayer.SKY,pos)<<4)|level.getBrightness(LightLayer.BLOCK,pos);
        }
        long key=((long)chunk.getPos().x()<<32)^(chunk.getPos().z()&0xffffffffL);
        TerrainTile tile=TILES.get(key);
        if(tile==null || !tile.sameSurface(heights,materials,colors,lights))
            tile=new TerrainTile(chunk.getPos().x(),chunk.getPos().z(),heights,materials,colors,lights);
        TILES.remove(key);TILES.put(key,tile);
        if(TILES.size()>LIMIT)TILES.remove(TILES.keySet().iterator().next());
        samples++;
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arcane")
            .executes(ctx -> message("Arcane Horizon: /arcane status | sample | toggle | render | preview | range <8-48> | brightness | saturation | overlap | culling | clear"))
            .then(Commands.literal("status").executes(ctx -> message(
                "Arcane Horizon 0.3.3-alpha | Capture: " + enabled + " | Tuiles: "
                + TILES.size() + "/" + LIMIT + " | Cellules: " + TILES.size() * 256
                + " | Releves: " + samples + " | Rendu: " + renderEnabled + " | Tuiles soumises: " + visibleTiles
                + " | Sommets: " + renderedVertices + " | Capture en attente: " + PENDING.size() + " | Portee: " + renderRange + " chunks | Apercu proche: " + preview)))
            .then(Commands.literal("render").executes(ctx -> {
                renderEnabled = !renderEnabled;
                return message("Rendu du relief: " + renderEnabled);
            }))
            .then(Commands.literal("preview").executes(ctx -> {
                preview = !preview;
                return message("Apercu sur les chunks charges: " + preview + " (chevauchement volontaire pour diagnostic)");
            }))
            .then(Commands.literal("range")
                .then(Commands.argument("chunks", IntegerArgumentType.integer(8,48)).executes(ctx -> {
                    renderRange = IntegerArgumentType.getInteger(ctx,"chunks");
                    return message("Portee maximale: " + renderRange + " chunks (limitee aussi par la projection de Minecraft).");
                })))
            .then(Commands.literal("brightness")
                .then(Commands.argument("value",FloatArgumentType.floatArg(0.2f,1.5f)).executes(ctx -> {
                    brightness=FloatArgumentType.getFloat(ctx,"value");
                    return message("Luminosite du relief: "+brightness);
                })))
            .then(Commands.literal("saturation")
                .then(Commands.argument("value",FloatArgumentType.floatArg(0.0f,1.5f)).executes(ctx -> {
                    saturation=FloatArgumentType.getFloat(ctx,"value");
                    return message("Saturation du relief: "+saturation);
                })))
            .then(Commands.literal("overlap")
                .then(Commands.argument("chunks",IntegerArgumentType.integer(0,8)).executes(ctx -> {
                    overlap=IntegerArgumentType.getInteger(ctx,"chunks");
                    return message("Bande de raccord: "+overlap+" chunks");
                })))
            .then(Commands.literal("culling").executes(ctx -> {
                culling=!culling;
                return message("Elimination hors champ: "+culling);
            }))
            .then(Commands.literal("toggle").executes(ctx -> {
                enabled = !enabled; return message("Capture du terrain: " + enabled);
            }))
            .then(Commands.literal("clear").executes(ctx -> {
                TILES.clear(); PENDING.clear(); samples = 0; return message("Cache de terrain vide.");
            }))
            .then(Commands.literal("sample").executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return message("Entre dans un monde pour tester.");
                int cx = mc.player.chunkPosition().x(), cz = mc.player.chunkPosition().z();
                TerrainTile tile = TILES.get(((long) cx << 32) ^ (cz & 0xffffffffL));
                if (tile == null) return message("Chunk pas encore releve: attends 15 secondes.");
                return message("Chunk " + cx + ", " + cz + " | 256 colonnes | Y: "
                    + tile.minHeight() + " a " + tile.maxHeight()
                    + " | Premier materiau: " + tile.materials()[0]);
            })));
    }

    private static int message(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.hud.getChat().addClientSystemMessage(Component.literal(text));
        } else {
            ArcaneHorizon.LOG.info(text);
        }
        return 1;
    }
}
