package dev.trueaura.client;

/** Tick-driven local state; intentionally independent of Minecraft. */
public final class AuraState {
    private boolean enabled;
    private float previousOpacity;
    private float opacity;
    private long ticks;

    public void toggle() { enabled = !enabled; }
    public void setEnabled(boolean value) { enabled = value; }

    public void tick(boolean activeSession) {
        if (!activeSession) {
            reset();
            return;
        }
        previousOpacity = opacity;
        opacity = clamp(opacity + (enabled ? 0.1f : -0.1f));
        ticks++;
    }

    public void reset() {
        enabled = false;
        previousOpacity = opacity = 0;
        ticks = 0;
    }

    public boolean enabled() { return enabled; }
    public float opacity(float partialTick) {
        return previousOpacity + (opacity - previousOpacity) * clamp(partialTick);
    }
    public double time(float partialTick) {
        return ticks + (double) clamp(partialTick);
    }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
}
