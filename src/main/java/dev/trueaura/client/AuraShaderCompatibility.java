package dev.trueaura.client;

import java.lang.reflect.Method;
import net.minecraftforge.fml.ModList;

/** Optional Oculus/Iris public API; never links the shader loader on a vanilla client or server. */
public final class AuraShaderCompatibility {
    private static boolean checked;
    private static Object api;
    private static Method active, shadow;
    private static void initialize() {
        if (checked) return;
        checked = true;
        if (!ModList.get().isLoaded("oculus") && !ModList.get().isLoaded("iris")) return;
        try {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = type.getMethod("getInstance").invoke(null);
            active = type.getMethod("isShaderPackInUse");
            shadow = type.getMethod("isRenderingShadowPass");
        } catch (ReflectiveOperationException | LinkageError ex) {
            com.mojang.logging.LogUtils.getLogger().warn("True Aura: shader loader API unavailable", ex);
            api = null;
        }
    }
    private static boolean query(boolean shadowPass) {
        initialize();
        if (api == null) return false;
        try { return (boolean) (shadowPass ? shadow : active).invoke(api); }
        catch (ReflectiveOperationException ex) { return false; }
    }
    public static boolean active() { return query(false); }
    public static boolean shadowPass() { return query(true); }
    private AuraShaderCompatibility() {}
}
