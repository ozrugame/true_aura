package dev.trueaura.client;

import dev.trueaura.particle.AuraParticleCatalog;

import dev.trueaura.aura.AuraEffect;
import dev.trueaura.aura.AuraSettings;
import dev.trueaura.aura.EpicParticleArguments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Calls particle providers only; never invokes mod attacks, commands or world-editing APIs. */
public final class AuraParticleEffects {
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static long heavyNext;
    private static int budget = 80;
    public static void clear() { NEXT.clear(); heavyNext = 0; budget = 80; }
    public static void tick() { budget = 80; }

    /** Empty means handled; a problem key asks the caller to use the original cosmetic burst. */
    public static String play(Player player, AuraSettings settings, boolean opening) {
        AuraEffect effect = settings.effect(opening);
        String problem = AuraParticleCatalog.problem(effect);
        if (!problem.isEmpty()) return problem;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return "world_required";
        long now = mc.level.getGameTime();
        NEXT.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (now < NEXT.getOrDefault(player.getUUID(), 0L) || budget <= 0) return "cooldown";
        NEXT.put(player.getUUID(), now + 8);
        try {
            ParticleOptions options = effect.id().equals("minecraft:vibration") && effect.parameters().equals("@self")
                ? new VibrationParticleOption(new BlockPositionSource(BlockPos.containing(player.position().add(0, 1, 0))), 12)
                : AuraParticleCatalog.parse(effect);
            String id = effect.id();
            if (id.startsWith("epicfight:")) {
                var args = EpicParticleArguments.forEffect(id.substring(10), settings.impactScale(), player.getId(),
                    player.getXRot(), player.getYRot(), opening);
                if (args != null) {
                    if (now < heavyNext) return "cooldown";
                    if (args.ground() && !player.onGround()) return "ground_required";
                    heavyNext = now + 10;
                    return spawn(options, player.getX(), player.getY() + args.height(), player.getZ(),
                        args.a(), args.b(), args.c()) ? "" : "provider_unavailable";
                }
            }
            // Unknown mod emitters are created once; known ambient particles form a bounded burst.
            boolean ambient = id.startsWith("minecraft:") && !id.contains("emitter") && !id.contains("explosion")
                || id.startsWith("epicfight:");
            int count = ambient ? 16 : 1;
            boolean created = false;
            for (int i = 0; i < count && budget > 0; i++) {
                double angle = i * Math.PI * 2 / count;
                double radius = (.45 + mc.level.random.nextDouble() * .35) * settings.impactScale();
                double dx = Math.cos(angle), dz = Math.sin(angle);
                double direction = opening ? .15 : -.12;
                created |= spawn(options, player.getX() + dx * radius, player.getY() + .2 + mc.level.random.nextDouble() * 1.4,
                    player.getZ() + dz * radius, dx * direction, .06, dz * direction);
            }
            return created ? "" : "provider_unavailable";
        } catch (Exception ex) {
            return "provider_unavailable";
        }
    }

    private static boolean spawn(ParticleOptions options, double x, double y, double z, double a, double b, double c) {
        if (budget-- <= 0) return false;
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(options, x, y, z, a, b, c);
        if (particle == null) return false;
        particle.setLifetime(Math.max(1, Math.min(60, particle.getLifetime())));
        return true;
    }
    private AuraParticleEffects() { }
}
