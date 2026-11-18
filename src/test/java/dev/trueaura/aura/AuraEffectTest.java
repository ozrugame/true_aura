package dev.trueaura.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraEffectTest {
    @Test void delayedShrieksCannotOutliveTheToggleEffectWindow() {
        assertTrue(new AuraEffect("minecraft:shriek", "0").timingAllowed());
        assertTrue(new AuraEffect("minecraft:shriek", "60").timingAllowed());
        for (String delay : new String[]{"-1", "61", "2147483647", "garbage"}) {
            assertFalse(new AuraEffect("minecraft:shriek", delay).timingAllowed());
        }
        assertTrue(new AuraEffect("other:particle", "").timingAllowed());
    }
    @Test void oldSettingsDefaultToOriginalBurstAndChoicesRemainIndependent() {
        var original = new AuraSettings(1, 2, 3, 1, 1, 1, AuraShape.FLAME);
        assertEquals(AuraEffect.BURST, original.openEffect());
        assertEquals(AuraEffect.BURST, original.closeEffect());
        var chosen = original.withEffect(true, new AuraEffect("epicfight:ground_slam", ""))
            .withEffect(false, AuraEffect.NONE);
        var edited = chosen.withColors(4, 5, 6).withAppearance(1.5, 1.2, .8, AuraShape.PULSE);
        assertEquals(chosen.openEffect(), edited.openEffect());
        assertEquals(AuraEffect.NONE, edited.closeEffect());
    }
    @Test void invalidOrUnboundedChoicesNormalizeBeforeReachingNetwork() {
        for (String id : new String[]{"../../bad", "Minecraft:flame", "no namespace", "x:" + "a".repeat(200)}) {
            assertEquals(AuraEffect.BURST, new AuraEffect(id, ""));
        }
        assertEquals(AuraEffect.BURST, new AuraEffect("minecraft:dust", "x".repeat(257)));
        assertEquals(AuraEffect.BURST, new AuraEffect("minecraft:dust", "a\nb"));
        assertEquals(new AuraEffect("minecraft:dust", "1 0 0 1"), new AuraEffect("minecraft:dust", " 1 0 0 1 "));
    }
}
