package dev.trueaura.client;

import net.minecraftforge.common.ForgeConfigSpec;
import dev.trueaura.aura.AuraSettings;
import dev.trueaura.aura.AuraLimits;
import dev.trueaura.aura.AuraShape;
import dev.trueaura.aura.AuraEffect;

public final class AuraConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> CUSTOM;
    private static final ForgeConfigSpec.BooleanValue INSTANT_APPEAR, INSTANT_DISAPPEAR, RISE_FROM_FEET;
    private static final ForgeConfigSpec.IntValue APPEAR_TICKS, DISAPPEAR_TICKS;
    private static final ForgeConfigSpec.DoubleValue MOTION;
    private static String cachedJson;
    private static dev.trueaura.aura.CustomAura cachedCustom;
    public static final ForgeConfigSpec.DoubleValue SCALE;
    public static final ForgeConfigSpec.DoubleValue INTENSITY;
    public static final ForgeConfigSpec.DoubleValue SPEED;
    public static final ForgeConfigSpec.DoubleValue MAX_SCALE, MAX_INTENSITY;
    public static final ForgeConfigSpec.IntValue MAIN_COLOR;
    public static final ForgeConfigSpec.IntValue HIGHLIGHT_COLOR;
    public static final ForgeConfigSpec.IntValue BASE_COLOR;
    public static final ForgeConfigSpec.EnumValue<AuraShape> SHAPE;
    public static final ForgeConfigSpec.ConfigValue<String> OPEN_EFFECT, CLOSE_EFFECT, OPEN_PARAMETERS, CLOSE_PARAMETERS;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("limits");
        MAX_SCALE = builder.comment("Local-only maximum size when the server does not have True Aura. Modded worlds use trueaura-server.toml.")
            .defineInRange("maxScale", 8.0, .5, AuraLimits.HARD_MAX);
        MAX_INTENSITY = builder.comment("Local-only maximum intensity when the server does not have True Aura.")
            .defineInRange("maxIntensity", 8.0, .1, AuraLimits.HARD_MAX);
        builder.pop();
        builder.push("appearance");
        CUSTOM = builder.comment("Embedded imported aura definition; empty uses a built-in shape. Use O to import JSON files.")
            .define("customDefinition", "", value -> value instanceof String text && text.length() <= dev.trueaura.aura.CustomAura.MAX_BYTES);
        SCALE = builder.comment("Overall aura size. Default: 1.0")
            .defineInRange("scale", 1.0, 0.5, AuraLimits.HARD_MAX);
        INTENSITY = builder.comment("Light/opacity strength. Default: 1.0")
            .defineInRange("intensity", 1.0, 0.1, AuraLimits.HARD_MAX);
        SPEED = builder.comment("Flame animation speed. Default: 1.0")
            .defineInRange("speed", 1.0, 0.1, 3.0);
        MAIN_COLOR = builder.comment("Main energy RGB color (integer). Use the in-game editor for hex colors.")
            .defineInRange("mainColor", AuraSettings.DEFAULT.mainColor(), 0, 0xFFFFFF);
        HIGHLIGHT_COLOR = builder.defineInRange("highlightColor", AuraSettings.DEFAULT.highlightColor(), 0, 0xFFFFFF);
        BASE_COLOR = builder.defineInRange("baseColor", AuraSettings.DEFAULT.baseColor(), 0, 0xFFFFFF);
        SHAPE = builder.defineEnum("shape", AuraShape.FLAME);
        builder.pop();
        builder.push("transitions");
        OPEN_EFFECT = builder.define("openEffect", AuraEffect.BURST.id());
        CLOSE_EFFECT = builder.define("closeEffect", AuraEffect.BURST.id());
        OPEN_PARAMETERS = builder.define("openParameters", "");
        CLOSE_PARAMETERS = builder.define("closeParameters", "");
        builder.pop();
        builder.push("animation");
        RISE_FROM_FEET = builder.comment("Grow flame tongues upward from the feet during gradual appearance.").define("riseFromFeet", true);
        INSTANT_APPEAR = builder.define("instantAppear", false);
        INSTANT_DISAPPEAR = builder.define("instantDisappear", false);
        APPEAR_TICKS = builder.comment("Fade-in duration in ticks (20 ticks per second). Retained when instant mode is used.").defineInRange("appearTicks", 10, 1, 100);
        DISAPPEAR_TICKS = builder.defineInRange("disappearTicks", 10, 1, 100);
        MOTION = builder.comment("Movement-driven spring/ripple strength. 0 disables deformation.").defineInRange("motionStrength", 1.0, 0.0, 2.0);
        builder.pop();
        SPEC = builder.build();
    }

    public static AuraLimits localLimits() { return new AuraLimits(MAX_SCALE.get(), MAX_INTENSITY.get()); }

    public static AuraSettings settings() {
        return new AuraSettings(MAIN_COLOR.get(), HIGHLIGHT_COLOR.get(), BASE_COLOR.get(),
            INTENSITY.get(), SCALE.get(), SPEED.get(), SHAPE.get(),
            new AuraEffect(OPEN_EFFECT.get(), OPEN_PARAMETERS.get()), new AuraEffect(CLOSE_EFFECT.get(), CLOSE_PARAMETERS.get()), custom(),
            new dev.trueaura.aura.AuraAnimation(INSTANT_APPEAR.get(), INSTANT_DISAPPEAR.get(), APPEAR_TICKS.get(), DISAPPEAR_TICKS.get(), MOTION.get(), RISE_FROM_FEET.get()));
    }

    private static dev.trueaura.aura.CustomAura custom() {
        String json = CUSTOM.get();
        if (!json.equals(cachedJson)) {
            cachedJson = json; cachedCustom = null;
            if (!json.isEmpty()) try { cachedCustom = dev.trueaura.aura.CustomAura.parse(json); }
            catch (IllegalArgumentException ex) { com.mojang.logging.LogUtils.getLogger().warn("Invalid saved custom aura: {}", ex.getMessage()); }
        }
        return cachedCustom;
    }
    public static void save(AuraSettings settings) {
        var animation = settings.animation();
        RISE_FROM_FEET.set(animation.riseFromFeet());
        INSTANT_APPEAR.set(animation.instantAppear()); INSTANT_DISAPPEAR.set(animation.instantDisappear());
        APPEAR_TICKS.set(animation.appearTicks()); DISAPPEAR_TICKS.set(animation.disappearTicks()); MOTION.set(animation.motionStrength());
        CUSTOM.set(settings.custom() == null ? "" : settings.custom().toJson());
        MAIN_COLOR.set(settings.mainColor());
        HIGHLIGHT_COLOR.set(settings.highlightColor());
        BASE_COLOR.set(settings.baseColor());
        INTENSITY.set(settings.intensity());
        SCALE.set(settings.scale());
        SPEED.set(settings.speed());
        SHAPE.set(settings.shape());
        OPEN_EFFECT.set(settings.openEffect().id());
        CLOSE_EFFECT.set(settings.closeEffect().id());
        OPEN_PARAMETERS.set(settings.openEffect().parameters());
        CLOSE_PARAMETERS.set(settings.closeEffect().parameters());
        SPEC.save();
    }
    private AuraConfig() { }
}
