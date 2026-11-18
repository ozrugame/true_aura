package dev.trueaura.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraSettingsTest {
    @Test void invalidNetworkNumbersCannotReachRenderer() {
        AuraSettings value = new AuraSettings(-1, 0xAB112233, 0, Double.NaN, Double.POSITIVE_INFINITY, -5, null);
        assertEquals(0xFFFFFF, value.mainColor());
        assertEquals(0x112233, value.highlightColor());
        assertEquals(1, value.intensity());
        assertEquals(1, value.scale());
        assertEquals(0.1, value.speed());
        assertEquals(AuraShape.FLAME, value.shape());
        value = new AuraSettings(0, 0, 0, 10, -10, 10, AuraShape.FLOW);
        assertEquals(10, value.intensity());
        assertEquals(0.5, value.scale());
        assertEquals(3, value.speed());
    }

    @Test void hexEditorAcceptsOnlyCompleteColors() {
        assertEquals(0x00FFAA, AuraSettings.parseHex("#00ffaa").orElseThrow());
        assertEquals(0x123456, AuraSettings.parseHex(" 123456 ").orElseThrow());
        for (String text : new String[]{"", "#12", "GG0000", "1234567", "0x1234", "-12345"}) {
            assertTrue(AuraSettings.parseHex(text).isEmpty(), text);
        }
        assertTrue(AuraSettings.parseHex(null).isEmpty());
        assertEquals("#00FFAA", AuraSettings.formatHex(0x00ffaa));
    }

    @Test void shapeIdsRoundTripAndUnknownIdsFallBack() {
        for (AuraShape shape : AuraShape.values()) assertEquals(shape, AuraShape.fromId(shape.ordinal()));
        assertEquals(AuraShape.FLAME, AuraShape.fromId(-1));
        assertEquals(AuraShape.FLAME, AuraShape.fromId(100));
    }
}
