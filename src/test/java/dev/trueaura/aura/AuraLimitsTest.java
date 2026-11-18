package dev.trueaura.aura;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuraLimitsTest {
    @Test void expandedSettingsRoundTripAndRespectConfiguredWorldLimits() {
        var high = AuraSettings.DEFAULT.withAppearance(12, 10, 2, AuraShape.LIGHTNING);
        assertEquals(12, high.intensity());
        assertEquals(10, high.scale());
        var limited = new AuraLimits(6, 4).apply(high);
        assertEquals(6, limited.scale());
        assertEquals(4, limited.intensity());
        assertEquals(high.shape(), limited.shape());
        assertEquals(high.mainColor(), limited.mainColor());
        assertEquals(high.openEffect(), limited.openEffect());
        assertEquals(limited, new AuraLimits(6, 4).apply(limited));
        assertEquals(2, high.impactScale());
    }
    @Test void unreasonableConfigAndPacketValuesStayFiniteAndBounded() {
        assertEquals(AuraLimits.DEFAULT, new AuraLimits(Double.NaN, Double.POSITIVE_INFINITY));
        assertEquals(.5, new AuraLimits(-1, -1).maxScale());
        assertEquals(.1, new AuraLimits(-1, -1).maxIntensity());
        var huge = AuraSettings.DEFAULT.withAppearance(1e100, 1e100, 1, AuraShape.FLAME);
        assertEquals(32, huge.scale());
        assertEquals(32, huge.intensity());
        assertEquals(32, new AuraLimits(1e100, 1e100).maxScale());
    }
}
