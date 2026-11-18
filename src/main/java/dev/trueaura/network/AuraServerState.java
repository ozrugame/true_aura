package dev.trueaura.network;

import dev.trueaura.aura.AuraSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/** Server-thread state. Commands supersede queued and in-flight edits from older revisions. */
final class AuraServerState {
    private final Map<UUID, AuraSnapshot> states = new HashMap<>();
    private final Map<UUID, Long> revisions = new HashMap<>();
    private final AuraUpdateQueue updates = new AuraUpdateQueue();

    long revision(UUID id) { return revisions.getOrDefault(id, 0L); }
    AuraSnapshot published(UUID id) { return states.get(id); }
    AuraSnapshot current(UUID id) {
        AuraSnapshot pending = updates.peek(id);
        return pending != null ? pending : states.getOrDefault(id, new AuraSnapshot(AuraSettings.DEFAULT, false));
    }
    boolean offer(UUID id, long revision, AuraSnapshot value) {
        if (revision != revision(id)) return false;
        updates.offer(id, value);
        return true;
    }
    AuraSnapshot command(UUID id, UnaryOperator<AuraSnapshot> edit) {
        AuraSnapshot result = edit.apply(current(id));
        updates.remove(id);
        revisions.put(id, revision(id) + 1);
        states.put(id, result);
        return result;
    }
    void reset(UUID id) {
        AuraSnapshot previous = current(id);
        updates.remove(id);
        states.put(id, new AuraSnapshot(previous.settings(), false));
    }
    void drain(long now, BiConsumer<UUID, AuraSnapshot> receiver) {
        updates.drain(now, (id, snapshot) -> {
            states.put(id, snapshot);
            receiver.accept(id, snapshot);
        });
    }
    void remove(UUID id) { updates.remove(id); states.remove(id); revisions.remove(id); }
    void clear() { updates.clear(); states.clear(); revisions.clear(); }
}
