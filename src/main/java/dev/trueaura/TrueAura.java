package dev.trueaura;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import dev.trueaura.client.AuraClient;
import dev.trueaura.network.AuraNetwork;

/** Client initialization is guarded so dedicated servers never load rendering classes. */
@Mod(TrueAura.MOD_ID)
public final class TrueAura {
    public static final String MOD_ID = "trueaura";
    public TrueAura() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
            dev.trueaura.config.AuraServerConfig.SPEC);
        AuraNetwork.initialize();
        // safeRunWhenOn validates the supplier even on the opposite dist in userdev.
        // With Java 17 that validation links AuraClient's Screen references on a server.
        // The guarded variant evaluates this deferred supplier only on CLIENT.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> AuraClient::initialize);
    }
}
