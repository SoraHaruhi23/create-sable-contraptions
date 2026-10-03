package dev.createsablecontraptions;

import dev.createsablecontraptions.elevator.ElevatorPhysics;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CreateSableContraptions.ID)
public final class CreateSableContraptions {
    public static final String ID = "create_sable_contraptions";

    public static String version() {
        return net.neoforged.fml.ModList.get().getModContainerById(ID)
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse("unknown");
    }

    public CreateSableContraptions(net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, CscConfig.CLIENT);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, CscConfig.SERVER);
        ElevatorPhysics.registerTicket();
        NeoForge.EVENT_BUS.addListener(dev.createsablecontraptions.bearing.BearingStatus::register);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,
                dev.createsablecontraptions.elevator.AssemblerProtection::interact);
        NeoForge.EVENT_BUS.addListener(dev.createsablecontraptions.oriented.CartStatus::register);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                dev.createsablecontraptions.oriented.PackedStructures::onJoin);
        NeoForge.EVENT_BUS.addListener(ElevatorPhysics::beforePhysics);
        NeoForge.EVENT_BUS.addListener(ElevatorPhysics::afterPhysics);
        NeoForge.EVENT_BUS.addListener(ElevatorPhysics::onLevelUnload);
    }
}
