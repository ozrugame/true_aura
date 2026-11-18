package dev.trueaura.particle;

import com.mojang.brigadier.StringReader;
import dev.trueaura.aura.AuraEffect;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Discovers installed particles, including parameterized types, without instantiating providers. */
public final class AuraParticleCatalog {
    private static final Set<String> EPIC_SUPPORTED = Set.of("ash_directional", "blood", "catharsis", "cut",
        "dust_expansive", "dust_contractive", "dust_normal", "enderman_death_emit", "ground_slam",
        "breath_flame", "force_field", "force_field_end", "adrenaline_player_beating", "white_afterimage",
        "neutralize", "boss_casting", "tsunami_splash", "feather", "air_burst", "hit_blunt", "hit_blade", "eviscerate", "blade_rush");

    public static List<String> entries(String query) {
        List<String> all = new ArrayList<>();
        all.add(AuraEffect.NONE.id());
        all.add(AuraEffect.BURST.id());
        ForgeRegistries.PARTICLE_TYPES.getKeys().forEach(id -> all.add(id.toString()));
        String needle = query.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return all.stream().distinct().filter(id -> id.contains(needle))
            .sorted(Comparator.comparingInt(AuraParticleCatalog::priority).thenComparing(id -> id)).toList();
    }
    private static int priority(String id) {
        return id.startsWith("trueaura:") ? 0 : id.startsWith("epicfight:") ? 1 : id.startsWith("minecraft:") ? 3 : 2;
    }
    public static String defaultParameters(String id) {
        return switch (id) {
            case "minecraft:block", "minecraft:block_marker", "minecraft:falling_dust" -> "minecraft:stone";
            case "minecraft:item" -> "minecraft:diamond";
            case "minecraft:dust" -> "1 0.7 0.1 1";
            case "minecraft:dust_color_transition" -> "1 0.7 0.1 1 0.7 0.4 1";
            case "minecraft:sculk_charge", "minecraft:shriek" -> "0";
            // Resolved at playback time to the emitting player's position on each viewer.
            case "minecraft:vibration" -> "@self";
            default -> "";
        };
    }
    public static String restriction(String id) {
        if (id.equals("epicfight:swing_trail") || id.equals("epicfight:projectile_trail")) return "context_required";
        if (id.equals("epicfight:laser")) return "laser_context";
        if (id.startsWith("epicfight:") && !EPIC_SUPPORTED.contains(id.substring(10))) return "context_required";
        return "";
    }
    public static boolean installed(AuraEffect effect) {
        return effect.builtin() || ForgeRegistries.PARTICLE_TYPES.containsKey(new ResourceLocation(effect.id()));
    }
    public static String problem(AuraEffect effect) {
        if (effect.builtin()) return "";
        if (!installed(effect)) return "missing_mod";
        String restricted = restriction(effect.id());
        if (!restricted.isEmpty()) return restricted;
        if (!effect.timingAllowed()) return "invalid_parameters";
        if (effect.id().equals("minecraft:vibration") && effect.parameters().equals("@self")) return "";
        try { parse(effect); return ""; }
        catch (Exception ex) { return "invalid_parameters"; }
    }
    public static ParticleOptions parse(AuraEffect effect) throws Exception {
        StringReader reader = new StringReader(effect.id() + (effect.parameters().isEmpty() ? "" : " " + effect.parameters()));
        ParticleOptions options = ParticleArgument.readParticle(reader, BuiltInRegistries.PARTICLE_TYPE.asLookup());
        reader.skipWhitespace();
        if (reader.canRead()) throw new IllegalArgumentException("Unexpected particle arguments");
        return options;
    }
    private AuraParticleCatalog() { }
}
