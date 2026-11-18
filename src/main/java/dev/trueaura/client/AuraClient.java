package dev.trueaura.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.trueaura.TrueAura;
import dev.trueaura.aura.AuraSettings;
import dev.trueaura.network.AuraNetwork;
import dev.trueaura.network.AuraSnapshot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraft.util.Mth;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TrueAura.MOD_ID, value = Dist.CLIENT)
public final class AuraClient {
    private static final AuraState STATE = new AuraState();
    private static final Map<UUID, Visual> REMOTE = new HashMap<>();
    private static AuraSettings draft;
    private static boolean needsSync = true;
    private static final org.joml.Matrix4f shaderView = new org.joml.Matrix4f();
    private static final org.joml.Matrix3f shaderNormal = new org.joml.Matrix3f();
    private static boolean capturedShaderView;
    public record Visual(AuraSettings settings, AuraState state) { }
    private static final KeyMapping TOGGLE = new KeyMapping("key.trueaura.toggle",
        KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.trueaura");
    private static final KeyMapping SETTINGS = new KeyMapping("key.trueaura.settings",
        KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.trueaura");

    public static void initialize() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AuraConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new AuraSettingsScreen(parent)));
        AuraNetwork.setClientReceiver(AuraClient::receive);
        AuraNetwork.setCommandReceiver(AuraClient::receiveCommand);
    }

    public static dev.trueaura.aura.AuraLimits limits() {
        return isShared() ? dev.trueaura.config.AuraServerConfig.limits() : AuraConfig.localLimits();
    }
    public static AuraSettings settings() { return limits().apply(AuraConfig.settings()); }
    public static void preview(AuraSettings settings) { draft = settings; }
    public static void clearPreview() { draft = null; }
    public static void saveSettings(AuraSettings settings) {
        AuraConfig.save(limits().apply(settings));
        needsSync = true;
    }
    public static boolean isAuraEnabled() { return STATE.enabled(); }
    public static boolean isShared() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && AuraNetwork.canSync(connection.getConnection());
    }
    public static boolean isPreviewing() { return draft != null; }
    public static AuraSettings renderSettings() { return draft != null ? draft : settings(); }
    public static Visual remote(UUID id) { return REMOTE.get(id); }

    private static void receiveCommand(AuraSnapshot snapshot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        boolean changed = STATE.enabled() != snapshot.enabled();
        AuraConfig.save(snapshot.settings());
        STATE.setEnabled(snapshot.enabled());
        needsSync = false;
        if (mc.screen instanceof AuraSettingsScreen screen) screen.applyServerSettings(snapshot.settings());
        if (changed) AuraBurstEffects.start(mc.player, snapshot.settings(), snapshot.enabled());
        mc.player.displayClientMessage(Component.translatable("message.trueaura.command_updated"), false);
    }

    private static void receive(UUID id, AuraSnapshot snapshot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || id.equals(mc.player.getUUID())) return;
        if (snapshot == null) {
            REMOTE.remove(id);
            AuraBurstEffects.remove(id);
            return;
        }
        Visual prior = REMOTE.get(id);
        if (AuraBurst.shouldStart(prior == null ? null : prior.state().enabled(), snapshot.enabled())) {
            AuraBurstEffects.start(mc.level.getPlayerByUUID(id), snapshot.settings(), snapshot.enabled());
        }
        AuraState state = prior == null ? new AuraState() : prior.state();
        state.setEnabled(snapshot.enabled());
        REMOTE.put(id, new Visual(snapshot.settings(), state));
    }

    private static void reset() {
        STATE.reset();
        REMOTE.clear();
        AuraBurstEffects.clear();
        draft = null;
        needsSync = true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            reset();
            while (TOGGLE.consumeClick()) { /* discard input from old worlds */ }
            while (SETTINGS.consumeClick()) { }
            return;
        }
        while (SETTINGS.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new AuraSettingsScreen(null));
        }
        while (TOGGLE.consumeClick()) {
            if (mc.screen == null) {
                STATE.toggle();
                AuraBurstEffects.start(mc.player, settings(), STATE.enabled());
                needsSync = true;
                mc.player.displayClientMessage(Component.translatable(
                    STATE.enabled() ? "message.trueaura.on" : "message.trueaura.off"), true);
            }
        }
        if (!mc.isPaused()) {
            STATE.tick(true);
            AuraBurstEffects.tick();
            for (Visual visual : REMOTE.values()) visual.state().tick(true);
        }
        if (needsSync && isShared()) {
            AuraNetwork.sendToServer(settings(), STATE.enabled());
            needsSync = false;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); AuraNetwork.resetClientConnection(); }

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn event) { reset(); AuraNetwork.resetClientConnection(); }

    @SubscribeEvent
    public static void respawn(ClientPlayerNetworkEvent.Clone event) { reset(); }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) reset();
    }

    @SubscribeEvent
    public static void previewFov(ViewportEvent.ComputeFov event) {
        if (Minecraft.getInstance().screen instanceof AuraSettingsScreen screen && event.usedConfiguredFov()) {
            event.setFOV(screen.previewFov());
        }
    }

    @SubscribeEvent
    public static void previewCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AuraSettingsScreen screen) || mc.player == null || mc.level == null) return;
        // Stable front orbit ignores look pitch, including straight up/down. Move only the
        // rendering camera; player position/rotation and gameplay packets remain untouched.
        double partial = event.getPartialTick();
        var orbit = screen.previewOrbit(Mth.rotLerp((float) partial, mc.player.yBodyRotO, mc.player.yBodyRot));
        var center = new net.minecraft.world.phys.Vec3(Mth.lerp(partial, mc.player.xOld, mc.player.getX()),
            Mth.lerp(partial, mc.player.yOld, mc.player.getY()) + screen.previewCenterHeight(mc.player.getBbHeight()),
            Mth.lerp(partial, mc.player.zOld, mc.player.getZ()));
        var desired = center.add(orbit.x(), 0, orbit.z());
        var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(center, desired,
            net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            double fraction = Math.max(.01, hit.getLocation().distanceTo(center) - .15) / desired.distanceTo(center);
            desired = center.add(desired.subtract(center).scale(fraction));
        }
        event.getCamera().setPosition(desired.x, desired.y, desired.z);
        event.setYaw(orbit.yaw());
        event.setPitch(0);
        event.setRoll(0);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (AuraShaderCompatibility.shadowPass()) return;
        if (AuraShaderCompatibility.active()) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                shaderView.set(event.getPoseStack().last().pose());
                shaderNormal.set(event.getPoseStack().last().normal());
                capturedShaderView = true;
            }
            // Oculus composites its world framebuffer at the end of LevelRenderer.
            // Forge 1.20.1 supplies a projection pose at AFTER_LEVEL, so restore the
            // world view captured above instead of applying the projection twice.
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && capturedShaderView) {
                capturedShaderView = false;
                var poses = event.getPoseStack();
                poses.pushPose();
                try {
                    poses.last().pose().set(shaderView);
                    poses.last().normal().set(shaderNormal);
                    AuraBurstEffects.render(event);
                    AuraRenderer.render(event, STATE);
                } finally {
                    poses.popPose();
                }
            }
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            AuraBurstEffects.render(event);
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            AuraRenderer.render(event, STATE);
        }
    }

    @Mod.EventBusSubscriber(modid = TrueAura.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
            event.register(SETTINGS);
        }
        @SubscribeEvent
        public static void shaders(RegisterShadersEvent event) throws IOException {
            AuraRenderType.registerShader(event);
        }
    }
    private AuraClient() { }
}
