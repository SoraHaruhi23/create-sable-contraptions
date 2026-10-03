package dev.createsablecontraptions.elevator;

import com.simibubi.create.content.contraptions.Contraption;
import java.util.UUID;

/** Saved on Create's logical contraption, including its spawn data. */
public interface ElevatorLink {
    UUID csc$getSubLevel();
    void csc$setSubLevel(UUID id);
    boolean csc$isBlocked();
    void csc$setBlocked(boolean blocked);
    boolean csc$isPhysicalChild();
    void csc$setPhysicalChild(boolean value);

    static boolean managed(Contraption contraption) {
        return supported(contraption) && contraption instanceof ElevatorLink link && link.csc$getSubLevel() != null;
    }

    static boolean bearing(Contraption contraption) {
        return contraption instanceof com.simibubi.create.content.contraptions.bearing.BearingContraption
                || contraption instanceof com.simibubi.create.content.contraptions.bearing.ClockworkContraption;
    }

    static boolean supported(Contraption contraption) {
        return linear(contraption) || bearing(contraption) || oriented(contraption);
    }
    static boolean oriented(Contraption c) {
        return c instanceof com.simibubi.create.content.contraptions.mounted.MountedContraption
                || c instanceof com.simibubi.create.content.contraptions.bearing.StabilizedContraption
                && c instanceof ElevatorLink link && link.csc$isPhysicalChild();
    }

    static boolean linear(Contraption contraption) {
        return contraption instanceof com.simibubi.create.content.contraptions.pulley.PulleyContraption
                || contraption instanceof com.simibubi.create.content.contraptions.piston.PistonContraption
                || contraption instanceof com.simibubi.create.content.contraptions.gantry.GantryContraption;
    }
}
