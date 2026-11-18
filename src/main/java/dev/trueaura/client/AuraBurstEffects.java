package dev.trueaura.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.trueaura.aura.AuraSettings;
import dev.trueaura.aura.AuraEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/** Entirely client-side visuals: no world mutations, explosions, damage or extra network traffic. */
public final class AuraBurstEffects {
    private record Crack(Vec3 from, Vec3 to) { }
    private record Tile(BlockPos pos, BlockState state, double top, double distance, double angle) { }
    private record Effect(Vec3 origin, AuraSettings settings, AuraBurst animation, List<Crack> cracks, List<Tile> tiles) { }
    private static final Map<UUID, Effect> ACTIVE = new LinkedHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFERS =
        MultiBufferSource.immediate(new BufferBuilder(96 * 1024));
    private static final MultiBufferSource.BufferSource TILE_BUFFERS =
        MultiBufferSource.immediate(new BufferBuilder(128 * 1024));
    private static final Map<UUID, Long> NEXT = new LinkedHashMap<>();
    private static int particleBudget = 80;

    public static void clear() { ACTIVE.clear(); NEXT.clear(); particleBudget = 80; AuraParticleEffects.clear(); }
    public static void remove(UUID id) { ACTIVE.remove(id); NEXT.remove(id); }
    public static void tick() {
        AuraParticleEffects.tick();
        ACTIVE.values().forEach(effect -> effect.animation().tick());
        ACTIVE.values().removeIf(effect -> effect.animation().expired());
        particleBudget = 80;
    }

