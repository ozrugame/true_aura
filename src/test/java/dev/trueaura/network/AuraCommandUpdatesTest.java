package dev.trueaura.network;

import dev.trueaura.aura.AuraSettings;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraCommandUpdatesTest {
    private static final AuraSnapshot ON = new AuraSnapshot(AuraSettings.DEFAULT, true);
    private static final AuraSnapshot OFF = new AuraSnapshot(AuraSettings.DEFAULT, false);

    @Test void duplicateCommandCannotUndoLocalToggleAfterAcknowledgement() {
        AuraCommandUpdates updates = new AuraCommandUpdates();
        AtomicReference<AuraSnapshot> local = new AtomicReference<>(OFF);
        var command = new AuraPacketCodec.RevisionSnapshot(1, ON);
        updates.receive(command, local::set);
        assertEquals(ON, local.get());
        local.set(OFF); // The player presses R after receiving revision 1.
        updates.receive(command, local::set); // A stale C2S edit caused a delayed resend.
        assertEquals(OFF, local.get());
        assertEquals(1, updates.revision());
        updates.receive(new AuraPacketCodec.RevisionSnapshot(2, ON), local::set);
        assertEquals(ON, local.get());
        updates.receive(new AuraPacketCodec.RevisionSnapshot(1, OFF), local::set);
        assertEquals(ON, local.get());
        assertEquals(2, updates.revision());
    }
    @Test void reconnectAcceptsTheNewServersFirstCommand() {
        AuraCommandUpdates updates = new AuraCommandUpdates();
        AtomicReference<AuraSnapshot> local = new AtomicReference<>(OFF);
        updates.receive(new AuraPacketCodec.RevisionSnapshot(100, ON), local::set);
        updates.reset();
        assertEquals(0, updates.revision());
        updates.receive(new AuraPacketCodec.RevisionSnapshot(1, OFF), local::set);
        assertEquals(OFF, local.get());
        assertEquals(1, updates.revision());
    }
}
