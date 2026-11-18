package dev.trueaura.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EpicParticleArgumentsTest {
    @Test void groundSlamUsesSmallNumericDensityInsteadOfEncodedEntityBits() {
        var args = EpicParticleArguments.forEffect("ground_slam", 2, 123, 0, 0, true);
        assertEquals(3.2, args.a());
        assertEquals(32, args.b());
        assertTrue(args.c() > 1);
        var close = EpicParticleArguments.forEffect("ground_slam", 1, 123, 0, 0, false);
        assertEquals(24, close.b());
        assertTrue(close.c() < 0);
        assertTrue(args.ground());
    }
    @Test void metaEmittersUseBoundedBitEncodedCounts() {
        var neutral = EpicParticleArguments.forEffect("neutralize", 1, 1, 0, 0, true);
        var casting = EpicParticleArguments.forEffect("boss_casting", 1, 1, 0, 0, false);
        assertEquals(8, Double.doubleToLongBits(neutral.b()));
        assertEquals(12, Double.doubleToLongBits(casting.b()));
        assertEquals(1, Double.doubleToLongBits(casting.c()));
    }
    @Test void entityIdsAndDetachedShieldSentinelSurviveCanonicalDoubleConversion() {
        var image = EpicParticleArguments.forEffect("white_afterimage", 1, 12345, 0, 0, true);
        assertEquals(12345, (int) Double.doubleToLongBits(image.a()));
        var shield = EpicParticleArguments.forEffect("force_field", 1, 12345, 0, 0, true);
        assertTrue(Double.isFinite(shield.b()));
        assertEquals(-1, (int) Double.doubleToLongBits(shield.b()));
    }
}
