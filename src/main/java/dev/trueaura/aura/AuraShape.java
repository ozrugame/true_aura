package dev.trueaura.aura;

public enum AuraShape {
    // Append only: these IDs are saved and sent over the network.
    FLAME, FLOW, SPIKES, VORTEX, LIGHTNING, PULSE, INFERNO, LOTUS, BLADE, VEIL;
    public static AuraShape fromId(int id) {
        return id >= 0 && id < values().length ? values()[id] : FLAME;
    }
}
