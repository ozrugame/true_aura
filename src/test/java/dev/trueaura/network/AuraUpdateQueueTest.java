package dev.trueaura.network;

import dev.trueaura.aura.AuraSettings;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraUpdateQueueTest {
    private static final AuraSnapshot ON = new AuraSnapshot(AuraSettings.DEFAULT, true);
    private static final AuraSnapshot OFF = new AuraSnapshot(AuraSettings.DEFAULT, false);
    @Test void burstDeliversOnlyLatestStateAfterRateWindowIncludingFinalOff() {
        AuraUpdateQueue queue = new AuraUpdateQueue();
        UUID id = UUID.randomUUID();
        Map<UUID, AuraSnapshot> delivered = new HashMap<>();
        queue.offer(id, ON);
        queue.drain(1000, delivered::put);
        assertEquals(Map.of(id, ON), delivered);
        delivered.clear();
        queue.offer(id, OFF);
        queue.offer(id, ON);
        queue.offer(id, OFF);
        queue.drain(1199, delivered::put);
        assertTrue(delivered.isEmpty());
        queue.drain(1200, delivered::put);
        assertEquals(Map.of(id, OFF), delivered);
        delivered.clear();
        queue.drain(1400, delivered::put);
        assertTrue(delivered.isEmpty());
    }
    @Test void playersHaveIndependentBudgetsAndRemovalDiscardsPendingState() {
        AuraUpdateQueue queue = new AuraUpdateQueue();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        Map<UUID, AuraSnapshot> delivered = new HashMap<>();
        queue.offer(first, ON);
        queue.drain(1000, delivered::put);
        delivered.clear();
        queue.offer(first, OFF);
        queue.offer(second, ON);
        queue.drain(1010, delivered::put);
        assertEquals(Map.of(second, ON), delivered);
        queue.remove(first);
        delivered.clear();
        queue.drain(1200, delivered::put);
        assertTrue(delivered.isEmpty());
        queue.offer(first, OFF);
        queue.clear();
        queue.drain(2000, delivered::put);
        assertTrue(delivered.isEmpty());
    }
    @Test void sustainedSpamNeverExceedsFiveDeliveriesPerSecondAndKeepsFinalState() {
        AuraUpdateQueue queue = new AuraUpdateQueue();
        UUID id = UUID.randomUUID();
        List<AuraSnapshot> delivered = new ArrayList<>();
        for (long time = 0; time < 1000; time++) {
            queue.offer(id, time % 2 == 0 ? ON : OFF);
            queue.drain(time, (player, snapshot) -> delivered.add(snapshot));
        }
        assertEquals(5, delivered.size());
        queue.drain(1000, (player, snapshot) -> delivered.add(snapshot));
        assertEquals(6, delivered.size());
        assertEquals(OFF, delivered.get(5));
    }

}
