package dev.trueaura.network;

import dev.trueaura.aura.AuraSettings;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraServerStateTest {
    private final UUID id = UUID.randomUUID();
    private final AuraServerState server = new AuraServerState();
    private static final AuraSnapshot OFF = new AuraSnapshot(AuraSettings.DEFAULT, false);

    @Test void commandKeepsLatestPendingAppearanceAndCannotBeUndoneByStalePackets() {
        AuraSettings blue = AuraSettings.DEFAULT.withColors(0x0000FF, 0xFFFFFF, 0xCC00FF);
        server.offer(id, 0, new AuraSnapshot(blue, false));
        AuraSnapshot on = server.command(id, old -> new AuraSnapshot(old.settings(), true));
        assertEquals(blue, on.settings());
        assertTrue(on.enabled());
        assertFalse(server.offer(id, 0, OFF));
        Map<UUID, AuraSnapshot> delivered = new HashMap<>();
        server.drain(1000, delivered::put);
        assertTrue(delivered.isEmpty());
        assertEquals(on, server.published(id));
        // Once the client has applied the command, normal user controls still work.
        assertTrue(server.offer(id, 1, new AuraSnapshot(blue, false)));
        server.drain(1200, delivered::put);
        assertFalse(delivered.get(id).enabled());
    }
    @Test void successiveCommandsComposeAndDoNotAffectOtherPlayers() {
        UUID other = UUID.randomUUID();
        server.offer(other, 0, OFF);
        server.command(id, old -> new AuraSnapshot(old.settings(), true));
        server.command(id, old -> new AuraSnapshot(old.settings().withColors(123, 456, 789), old.enabled()));
        assertTrue(server.current(id).enabled());
        assertEquals(123, server.current(id).settings().mainColor());
        assertEquals(2, server.revision(id));
        assertEquals(0, server.revision(other));
        assertEquals(OFF, server.current(other));
    }
    @Test void respawnKeepsRevisionAndAppearanceButLogoutResetsSession() {
        server.command(id, old -> new AuraSnapshot(old.settings(), true));
        server.reset(id);
        assertFalse(server.current(id).enabled());
        assertEquals(1, server.revision(id));
        server.remove(id);
        assertNull(server.published(id));
        assertEquals(0, server.revision(id));
        server.command(id, old -> old);
        server.clear();
        assertNull(server.published(id));
        assertEquals(0, server.revision(id));
    }
    @Test void revisionPacketRoundTripsAndRejectsMalformedLengthOrNegativeRevision() {
        var value = new AuraPacketCodec.RevisionSnapshot(42, OFF);
        byte[] bytes = AuraPacketCodec.encodeRevision(value);
        assertEquals(value, AuraPacketCodec.decodeRevision(bytes));
        assertThrows(IllegalArgumentException.class, () -> AuraPacketCodec.decodeRevision(Arrays.copyOf(bytes, bytes.length + 1)));
        bytes[0] = (byte) 0xFF;
        assertThrows(IllegalArgumentException.class, () -> AuraPacketCodec.decodeRevision(bytes));
    }
}
