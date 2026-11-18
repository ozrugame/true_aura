package dev.trueaura.config;

import dev.trueaura.aura.AuraLimits;
import net.minecraftforge.common.ForgeConfigSpec;

/** Forge SERVER configs are per-world and automatically synchronized to connecting clients. */
public final class AuraServerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue MAX_SCALE, MAX_INTENSITY;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("limits");
        MAX_SCALE = builder.comment("Maximum aura size allowed in this world. Synced to players. Default: 8.")
            .defineInRange("maxScale", 8.0, .5, AuraLimits.HARD_MAX);
        MAX_INTENSITY = builder.comment("Maximum aura intensity allowed in this world. Synced to players. Default: 8.")
            .defineInRange("maxIntensity", 8.0, .1, AuraLimits.HARD_MAX);
        builder.pop();
        SPEC = builder.build();
    }
    public static AuraLimits limits() {
        return SPEC.isLoaded() ? new AuraLimits(MAX_SCALE.get(), MAX_INTENSITY.get()) : AuraLimits.DEFAULT;
    }
    private AuraServerConfig() {}
}
