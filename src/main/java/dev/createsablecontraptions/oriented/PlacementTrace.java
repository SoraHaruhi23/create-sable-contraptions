package dev.createsablecontraptions.oriented;

import java.util.*;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

/** Server-side wall-clock samples only; no NBT serialization or inventory reads. */
public final class PlacementTrace {
    public static final class Session {
        final long started=System.nanoTime();
        final Map<String,Long> times=new LinkedHashMap<>();
        void add(String phase,long nanos) { times.merge(phase,nanos,Long::sum); }
    }
    private static final ThreadLocal<Session> ACTIVE=new ThreadLocal<>();
    private static final Map<AbstractMinecart,String> LAST=new WeakHashMap<>();
    private PlacementTrace() {}
    public static Session begin() { var previous=ACTIVE.get(); ACTIVE.set(new Session()); return previous; }
    public static void phase(String name,long started) { var s=ACTIVE.get(); if(s!=null)s.add(name,System.nanoTime()-started); }
    public static void end(AbstractMinecart cart,Session previous) {
        var s=ACTIVE.get();
        try {
            if(s==null)return;
            var line=new StringBuilder("服务端结构恢复调用总耗时=").append(ms(System.nanoTime()-s.started)).append(" ms");
            s.times.forEach((k,v)->line.append("；").append(k).append('=').append(ms(v)).append(" ms"));
            line.append("（不含后续区块传输及客户端处理）");
            LAST.put(cart,line.toString());
        } finally { if(previous==null)ACTIVE.remove();else ACTIVE.set(previous); }
    }
    private static String ms(long nanos) { return String.format(Locale.ROOT,"%.2f",nanos/1_000_000.0); }
    public static String describe(AbstractMinecart cart) { return LAST.getOrDefault(cart,"本次会话没有该矿车的放置计时记录"); }
}
