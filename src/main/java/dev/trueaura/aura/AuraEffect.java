package dev.trueaura.aura;

import java.nio.charset.StandardCharsets;

/** A registry ID and particle arguments, never an executable command. */
public record AuraEffect(String id, String parameters) {
    public static final int MAX_ID_BYTES = 128;
    public static final int MAX_PARAMETER_BYTES = 256;
    public static final AuraEffect BURST = new AuraEffect("trueaura:burst", "");
    public static final AuraEffect NONE = new AuraEffect("trueaura:none", "");

    public AuraEffect {
        parameters = parameters == null ? "" : parameters.trim();
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
            || id.length() > MAX_ID_BYTES || !validParameters(parameters)) {
            id = "trueaura:burst";
            parameters = "";
        }
        if (id.equals("trueaura:burst") || id.equals("trueaura:none")) parameters = "";
    }
    public static boolean validParameters(String text) {
        return text != null && text.getBytes(StandardCharsets.UTF_8).length <= MAX_PARAMETER_BYTES
            && text.chars().noneMatch(Character::isISOControl);
    }
    public boolean builtin() { return equals(BURST) || equals(NONE); }
    public boolean timingAllowed() {
        // Shriek's delay is decremented separately from Particle.age, bypassing lifetime caps.
        if (!id.equals("minecraft:shriek")) return true;
        try {
            int delay = Integer.parseInt(parameters);
            return delay >= 0 && delay <= 60;
        } catch (NumberFormatException ex) { return false; }
    }
}
