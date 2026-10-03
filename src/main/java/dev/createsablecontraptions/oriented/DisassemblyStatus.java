package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.*;
import java.util.*;

/** Last actual preflight result; reading diagnostics never attempts a disassembly. */
public final class DisassemblyStatus {
    private record Result(long tick,String reason) {}
    private static final Map<AbstractContraptionEntity,Result> LAST=new WeakHashMap<>();
    private DisassemblyStatus() {}
    public static boolean record(Contraption c,String reason,boolean accepted) {
        var root=c.entity;
        if(root!=null) {
            while(root.getVehicle() instanceof AbstractContraptionEntity parent)root=parent;
            LAST.put(root,new Result(root.level().getGameTime(),reason));
        }
        return accepted;
    }
    public static String describe(AbstractContraptionEntity root) {
        var r=LAST.get(root);
        return r==null?"尚无拆卸检查记录":"最近拆卸检查（"+(root.level().getGameTime()-r.tick)+" tick 前）："+r.reason;
    }
}
