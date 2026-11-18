package dev.trueaura.aura;

import java.util.OptionalInt;

public record AuraSettings(int mainColor, int highlightColor, int baseColor,
                           double intensity, double scale, double speed, AuraShape shape,
                           AuraEffect openEffect, AuraEffect closeEffect, CustomAura custom) {
    public AuraSettings(int mainColor, int highlightColor, int baseColor, double intensity, double scale, double speed, AuraShape shape, AuraEffect openEffect, AuraEffect closeEffect) {
        this(mainColor, highlightColor, baseColor, intensity, scale, speed, shape, openEffect, closeEffect, null);
    }
    public AuraSettings(int mainColor, int highlightColor, int baseColor,
                        double intensity, double scale, double speed, AuraShape shape) {
        this(mainColor, highlightColor, baseColor, intensity, scale, speed, shape, AuraEffect.BURST, AuraEffect.BURST);
    }
    public static final AuraSettings DEFAULT = new AuraSettings(0xFFB30F, 0xFFF5A0, 0xB080FF, 1, 1, 1, AuraShape.FLAME);
    public AuraSettings {
        mainColor &= 0xFFFFFF;
        highlightColor &= 0xFFFFFF;
        baseColor &= 0xFFFFFF;
        intensity = finiteRange(intensity, 0.1, AuraLimits.HARD_MAX);
        scale = finiteRange(scale, 0.5, AuraLimits.HARD_MAX);
        speed = finiteRange(speed, 0.1, 3);
        if (shape == null) shape = AuraShape.FLAME;
        if (custom != null) shape = custom.pattern();
        if (openEffect == null) openEffect = AuraEffect.BURST;
        if (closeEffect == null) closeEffect = AuraEffect.BURST;
    }
    public double impactScale() { return Math.min(2, scale); }
    public AuraEffect effect(boolean opening) { return opening ? openEffect : closeEffect; }
    public AuraSettings withEffect(boolean opening, AuraEffect effect) {
        return new AuraSettings(mainColor, highlightColor, baseColor, intensity, scale, speed, shape,
            opening ? effect : openEffect, opening ? closeEffect : effect, custom);
    }
    public AuraSettings withAppearance(double intensity, double scale, double speed, AuraShape shape) {
        return new AuraSettings(mainColor, highlightColor, baseColor, intensity, scale, speed, shape, openEffect, closeEffect, shape == this.shape ? custom : null);
    }
    public AuraSettings withColors(int main, int highlight, int base) {
        return new AuraSettings(main, highlight, base, intensity, scale, speed, shape, openEffect, closeEffect, custom);
    }
    public AuraSettings withCustom(CustomAura definition) {
        return new AuraSettings(mainColor, highlightColor, baseColor, intensity, scale, speed,
            definition == null ? shape : definition.pattern(), openEffect, closeEffect, definition);
    }
    public AuraSettings withShape(AuraShape value) {
        return new AuraSettings(mainColor, highlightColor, baseColor, intensity, scale, speed, value, openEffect, closeEffect, null);
    }
    private static double finiteRange(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : 1;
    }
    public static OptionalInt parseHex(String text) {
        if (text == null) return OptionalInt.empty();
        String value = text.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (!value.matches("[0-9a-fA-F]{6}")) return OptionalInt.empty();
        return OptionalInt.of(Integer.parseInt(value, 16));
    }
    public static String formatHex(int color) {
        return String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }
}
