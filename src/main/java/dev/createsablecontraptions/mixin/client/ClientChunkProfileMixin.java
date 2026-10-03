package dev.createsablecontraptions.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.createsablecontraptions.client.ClientPlacementProfile;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=ClientPacketListener.class,remap=false)
public abstract class ClientChunkProfileMixin {
    @WrapMethod(method="handleLevelChunkWithLight")
    private void csc$timeChunk(ClientboundLevelChunkWithLightPacket packet,Operation<Void> original) {
        // Packet dispatch first visits the network thread, then reschedules on the client.
        long start=net.minecraft.client.Minecraft.getInstance().isSameThread()?ClientPlacementProfile.start():0;
        try { original.call(packet); } finally { ClientPlacementProfile.end("客户端区块处理（含普通区块）",start); }
    }
}
