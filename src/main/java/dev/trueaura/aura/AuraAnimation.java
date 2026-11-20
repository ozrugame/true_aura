package dev.trueaura.aura;

/** Shared visual settings. Durations are retained while the corresponding instant mode is selected. */
public record AuraAnimation(boolean instantAppear, boolean instantDisappear, int appearTicks,
                            int disappearTicks, double motionStrength, boolean riseFromFeet) {
    public AuraAnimation(boolean instantAppear, boolean instantDisappear, int appearTicks, int disappearTicks, double motionStrength) {
        this(instantAppear, instantDisappear, appearTicks, disappearTicks, motionStrength, true);
    }
    public static final AuraAnimation DEFAULT = new AuraAnimation(false, false, 10, 10, 1);
    public AuraAnimation {
        appearTicks = Math.max(1, Math.min(100, appearTicks));
        disappearTicks = Math.max(1, Math.min(100, disappearTicks));
        motionStrength = Double.isFinite(motionStrength) ? Math.max(0, Math.min(2, motionStrength)) : 1;
    }
    public String appearMode() { return instantAppear ? "instant" : riseFromFeet ? "rise" : "fade"; }
    public AuraAnimation withRise(boolean rise) { return new AuraAnimation(false, instantDisappear, appearTicks, disappearTicks, motionStrength, rise); }
    public AuraAnimation nextAppearMode() { return instantAppear ? withRise(false) : riseFromFeet ? withAppear(true) : withRise(true); }
    public AuraAnimation withAppear(boolean instant) { return new AuraAnimation(instant, instantDisappear, appearTicks, disappearTicks, motionStrength, riseFromFeet); }
    public AuraAnimation withDisappear(boolean instant) { return new AuraAnimation(instantAppear, instant, appearTicks, disappearTicks, motionStrength, riseFromFeet); }
    public AuraAnimation withDurations(int in, int out) { return new AuraAnimation(instantAppear, instantDisappear, in, out, motionStrength, riseFromFeet); }
    public AuraAnimation withMotion(double strength) { return new AuraAnimation(instantAppear, instantDisappear, appearTicks, disappearTicks, strength, riseFromFeet); }
}
