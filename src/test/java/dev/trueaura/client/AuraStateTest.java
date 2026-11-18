package dev.trueaura.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraStateTest {
    @Test void startsOffAndFadesBothDirectionsWithoutOvershoot() {
        AuraState state = new AuraState();
        assertFalse(state.enabled());
        assertEquals(0, state.opacity(1));
        state.toggle();
        state.tick(true);
        assertTrue(state.opacity(1) > 0 && state.opacity(1) < 1);
        assertEquals(0, state.opacity(0));
        for (int i = 0; i < 40; i++) state.tick(true);
        assertEquals(1, state.opacity(1));
        state.toggle();
        state.tick(true);
        assertTrue(state.opacity(1) > 0 && state.opacity(1) < 1);
        for (int i = 0; i < 40; i++) state.tick(true);
        assertEquals(0, state.opacity(1));
    }

    @Test void reversalPreservesPreviousFrameAndDisconnectResetsEverything() {
        AuraState state = new AuraState();
        state.toggle();
        for (int i = 0; i < 3; i++) state.tick(true);
        float before = state.opacity(1);
        state.toggle();
        state.tick(true);
        assertEquals(before, state.opacity(0), 0.00001);
        assertTrue(state.opacity(1) < before);
        state.tick(false);
        assertFalse(state.enabled());
        assertEquals(0, state.opacity(0));
        assertEquals(0, state.opacity(1));
        assertEquals(0, state.time(0));
    }

    @Test void clockOnlyMovesOnTicksAndPartialTicksAreClamped() {
        AuraState state = new AuraState();
        state.tick(true);
        double time = state.time(0.5f);
        assertEquals(time, state.time(0.5f));
        state.tick(true);
        assertEquals(1, state.time(0.5f) - time, 0.00001);
        state.toggle();
        state.tick(true);
        assertEquals(state.opacity(0), state.opacity(-1));
        assertEquals(state.opacity(1), state.opacity(2));
    }

    @Test void animationClockDoesNotJumpAfterTwentyMinutes() {
        AuraState state = new AuraState();
        for (int i = 0; i < 23999; i++) state.tick(true);
        double before = state.time(0.5f);
        state.tick(true);
        assertEquals(1.0, state.time(0.5f) - before, 0.00001);
    }

    @Test void unloadResetClearsEnabledFadeAndClockBeforeNextWorld() {
        AuraState state = new AuraState();
        state.toggle();
        for (int i = 0; i < 30; i++) state.tick(true);
        state.reset();
        assertFalse(state.enabled());
        assertEquals(0, state.opacity(0.5f));
        assertEquals(0, state.time(0));
        state.tick(true);
        assertEquals(0, state.opacity(1));
    }

    @Test void remoteUpdatesSetStateRatherThanToggleIt() {
        AuraState state = new AuraState();
        state.setEnabled(true);
        state.tick(true);
        float before = state.opacity(1);
        state.setEnabled(true);
        state.tick(true);
        assertTrue(state.enabled());
        assertTrue(state.opacity(1) > before);
        state.setEnabled(false);
        state.tick(true);
        assertFalse(state.enabled());
    }
}
