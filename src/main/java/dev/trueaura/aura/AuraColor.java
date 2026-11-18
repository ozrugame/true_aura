package dev.trueaura.aura;

/** Color math independent of the renderer, so opening the picker never changes a saved RGB value. */
public final class AuraColor {
    public record Hsv(double hue, double saturation, double value) { }
    public static int rgb(double hue, double saturation, double value) {
        double h = Double.isFinite(hue) ? hue - Math.floor(hue) : 0;
        double s = clamp(saturation), v = clamp(value);
        double sector = h * 6, c = v * s, x = c * (1 - Math.abs(sector % 2 - 1)), m = v - c;
        double r = 0, g = 0, b = 0;
        switch ((int) sector) {
            case 0 -> { r = c; g = x; }
            case 1 -> { r = x; g = c; }
            case 2 -> { g = c; b = x; }
            case 3 -> { g = x; b = c; }
            case 4 -> { r = x; b = c; }
            default -> { r = c; b = x; }
        }
        return (int) Math.round((r + m) * 255) << 16 | (int) Math.round((g + m) * 255) << 8 | (int) Math.round((b + m) * 255);
    }
    public static Hsv hsv(int rgb) {
        double r = ((rgb >> 16) & 255) / 255.0, g = ((rgb >> 8) & 255) / 255.0, b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
        double h = 0;
        if (delta > 0) {
            if (max == r) h = (g - b) / delta;
            else if (max == g) h = (b - r) / delta + 2;
            else h = (r - g) / delta + 4;
            h = (h / 6 + 1) % 1;
        }
        return new Hsv(h, max == 0 ? 0 : delta / max, max);
    }
    public static double clamp(double value) { return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }
    private AuraColor() { }
}
