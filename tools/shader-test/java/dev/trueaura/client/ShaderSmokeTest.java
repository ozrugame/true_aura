package dev.trueaura.client;

import com.mojang.logging.LogUtils;
import dev.trueaura.TrueAura;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated development source set; never included in the release JAR. */
@Mod.EventBusSubscriber(modid = TrueAura.MOD_ID, value = Dist.CLIENT)
public final class ShaderSmokeTest {
    private static final String[] PACKS = {"ComplementaryReimagined_r5.9.3.zip", "ComplementaryUnbound_r5.9.3.zip", "BSL_v10.1.8.zip", null};
    private static int ticks, pack;
    private static boolean finished;
    private static boolean occlusionCamera;
    private static String shot;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || finished || mc.level == null || mc.player == null) return;
        if (mc.getSingleplayerServer() == null || !mc.level.dimension().location().toString().equals("minecraft:overworld")) return;
        ticks++;
        try {
            if (ticks == 20) {
                mc.options.pauseOnLostFocus = false;
                mc.setScreen(null);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.hideGui = true;
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    var commands = server.getCommands(); var source = server.createCommandSourceStack();
                    commands.performPrefixedCommand(source, "tp AuraShaderTest 0 -60 0 0 0");
                    commands.performPrefixedCommand(source, "gamerule doDaylightCycle false");
                    commands.performPrefixedCommand(source, "time set 6000");
                    commands.performPrefixedCommand(source, "weather clear");
                    commands.performPrefixedCommand(source, "fill -4 -60 2 4 -54 2 air");
                });
            }
            if (ticks == 60) {
                setPack(PACKS[pack]);
                command("aura set size 1.4");
                command("aura set intensity 2");
                command("aura set shape flame");
                command("aura set open_effect trueaura:burst");
                command("aura set close_effect trueaura:burst");
                command("aura off");
            }
            if (ticks == 160) shot = pack + "-off.png";
            if (ticks == 180) command("aura on");
            if (ticks == 184) shot = pack + "-impact.png";
            if (ticks == 250) shot = pack + "-on.png";
            if (ticks == 280) {
                command("aura set size 4"); command("aura set intensity 5");
                command("aura set size 7"); command("aura set intensity 8");
            }
            if (ticks == 300) mc.setScreen(new AuraSettingsScreen(null));
            if (ticks == 330) shot = pack + "-large-preview.png";
            if (ticks == 360) {
                mc.setScreen(null);
                command("aura set size 1.4"); command("aura set intensity 2");
                worldCommand("fill -4 -60 2 4 -54 2 stone");
                occlusionCamera = true;
            }
            if (ticks == 410) shot = pack + "-occluded.png";
            if (ticks == 430) {
                worldCommand("fill -4 -60 2 4 -54 2 air");
                occlusionCamera = false;
            }
            if (ticks == 460) command("aura off");
            if (ticks == 464) shot = pack + "-close-impact.png";
            if (ticks == 520) shot = pack + "-closed.png";
            if (ticks == 540) {
                if (++pack < PACKS.length) ticks = 30;
                else {
                    finished = true;
                    mc.options.hideGui = false;
                    LogUtils.getLogger().info("AURA_SHADER_TEST COMPLETE: three packs and shaders disabled captured; client remains open");
                }
            }
        } catch (Throwable ex) {
            finished = true;
            mc.options.hideGui = false;
            LogUtils.getLogger().error("AURA_SHADER_TEST FAILED", ex);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (!occlusionCamera) return;
        // Fixed development-only camera beyond the wall; bypass normal third-person collision.
        event.getCamera().setPosition(0, -58.38, 6);
        event.setYaw(180); event.setPitch(0); event.setRoll(0);
    }
    private static void worldCommand(String command) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || shot == null) return;
        String name = "aura-shader-" + shot; shot = null;
        LogUtils.getLogger().info("AURA_SHADER_TEST state {} on={} size={} intensity={} limits={} shader={}",
            name, AuraClient.isAuraEnabled(), AuraClient.renderSettings().scale(), AuraClient.renderSettings().intensity(),
            AuraClient.limits(), AuraShaderCompatibility.active());
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message ->
            LogUtils.getLogger().info("AURA_SHADER_TEST screenshot {}: {}", name, message.getString()));
    }
    private static void command(String command) { Minecraft.getInstance().getConnection().sendCommand(command); }
    private static void setPack(String name) throws Exception {
        Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
        Object config = iris.getMethod("getIrisConfig").invoke(null);
        if (name != null) config.getClass().getMethod("setShaderPackName", String.class).invoke(config, name);
        config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, name != null);
        config.getClass().getMethod("save").invoke(config);
        iris.getMethod("reload").invoke(null);
        LogUtils.getLogger().info("AURA_SHADER_TEST pack {} fallback={}", name, iris.getMethod("isFallback").invoke(null));
    }
}
