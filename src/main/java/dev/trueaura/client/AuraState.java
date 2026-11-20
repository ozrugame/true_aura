package dev.trueaura.client;

/** Tick-driven local state; intentionally independent of Minecraft. */
public final class AuraState {
    private boolean enabled;
    private float previousOpacity;
    private float opacity;
    private long ticks;
    private dev.trueaura.aura.AuraAnimation animation = dev.trueaura.aura.AuraAnimation.DEFAULT;
    private final AuraMotion motion = new AuraMotion();
    public AuraMotion motion() { return motion; }
    public void configure(dev.trueaura.aura.AuraAnimation value) { animation = value; snapIfInstant(); }
    private void snapIfInstant() {
        if (enabled ? animation.instantAppear() : animation.instantDisappear()) previousOpacity = opacity = enabled ? 1 : 0;
    }

    public void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean value) { enabled = value; snapIfInstant(); }

    public void tick(boolean activeSession) {
        if (!activeSession) {
            reset();
            return;
        }
        previousOpacity = opacity;
        opacity = clamp(opacity + (enabled ? 1f / animation.appearTicks() : -1f / animation.disappearTicks()));
        snapIfInstant();
        ticks++;
    }

    public void reset() {
        enabled = false;
        previousOpacity = opacity = 0;
        ticks = 0;
        motion.reset();
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
