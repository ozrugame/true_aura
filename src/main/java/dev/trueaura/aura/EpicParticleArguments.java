package dev.trueaura.aura;

/** Epic Fight 20.14.x particle-provider arguments; kept separate from ordinary velocity. */
public record EpicParticleArguments(double a, double b, double c, double height, boolean ground) {
    private static double bits(int value) { return Double.longBitsToDouble(Integer.toUnsignedLong(value)); }
    public static EpicParticleArguments forEffect(String path, double scale, int entityId, float pitch, float yaw, boolean opening) {
        return switch (path) {
            case "ground_slam" -> new EpicParticleArguments(scale * 1.6, opening ? 32 : 24, opening ? 1.7 : -1.1, .03, true);
            case "neutralize" -> new EpicParticleArguments(scale * 2, bits(8), 0, 1, false);
            case "boss_casting" -> new EpicParticleArguments(scale * 2, bits(12), bits(1), 1, false);
            // Unsigned low 32 bits survive Double.doubleToLongBits; -1L would become canonical NaN.
            case "force_field" -> new EpicParticleArguments(0, bits(-1), 0, 1, false);
            case "air_burst" -> new EpicParticleArguments(90, yaw, 6, 1, false);
            case "white_afterimage", "adrenaline_player_beating" -> new EpicParticleArguments(bits(entityId), 0, 0, 0, false);
            case "eviscerate" -> new EpicParticleArguments(pitch, yaw, 0, 1, false);
            case "force_field_end", "hit_blade", "blade_rush", "hit_blunt" -> new EpicParticleArguments(0, 0, 0, 1, false);
            default -> null;
        };
    }
}
