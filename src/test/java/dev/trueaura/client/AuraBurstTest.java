package dev.trueaura.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraBurstTest {
    @Test void activationExpandsAndDeactivationContractsThenBothExpire() {
        AuraBurst on = new AuraBurst(true), off = new AuraBurst(false);
        double onStart = on.radius(0), offStart = off.radius(0);
        for (int i = 0; i < 8; i++) { on.tick(); off.tick(); }
        assertTrue(on.radius(0) > onStart + 0.7);
        assertTrue(off.radius(0) < offStart - 0.7);
        for (int i = 0; i < 50; i++) { on.tick(); off.tick(); }
        assertTrue(on.expired());
        assertTrue(off.expired());
        assertEquals(0, on.opacity(0));
        assertEquals(0, off.opacity(1));
    }

    @Test void snapshotsAndAppearanceEditsDoNotTriggerImpacts() {
        assertFalse(AuraBurst.shouldStart(null, true));
        assertFalse(AuraBurst.shouldStart(null, false));
        assertFalse(AuraBurst.shouldStart(true, true));
        assertFalse(AuraBurst.shouldStart(false, false));
        assertTrue(AuraBurst.shouldStart(false, true));
        assertTrue(AuraBurst.shouldStart(true, false));
    }
    @Test void impactWaveArrivesQuicklyAndTilesSettleWithoutResidualOffset() {
        AuraBurst on = new AuraBurst(true), off = new AuraBurst(false);
        assertEquals(0, on.tileLift(0, 0));
        assertEquals(0, off.tileLift(1, 0));
        for (int i = 0; i < 4; i++) { on.tick(); off.tick(); }
        assertTrue(on.radius(0) > 2.5);
        assertTrue(on.tileLift(0, 0) > .6);
        assertEquals(0, on.tileLift(1, 0)); // Wave has not reached the outer band.
        assertTrue(off.tileLift(1, 0) > 0);
        assertEquals(0, off.tileLift(0, 0)); // Closing wave travels inward.
        for (int i = 4; i < 45; i++) {
            for (double distance : new double[]{0, .5, 1}) {
                assertTrue(Double.isFinite(on.tileLift(distance, .5f)));
                assertTrue(on.tileLift(distance, .5f) >= 0);
                assertTrue(off.tileLift(distance, .5f) >= 0);
            }
            on.tick(); off.tick();
        }
        for (double distance : new double[]{0, .5, 1}) {
            assertEquals(0, on.tileLift(distance, 1));
            assertEquals(0, off.tileLift(distance, 1));
            assertEquals(0, on.tileTilt(distance, 1));
            assertEquals(0, off.tileTilt(distance, 1));
        }
        assertEquals(0, on.waveOpacity(1));
        assertEquals(0, off.waveOpacity(1));
    }

}
