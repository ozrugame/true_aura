package dev.trueaura.client;

/** A level front orbit, independent of player pitch and GUI scale. */
public final class AuraPreviewFraming {
    public record Orbit(double x, double z, float yaw) { }
    public static Orbit orbit(double bodyYaw, int width, int height, int freeWidth, double verticalFov) {
        return orbit(bodyYaw, width, height, freeWidth, verticalFov, 4);
    }
    public static Orbit orbit(double bodyYaw, int width, int height, int freeWidth, double verticalFov, double distance) {
        double targetNdc = freeWidth / (double) Math.max(1, width) - 1;
        double tanH = Math.tan(Math.toRadians(verticalFov / 2)) * width / Math.max(1.0, height);
        double shift = -targetNdc * distance * tanH;
        double angle = Math.toRadians(bodyYaw);
        return new Orbit(-Math.sin(angle) * distance + Math.cos(angle) * shift,
            Math.cos(angle) * distance + Math.sin(angle) * shift, (float) (bodyYaw + 180));
    }
    private AuraPreviewFraming() { }
}