    public static String start(Player player, AuraSettings settings, boolean opening) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || player == null || player.isSpectator()
            || player.isInvisible() || !player.isAlive()
            || player.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > 32 * 32) return "not_visible";
        AuraEffect selected = settings.effect(opening);
        if (selected.equals(AuraEffect.NONE)) { ACTIVE.remove(player.getUUID()); return ""; }
        long now = mc.level.getGameTime();
        NEXT.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (now < NEXT.getOrDefault(player.getUUID(), 0L)) return "cooldown";
        String fallback = "";
        boolean nativeSlam = false;
        if (!selected.equals(AuraEffect.BURST)) {
            fallback = AuraParticleEffects.play(player, settings, opening);
            // The native emitter has a global budget. Its cooldown must not suppress
            // another player's lightweight fracture animation in multiplayer.
            nativeSlam = selected.id().equals("epicfight:ground_slam")
                && (fallback.isEmpty() || fallback.equals("cooldown"));
            if (nativeSlam) fallback = "";
            if (!nativeSlam && (fallback.isEmpty() || fallback.equals("cooldown") || fallback.equals("ground_required"))) return fallback;
        }
        // Native particles such as bubbles need water. This restriction belongs only to
        // our ground-flare fallback, not to the installed particle providers above.
        if (player.isInWaterOrBubble() || player.isInLava()) return fallback.isEmpty() ? "not_visible" : fallback;
        NEXT.put(player.getUUID(), now + 8);
        Vec3 origin = player.position();
        AuraBurst animation = new AuraBurst(opening);
        List<Crack> cracks = new ArrayList<>();
        if (player.onGround()) {
            // Raycast only at trigger time, never every render frame. Each short strip sits on
            // real collision surfaces, so slabs work and holes/cliffs don't get bridged.
            for (int ray = 0; ray < 14; ray++) {
                double angle = ray * Math.PI / 7 + mc.level.random.nextDouble() * .22;
                Vec3 previous = origin;
                for (int segment = 1; segment <= 6; segment++) {
                    double radius = segment * animation.maxRadius() / 6 * settings.impactScale();
                    double bend = angle + (mc.level.random.nextDouble() - .5) * .22;
                    Vec3 next = origin.add(Math.cos(bend) * radius, 0, Math.sin(bend) * radius);
                    addCrack(mc.level, player, previous, next, cracks);
                    if (segment == 3) {
                        Vec3 branch = next.add(Math.cos(bend + .7) * .48 * settings.impactScale(), 0,
                            Math.sin(bend + .7) * .48 * settings.impactScale());
                        addCrack(mc.level, player, next, branch, cracks);
                    }
                    previous = next;
                }
            }
        }
        ACTIVE.remove(player.getUUID());
        while (ACTIVE.size() >= 32) ACTIVE.remove(ACTIVE.keySet().iterator().next());
        ACTIVE.put(player.getUUID(), new Effect(origin, settings, animation, List.copyOf(cracks), tiles(mc.level, player, origin, animation.maxRadius() * settings.impactScale())));
        if (!nativeSlam) particles(mc.level, player, origin, settings, opening);
        if (player.onGround()) mc.level.playLocalSound(origin.x, origin.y, origin.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, opening ? .7f : .45f, opening ? .75f : 1.05f, false);
        return fallback;
    }

    private static List<Tile> tiles(ClientLevel level, Player player, Vec3 origin, double radius) {
        if (!player.onGround()) return List.of();
        List<Tile> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        // Three concentric bands, max 36 tiles. Sample at trigger time only.
        for (int band = 0; band < 3; band++) {
            for (int i = 0; i < 12; i++) {
                double angle = (i + band * .43) * Math.PI / 6;
                double r = radius * (.24 + band * .29);
                BlockPos sample = BlockPos.containing(origin.add(Math.cos(angle) * r, -.1, Math.sin(angle) * r));
                Vec3 surface = ground(level, player, new Vec3(sample.getX() + .5, origin.y, sample.getZ() + .5));
                if (surface == null) continue;
                BlockPos pos = BlockPos.containing(surface.add(0, -.04, 0));
                BlockState state = level.getBlockState(pos);
                // Solid ordinary cubes only: no chests, fluids, fragile plants or custom block entities.
                if (!visited.add(pos) || state.hasBlockEntity() || !state.isCollisionShapeFullBlock(level, pos)
                    || state.getRenderShape() != RenderShape.MODEL || !state.isSolidRender(level, pos)) continue;
                double dx = pos.getX() + .5 - origin.x, dz = pos.getZ() + .5 - origin.z;
                result.add(new Tile(pos.immutable(), state, surface.y - .018,
                    Math.min(1, Math.sqrt(dx * dx + dz * dz) / radius), Math.atan2(dz, dx)));
            }
        }
        return List.copyOf(result);
    }

    private static Vec3 ground(ClientLevel level, Player player, Vec3 point) {
        var hit = level.clip(new ClipContext(point.add(0, .6, 0), point.add(0, -1.1, 0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return null;
        var state = level.getBlockState(hit.getBlockPos());
        if (!state.getFluidState().isEmpty() || state.getRenderShape() == RenderShape.INVISIBLE) return null;
        return hit.getLocation().add(0, .018, 0);
    }

    private static void addCrack(ClientLevel level, Player player, Vec3 from, Vec3 to, List<Crack> cracks) {
        Vec3 a = ground(level, player, from), b = ground(level, player, to);
        Vec3 middle = ground(level, player, from.add(to).scale(.5));
        if (a != null && b != null && middle != null && Math.abs(a.y - b.y) < .15
            && Math.abs(middle.y - a.y) < .15) cracks.add(new Crack(a, b));
    }

    private static void particles(ClientLevel level, Player player, Vec3 origin, AuraSettings settings, boolean opening) {
        int color = opening ? settings.highlightColor() : settings.baseColor();
        var dust = new DustParticleOptions(new Vector3f(((color >> 16) & 255) / 255f,
            ((color >> 8) & 255) / 255f, (color & 255) / 255f), opening ? 1.1f : .8f);
        for (int i = 0; i < (opening ? 44 : 28) && particleBudget > 0; i++, particleBudget--) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = (.35 + level.random.nextDouble() * 1.4) * settings.impactScale();
            double x = Math.cos(angle), z = Math.sin(angle);
            level.addParticle(dust, origin.x + x * radius, origin.y + level.random.nextDouble() * 1.8,
                origin.z + z * radius, x * (opening ? .15 : -.1), .08, z * (opening ? .15 : -.1));
            if (!player.onGround() || i % 2 != 0 || particleBudget < 2) continue;
            Vec3 surface = ground(level, player, origin.add(x * radius, 0, z * radius));
            if (surface == null) continue;
            var pos = net.minecraft.core.BlockPos.containing(surface.add(0, -.03, 0));
            var state = level.getBlockState(pos);
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(pos),
                surface.x, surface.y + .05, surface.z, x * (opening ? .38 : -.24), .25 + level.random.nextDouble() * .35, z * (opening ? .38 : -.24));
            particleBudget--;
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!AuraRenderType.burstReady()) return;
        renderTiles(event);
        var type = AuraRenderType.BURSTS;
        VertexConsumer vertices = BUFFERS.getBuffer(type);
        Matrix4f matrix = event.getPoseStack().last().pose();
        Vec3 camera = event.getCamera().getPosition();
        for (var entry : ACTIVE.entrySet()) {
            Effect effect = entry.getValue();
            Player owner = mc.level.getPlayerByUUID(entry.getKey());
            if (owner == null || owner.isInvisible() || owner.isSpectator() || !owner.isAlive()
                || effect.origin().distanceToSqr(camera) > 40 * 40) continue;
            float partial = event.getPartialTick();
            float alpha = effect.animation().opacity(partial);
            double radius = effect.animation().radius(partial) * effect.settings().impactScale();
            int color = effect.animation().opening() ? effect.settings().mainColor() : effect.settings().baseColor();
            float glow = Math.min(1, alpha * (float) effect.settings().intensity());
            float wave = effect.animation().waveOpacity(partial) * Math.min(1, (float) effect.settings().intensity());
            ring(vertices, matrix, camera, effect.origin(), radius, .14 * effect.settings().impactScale(), color, wave);
            double echo = effect.animation().echoRadius(partial) * effect.settings().impactScale();
            if (echo > .05) ring(vertices, matrix, camera, effect.origin().add(0, .025, 0), echo,
                .07 * effect.settings().impactScale(), effect.settings().highlightColor(), wave * .75f);
            for (Crack crack : effect.cracks()) {
                if (effect.animation().opening() && crack.from().distanceToSqr(effect.origin()) > radius * radius) continue;
                strip(vertices, matrix, camera, crack.from(), crack.to(), .095 * effect.settings().impactScale(), 0x17101B, alpha * .9f);
                strip(vertices, matrix, camera, crack.from().add(0, .002, 0), crack.to().add(0, .002, 0),
                    .026 * effect.settings().impactScale(), color, glow);
            }
        }
        BUFFERS.endBatch(type);
    }

    private static void renderTiles(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(false);
        var pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        int remaining = 192;
        for (var entry : ACTIVE.entrySet()) {
            Effect effect = entry.getValue();
            Player owner = mc.level.getPlayerByUUID(entry.getKey());
            if (owner == null || owner.isInvisible() || owner.isSpectator() || !owner.isAlive()
                || effect.origin().distanceToSqr(camera) > 40 * 40) continue;
            for (Tile tile : effect.tiles()) {
                if (remaining <= 0) break;
                // Never display a stale copy after mining, replacing or updating a block.
                if (mc.level.getBlockState(tile.pos()) != tile.state()) continue;
                double lift = effect.animation().tileLift(tile.distance(), event.getPartialTick());
                double tilt = effect.animation().tileTilt(tile.distance(), event.getPartialTick());
                if (lift < .001 && Math.abs(tilt) < .01) continue;
                remaining--;
                pose.pushPose();
                pose.translate(tile.pos().getX() + .5 - camera.x, tile.top() - .12 + lift - camera.y,
                    tile.pos().getZ() + .5 - camera.z);
                pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(tilt),
                    (float) -Math.sin(tile.angle()), 0, (float) Math.cos(tile.angle())));
                pose.scale(.94f, .24f, .94f);
                pose.translate(-.5, -.5, -.5);
                mc.getBlockRenderer().renderSingleBlock(tile.state(), pose, TILE_BUFFERS,
                    LevelRenderer.getLightColor(mc.level, tile.pos().above()), OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        TILE_BUFFERS.endBatch();
    }

    private static void ring(VertexConsumer vertices, Matrix4f matrix, Vec3 camera, Vec3 origin,
                             double radius, double width, int color, float alpha) {
        for (int i = 0; i < 64; i++) {
            double a = i * Math.PI / 32, b = (i + 1) * Math.PI / 32;
            strip(vertices, matrix, camera, origin.add(Math.cos(a) * radius, .07, Math.sin(a) * radius),
                origin.add(Math.cos(b) * radius, .07, Math.sin(b) * radius), width, color, alpha);
        }
    }

    private static void strip(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 a, Vec3 b,
                              double width, int color, float opacity) {
        double dx = b.x - a.x, dz = b.z - a.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < .0001) return;
        double px = -dz / length * width, pz = dx / length * width;
        vertex(out, matrix, a.add(px, 0, pz).subtract(camera), color, opacity);
        vertex(out, matrix, b.add(px, 0, pz).subtract(camera), color, opacity);
        vertex(out, matrix, b.add(-px, 0, -pz).subtract(camera), color, opacity);
        vertex(out, matrix, a.add(-px, 0, -pz).subtract(camera), color, opacity);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 p, int color, float opacity) {
        out.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
            .color((color >> 16) & 255, (color >> 8) & 255, color & 255, Math.round(opacity * 255)).endVertex();
    }
    private AuraBurstEffects() { }
}
