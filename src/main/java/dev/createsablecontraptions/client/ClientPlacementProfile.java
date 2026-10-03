package dev.createsablecontraptions.client;

import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Opt-in, bounded client timing; no payloads or inventory data are retained. */
@EventBusSubscriber(modid="create_sable_contraptions",value=Dist.CLIENT)
public final class ClientPlacementProfile {
    private record Sample(int count,long total,long max) {}
    private static final Map<String,Sample> SAMPLES=new LinkedHashMap<>();
    private static long deadline;
    private ClientPlacementProfile() {}
    public static long start() { return System.nanoTime()<deadline?System.nanoTime():0; }
    public static void end(String name,long start) {
        if(start==0)return;
        long elapsed=System.nanoTime()-start;
        var old=SAMPLES.getOrDefault(name,new Sample(0,0,0));
        SAMPLES.put(name,new Sample(old.count+1,old.total+elapsed,Math.max(old.max,elapsed)));
    }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("csc_client_profile")
            .then(Commands.literal("start").executes(c->{
                SAMPLES.clear();deadline=System.nanoTime()+60_000_000_000L;
                c.getSource().sendSuccess(()->Component.translatable("csc.profile.start"),false);return 1;
            }))
            .then(Commands.literal("show").executes(c->{
                deadline=0;
                if(SAMPLES.isEmpty())c.getSource().sendSuccess(()->Component.translatable("csc.profile.empty"),false);
                SAMPLES.forEach((name,s)->c.getSource().sendSuccess(()->Component.translatable("csc.profile.sample",
                        name,s.count,String.format(Locale.ROOT,"%.2f",s.total/1e6),String.format(Locale.ROOT,"%.2f",s.max/1e6)),false));return 1;
            })));
    }
}
