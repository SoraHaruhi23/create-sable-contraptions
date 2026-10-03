package dev.createsablecontraptions.client;

import dev.createsablecontraptions.CreateSableContraptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateSableContraptions.ID, dist = Dist.CLIENT)
public final class CscClient {
    public CscClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
