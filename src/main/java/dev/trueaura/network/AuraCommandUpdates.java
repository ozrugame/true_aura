package dev.trueaura.network;

import java.util.function.Consumer;

/** Client-thread command revision tracking, independent of ordinary local edits. */
final class AuraCommandUpdates {
    private long revision;
    long revision() { return revision; }
    void reset() { revision = 0; }
    void receive(AuraPacketCodec.RevisionSnapshot message, Consumer<AuraSnapshot> receiver) {
        // Resends acknowledge stale C2S edits; they must not undo later local R/O input.
        if (message.revision() <= revision) return;
        revision = message.revision();
        receiver.accept(message.snapshot());
    }
}
