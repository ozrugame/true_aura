package dev.trueaura.client;

/** Short-lived toggle animation. Initial synchronization is deliberately not a toggle. */
public final class AuraBurst {
    private final boolean opening;
    private int age;
    public AuraBurst(boolean opening) { this.opening = opening; }
    public static boolean shouldStart(Boolean previous, boolean next) {
        return previous != null && previous != next;
    }
    public void tick() { age++; }
    public boolean opening() { return opening; }
    public boolean expired() { return age >= (opening ? 40 : 28); }
    private double time(float partial) { return age + Math.max(0, Math.min(1, partial)); }
    public float opacity(float partial) {
        double p = Math.min(1, time(partial) / (opening ? 40 : 28));
        return (float) ((1 - p) * (1 - p));
    }
    public double maxRadius() { return opening ? 3.4 : 2.6; }
    public double radius(float partial) {
        double t = time(partial);
        return opening ? .2 + 3.2 * (1 - Math.pow(1 - Math.min(1, t / 10), 3))
            : .12 + 2.48 * Math.pow(1 - Math.min(1, t / 12), 2);
    }
    /** A short second outward pulse after the closing contraction. */
    public double echoRadius(float partial) {
        double p = Math.max(0, Math.min(1, (time(partial) - (opening ? 3 : 6)) / 9));
        return p * maxRadius() * .86;
    }
    public float waveOpacity(float partial) {
        double p = Math.min(1, time(partial) / (opening ? 16 : 20));
        return (float) ((1 - p) * (1 - p));
    }
    /** Local wave age: outside tiles react later; closing pulls the outside in first. */
    private double tileTime(double distance, float partial) {
        double d = Math.max(0, Math.min(1, distance));
        return time(partial) - (opening ? d * 5 : (1 - d) * 4 + 2);
    }
    public double tileLift(double distance, float partial) {
        double t = tileTime(distance, partial);
        if (t <= 0 || expired()) return 0;
        double bounce = Math.max(0, 1 - Math.pow((t - 4) / 4, 2));
        double settle = Math.max(0, 1 - Math.max(0, t - 12) / 12);
        double arrival = Math.min(1, t / 2);
        return (opening ? .62 : .36) * bounce + .12 * arrival * settle;
    }
    public double tileTilt(double distance, float partial) {
        double t = tileTime(distance, partial);
        if (t <= 0 || expired()) return 0;
        return (opening ? 24 : -18) * Math.min(1, t / 3)
            * Math.max(0, 1 - Math.max(0, t - 10) / 13);
    }
}
