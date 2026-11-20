package dev.trueaura.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import dev.trueaura.aura.AuraSettings;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public final class AuraRenderer {
    // Own reusable buffer: do not flush or disturb Minecraft's shared entity buffers.
    private static final MultiBufferSource.BufferSource BUFFERS =
        MultiBufferSource.immediate(new BufferBuilder(384 * 1024));

    public static void render(RenderLevelStageEvent event, AuraState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        var candidates = new ArrayList<>(mc.level.players());
        candidates.sort(Comparator.comparingDouble(player -> player.distanceToSqr(camera)));
        var selected = new ArrayList<DrawRequest>();
        for (AbstractClientPlayer player : candidates) {
            boolean own = player == mc.player;
            if (own) {
                if (mc.getCameraEntity() != player || mc.options.getCameraType().isFirstPerson()) continue;
            }
            AuraClient.Visual remote = own ? null : AuraClient.remote(player.getUUID());
            if (!own && remote == null) continue;
            AuraState animation = own ? state : remote.state();
            AuraSettings settings = own ? AuraClient.renderSettings() : remote.settings();
            float opacity = own && AuraClient.isPreviewing() ? AuraClient.previewOpacity(event.getPartialTick()) : animation.opacity(event.getPartialTick());
            if (opacity < 0.001f || (!(own && AuraClient.isPreviewing()) && player.distanceToSqr(camera) > 64 * 64)
                || !event.getFrustum().isVisible(player.getBoundingBox().inflate(settings.scale() * (settings.custom() == null ? 4 : 10)))) continue;
            if (!eligible(player)) continue;
            selected.add(new DrawRequest(player, animation, settings, opacity));
            if (selected.size() >= 32) break;
        }
        // Choose the nearest bounded set, then composite far-to-near across players.
        // Fabulous writes aura depth; drawing near first would erase farther translucent layers.
        Collections.reverse(selected);
        for (DrawRequest request : selected) {
            draw(event, request.player(), request.animation(), request.settings(), request.opacity());
        }
    }

    private record DrawRequest(AbstractClientPlayer player, AuraState animation, AuraSettings settings, float opacity) { }

    private static boolean eligible(AbstractClientPlayer player) {
        return !player.isSpectator() && !player.isInvisible() && player.isAlive()
            && !player.isSwimming() && !player.isVisuallySwimming() && !player.isFallFlying()
            && !player.isSleeping() && !player.isPassenger();
    }

    private static boolean draw(RenderLevelStageEvent event, AbstractClientPlayer player, AuraState state,
                                AuraSettings settings, float opacity) {
        float partial = event.getPartialTick();
        // All temporal frequencies in the mesh and shader are multiples of 0.1.
        // Reduce after speed scaling at their common period: no phase snap or float drift.
        float time = (float) ((state.time(partial) / 20.0 * settings.speed()) % (20 * Math.PI));
        float scale = (float) settings.scale();
        Vec3 camera = event.getCamera().getPosition();
        double x = Mth.lerp(partial, player.xOld, player.getX());
        double y = Mth.lerp(partial, player.yOld, player.getY());
        double z = Mth.lerp(partial, player.zOld, player.getZ());
        float angle = (float) Math.atan2(camera.z - z, camera.x - x);
        boolean fabulous = Minecraft.useShaderTransparency() && !AuraShaderCompatibility.active();
        if (!AuraRenderType.prepare(time, settings, angle, fabulous)) return false;
        RenderType renderType = fabulous ? AuraRenderType.FABULOUS_FLAMES : AuraRenderType.FLAMES;

        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        try {
            poses.translate(x - camera.x, y - camera.y + 0.015, z - camera.z);
            poses.scale(scale, scale, scale);
            Matrix4f matrix = poses.last().pose();
            VertexConsumer vertices = BUFFERS.getBuffer(renderType);
            boolean rising = settings.animation().riseFromFeet();
            int alpha = Math.round((rising ? Math.min(1, opacity * 6) : opacity) * 255);
            AuraMesh.VertexSink sink = (vx, vy, vz, u, v, layer) ->
                vertices.vertex(matrix, vx, vy, vz)
                    .color(Math.round(layer * 127.5f), 255, 255, alpha)
                    .uv(u, v).endVertex();
            double height = settings.custom() == null ? player.getBbHeight() + 1.4 : settings.custom().maxHeight(player.getBbHeight());
            sink = state.motion().sample(partial).wrap(sink, height, state.time(partial) / 20, settings.animation().motionStrength());
            if (rising) sink = AuraReveal.wrap(sink, opacity, state.time(partial) / 20);
            if (settings.custom() == null) AuraMesh.emit(time, player.getBbHeight(), settings.shape(), player.distanceToSqr(camera) > 16 * 16, sink);
            else CustomAuraMesh.emit(state.time(partial) / 20 * settings.speed(), player.getBbHeight(), settings.custom(), player.distanceToSqr(camera) > 16 * 16, sink);
            BUFFERS.endBatch(renderType);
        } finally {
            poses.popPose();
        }
        return true;
    }
    private AuraRenderer() { }
}
