package dev.createsablecontraptions.physics;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Gameplay status independent of optional developer diagnostics. Positions remain in plot coordinates. */
public final class CollisionState {
    public record Hit(int reason,BlockPos target) {}
    private static final Map<AbstractContraptionEntity,Hit> LAST=new WeakHashMap<>();
    private CollisionState() {}
    public static void begin(AbstractContraptionEntity e) { if(e!=null)LAST.remove(e); }
    public static Hit get(AbstractContraptionEntity e) { return LAST.get(e); }
    public static void restore(AbstractContraptionEntity e,Hit hit) { if(hit!=null)LAST.put(e,hit); }
    public static boolean fail(AbstractContraptionEntity e,int reason) {
        if(e!=null)LAST.put(e,new Hit(reason,null)); return true;
    }
    public static void contact(AbstractContraptionEntity e,BlockPos pos) {
        if(e!=null)LAST.putIfAbsent(e,new Hit(StructureStatus.COLLISION,pos.immutable()));
    }
}
