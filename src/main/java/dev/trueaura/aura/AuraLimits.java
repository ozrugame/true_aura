package dev.trueaura.aura;

/** User-configured limits inside finite renderer/network bounds. */
public record AuraLimits(double maxScale, double maxIntensity) {
    public static final double HARD_MAX = 32;
    public static final AuraLimits DEFAULT = new AuraLimits(8, 8);
    public AuraLimits {
        maxScale = bound(maxScale, .5);
        maxIntensity = bound(maxIntensity, .1);
    }
    private static double bound(double value, double min) {
        return Double.isFinite(value) ? Math.max(min, Math.min(HARD_MAX, value)) : 8;
    }
    public AuraSettings apply(AuraSettings settings) {
        return settings.withAppearance(Math.min(maxIntensity, settings.intensity()),
            Math.min(maxScale, settings.scale()), settings.speed(), settings.shape());
    }
}
