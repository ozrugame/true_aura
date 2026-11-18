package dev.trueaura.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Server-thread-only latest-wins queue, using monotonic milliseconds. */
final class AuraUpdateQueue {
    private static final long INTERVAL_MILLIS = 200;
    private final Map<UUID, AuraSnapshot> pending = new HashMap<>();
    private final Map<UUID, Long> nextAllowed = new HashMap<>();

    void offer(UUID id, AuraSnapshot value) {
        pending.put(id, value);
    }

    AuraSnapshot peek(UUID id) { return pending.get(id); }

    void drain(long nowMillis, BiConsumer<UUID, AuraSnapshot> receiver) {
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Long next = nextAllowed.get(entry.getKey());
            if (next != null && nowMillis < next) continue;
            nextAllowed.put(entry.getKey(), nowMillis + INTERVAL_MILLIS);
            receiver.accept(entry.getKey(), entry.getValue());
            iterator.remove();
        }
    }

    void remove(UUID id) {
        pending.remove(id);
        nextAllowed.remove(id);
    }

    void clear() {
        pending.clear();
        nextAllowed.clear();
    }
}
