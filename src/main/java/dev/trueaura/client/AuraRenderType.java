package dev.trueaura.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.trueaura.TrueAura;
import dev.trueaura.aura.AuraSettings;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import java.io.IOException;

public final class AuraRenderType extends RenderType {
    private static ShaderInstance shader;
    private static ShaderInstance fabulousShader;
    private static ShaderInstance burstShader;
    public static final RenderType BURSTS = burstType();

    private static RenderType burstType() {
        return create("trueaura_bursts",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 96 * 1024, false, true,
            CompositeState.builder()
                .setShaderState(new ShaderStateShard(() -> burstShader))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                // Ground decals precede translucent rendering, on the main target. Keeping
                // terrain depth prevents a fading ring from becoming an opaque depth plane.
                .setOutputState(new OutputStateShard("trueaura_ground_target",
                    () -> net.minecraft.client.Minecraft.getInstance().getMainRenderTarget().bindWrite(false), () -> { }))
                .createCompositeState(false));
    }

    public static boolean burstReady() { return burstShader != null; }
    public static final RenderType FLAMES = create("trueaura_flames",
        DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 384 * 1024, false, false,
        CompositeState.builder()
            .setShaderState(new ShaderStateShard(() -> shader))
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setOutputState(new OutputStateShard("trueaura_shader_aware_target", () -> {
                if (AuraShaderCompatibility.active()) net.minecraft.client.Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                else PARTICLES_TARGET.setupRenderState();
            }, () -> { if (!AuraShaderCompatibility.active()) PARTICLES_TARGET.clearRenderState(); }))
            .createCompositeState(false));

    // Fabulous composites a premultiplied color target using its own depth texture.
    // Sorted quads write surface depth so water/glass can be ordered against the aura.
    public static final RenderType FABULOUS_FLAMES = create("trueaura_fabulous_flames",
        DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 384 * 1024, false, true,
        CompositeState.builder()
            .setShaderState(new ShaderStateShard(() -> fabulousShader))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setCullState(NO_CULL)
            .setOutputState(PARTICLES_TARGET)
            .createCompositeState(false));

    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            new ResourceLocation(TrueAura.MOD_ID, "burst"), DefaultVertexFormat.POSITION_COLOR),
            loaded -> burstShader = loaded);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            new ResourceLocation(TrueAura.MOD_ID, "aura"), DefaultVertexFormat.POSITION_COLOR_TEX),
            loaded -> shader = loaded);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            new ResourceLocation(TrueAura.MOD_ID, "aura_fabulous"), DefaultVertexFormat.POSITION_COLOR_TEX),
            loaded -> fabulousShader = loaded);
    }

    public static boolean prepare(float time, AuraSettings settings, float viewAngle, boolean fabulous) {
        ShaderInstance active = fabulous ? fabulousShader : shader;
        if (active == null) return false;
        active.safeGetUniform("AuraTime").set(time);
        active.safeGetUniform("AuraIntensity").set((float) settings.intensity());
        active.safeGetUniform("ViewAngle").set(viewAngle);
        active.safeGetUniform("AuraShape").set(settings.shape().ordinal());
        color(active, "MainColor", settings.mainColor());
        color(active, "HighlightColor", settings.highlightColor());
        color(active, "BaseColor", settings.baseColor());
        return true;
    }

    private static void color(ShaderInstance shader, String uniform, int rgb) {
        shader.safeGetUniform(uniform).set(((rgb >> 16) & 255) / 255f,
            ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
    }

    private AuraRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                           boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }
}
