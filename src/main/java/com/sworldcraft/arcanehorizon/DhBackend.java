package com.sworldcraft.arcanehorizon;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.seibel.distanthorizons.api.DhApi;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Integration only: Distant Horizons remains a separate, credited engine mod. */
@EventBusSubscriber(modid=ArcaneHorizon.ID,value=Dist.CLIENT)
public final class DhBackend {
    private DhBackend() {}
    public static boolean active(){return ModList.get().isLoaded("distanthorizons");}
    private static boolean ready(){return active() && DhApi.Delayed.configs!=null;}
    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arcane")
            .executes(ctx -> message("Arcane Horizon: moteur Distant Horizons | /arcane status | preset | range <chunks> | render | generation"))
            .then(Commands.literal("status").executes(ctx -> {
                if(!ready())return message("Arcane Horizon 0.5.0-alpha | Moteur DH en initialisation. Aucun rendu experimental actif.");
                var c=DhApi.Delayed.configs;
                return message("Arcane Horizon 0.5.0-alpha | Moteur: Distant Horizons "+DhApi.getModVersion()
                    +" | Portee: "+c.graphics().chunkRenderDistance().getValue()+" chunks | Rendu: "
                    +c.graphics().renderingEnabled().getValue()+" | Generation: "
                    +c.worldGenerator().enableDistantWorldGeneration().getValue()+" | Ancien moteur: desactive");
            }))
            .then(Commands.literal("preset").executes(ctx -> {
                if(!ready())return message("Attends la fin de l'initialisation de Distant Horizons.");
                var c=DhApi.Delayed.configs;var g=c.graphics();
                boolean ok=g.chunkRenderDistance().setValue(64);
                ok=g.renderingEnabled().setValue(true)&ok;
                ok=g.brightnessMultiplier().setValue(g.brightnessMultiplier().getDefaultValue())&ok;
                ok=g.saturationMultiplier().setValue(g.saturationMultiplier().getDefaultValue())&ok;
                ok=c.worldGenerator().enableDistantWorldGeneration().setValue(true)&ok;
                return message(ok?"Profil DH actif: 64 chunks, rendu et generation actives, couleurs par defaut de DH."
                    :"Certains reglages ont ete refuses par DH. Verifie ses options et latest.log.");
            }))
            .then(Commands.literal("range").then(Commands.argument("chunks",IntegerArgumentType.integer(8,512)).executes(ctx -> {
                if(!ready())return message("DH n'est pas encore initialise.");
                int chunks=IntegerArgumentType.getInteger(ctx,"chunks");
                return message(DhApi.Delayed.configs.graphics().chunkRenderDistance().setValue(chunks)
                    ?"Portee DH: "+chunks+" chunks":"Portee refusee par DH.");
            })))
            .then(Commands.literal("render").executes(ctx -> {
                if(!ready())return message("DH n'est pas encore initialise.");
                var v=DhApi.Delayed.configs.graphics().renderingEnabled();boolean value=!v.getValue();
                return message(v.setValue(value)?"Rendu DH: "+value:"Modification refusee par DH.");
            }))
            .then(Commands.literal("generation").executes(ctx -> {
                if(!ready())return message("DH n'est pas encore initialise.");
                var v=DhApi.Delayed.configs.worldGenerator().enableDistantWorldGeneration();boolean value=!v.getValue();
                return message(v.setValue(value)?"Generation DH: "+value+" (en solo ou avec serveur compatible)":"Modification refusee par DH.");
            }))
            .then(Commands.literal("brightness").then(Commands.argument("value",FloatArgumentType.floatArg(0.2f,1.5f)).executes(ctx -> {
                if(!ready())return message("DH n'est pas encore initialise.");
                return message(DhApi.Delayed.configs.graphics().brightnessMultiplier().setValue(FloatArgumentType.getFloat(ctx,"value"))
                    ?"Luminosite DH mise a jour.":"Reglage refuse par DH.");
            })))
            .then(Commands.literal("saturation").then(Commands.argument("value",FloatArgumentType.floatArg(0,1.5f)).executes(ctx -> {
                if(!ready())return message("DH n'est pas encore initialise.");
                return message(DhApi.Delayed.configs.graphics().saturationMultiplier().setValue(FloatArgumentType.getFloat(ctx,"value"))
                    ?"Saturation DH mise a jour.":"Reglage refuse par DH.");
            })))
            .then(Commands.literal("foliage").executes(ctx -> message("Le feuillage est maintenant gere par le moteur Distant Horizons.")))
            .then(Commands.literal("sample").executes(ctx -> message("La capture et les donnees sont maintenant gerees par Distant Horizons. Utilise /arcane status."))));
    }
    private static int message(String text){
        var mc=Minecraft.getInstance();
        if(mc.player!=null)mc.gui.hud.getChat().addClientSystemMessage(Component.literal(text));
        else ArcaneHorizon.LOG.info(text);
        return 1;
    }
}
