package dev.trueaura.network;

import dev.trueaura.aura.AuraSettings;
import dev.trueaura.aura.AuraShape;
import dev.trueaura.aura.AuraEffect;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraPacketCodecTest {
    @Test void animationSettingsSurviveEveryWireMessage() {
        var animation=new dev.trueaura.aura.AuraAnimation(true,false,32,25,1.7);
        var state=new AuraSnapshot(AuraSettings.DEFAULT.withAnimation(animation),true);
        assertEquals(state,AuraPacketCodec.decodeUpdate(AuraPacketCodec.encodeUpdate(state)));
        var revision=new AuraPacketCodec.RevisionSnapshot(3,state);
        assertEquals(revision,AuraPacketCodec.decodeRevision(AuraPacketCodec.encodeRevision(revision)));
        var player=new AuraPacketCodec.PlayerSnapshot(UUID.randomUUID(),state);
        assertEquals(player,AuraPacketCodec.decodeSnapshot(AuraPacketCodec.encodeSnapshot(player)));
    }

    @Test void importedDefinitionTravelsWithoutAnyLocalFile() {
        var custom = dev.trueaura.aura.CustomAura.parse("""
            {"version":1,"id":"test:halo","name":"Halo","pattern":"pulse",
             "layers":[{"profile":[[0,0.5],[1,0.2]]}]}
            """);
        var value = new AuraSnapshot(AuraSettings.DEFAULT.withCustom(custom), true);
        assertEquals(value, AuraPacketCodec.decodeUpdate(AuraPacketCodec.encodeUpdate(value)));
        var player = new AuraPacketCodec.PlayerSnapshot(UUID.randomUUID(), value);
        assertEquals(player, AuraPacketCodec.decodeSnapshot(AuraPacketCodec.encodeSnapshot(player)));
        var revision = new AuraPacketCodec.RevisionSnapshot(5, value);
        assertEquals(revision, AuraPacketCodec.decodeRevision(AuraPacketCodec.encodeRevision(revision)));
    }

    @Test void independentModEffectsAndParametersSurviveUpdateAndSnapshot() {
        var settings = AuraSettings.DEFAULT.withEffect(true, new AuraEffect("epicfight:ground_slam", ""))
            .withEffect(false, new AuraEffect("minecraft:dust", "0.2 0.4 1 1.5"));
        var snapshot = new AuraSnapshot(settings, true);
        assertEquals(snapshot, AuraPacketCodec.decodeUpdate(AuraPacketCodec.encodeUpdate(snapshot)));
        var message = new AuraPacketCodec.PlayerSnapshot(UUID.randomUUID(), snapshot);
        assertEquals(message, AuraPacketCodec.decodeSnapshot(AuraPacketCodec.encodeSnapshot(message)));
    }
    @Test void updateRoundTripPreservesEverySettingAndToggle() {
        for (AuraShape shape : AuraShape.values()) {
            AuraSnapshot value = new AuraSnapshot(new AuraSettings(0x112233, 0x445566, 0x778899, 1.7, 0.8, 2.6, shape), true);
            byte[] bytes = AuraPacketCodec.encodeUpdate(value);
            assertEquals(AuraPacketCodec.UPDATE_BYTES, bytes.length);
            assertEquals(value, AuraPacketCodec.decodeUpdate(bytes));
            assertFalse(AuraPacketCodec.decodeUpdate(AuraPacketCodec.encodeUpdate(new AuraSnapshot(value.settings(), false))).enabled());
        }
    }
    @Test void serverSnapshotPreservesIdentityAndRemoval() {
        UUID id = UUID.fromString("00001111-2222-3333-4444-555566667777");
        AuraPacketCodec.PlayerSnapshot message = new AuraPacketCodec.PlayerSnapshot(id, new AuraSnapshot(AuraSettings.DEFAULT, true));
        byte[] bytes = AuraPacketCodec.encodeSnapshot(message);
        assertEquals(AuraPacketCodec.SNAPSHOT_BYTES, bytes.length);
        assertEquals(message, AuraPacketCodec.decodeSnapshot(bytes));
        AuraPacketCodec.PlayerSnapshot removal = new AuraPacketCodec.PlayerSnapshot(id, null);
        assertEquals(AuraPacketCodec.SNAPSHOT_BYTES, AuraPacketCodec.encodeSnapshot(removal).length);
        assertEquals(removal, AuraPacketCodec.decodeSnapshot(AuraPacketCodec.encodeSnapshot(removal)));
    }
    @Test void untrustedBytesAreNormalizedBeforeBecomingState() {
        ByteBuffer bytes = ByteBuffer.wrap(AuraPacketCodec.encodeUpdate(new AuraSnapshot(AuraSettings.DEFAULT, false)));
        bytes.putInt(-1).putInt(0xAB112233).putInt(0);
        bytes.putDouble(Double.NaN).putDouble(Double.POSITIVE_INFINITY).putDouble(-100);
        bytes.put((byte)255).put((byte)1);
        AuraSnapshot value = AuraPacketCodec.decodeUpdate(bytes.array());
        assertEquals(new AuraSettings(0xFFFFFF, 0x112233, 0, 1, 1, 0.1, AuraShape.FLAME), value.settings());
        assertTrue(value.enabled());
    }
    @Test void malformedTextLengthAndUtf8AreRejected() {
        byte[] bytes = AuraPacketCodec.encodeUpdate(new AuraSnapshot(AuraSettings.DEFAULT, true));
        ByteBuffer.wrap(bytes).putShort(38, (short) 129);
        assertThrows(IllegalArgumentException.class, () -> AuraPacketCodec.decodeUpdate(bytes));
        ByteBuffer.wrap(bytes).putShort(38, (short) 1);
        bytes[40] = (byte) 0xFF;
        assertThrows(IllegalArgumentException.class, () -> AuraPacketCodec.decodeUpdate(bytes));
    }
    @Test void malformedLengthCannotBeParsedAsAValidPacket() {
        assertThrows(IllegalArgumentException.class, () -> AuraPacketCodec.decodeUpdate(new byte[AuraPacketCodec.UPDATE_BYTES - 1]));
        assertThrows(IllegalArgumentException.class, () -> AuraPacketCodec.decodeUpdate(new byte[AuraPacketCodec.UPDATE_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> AuraPacketCodec.decodeSnapshot(new byte[AuraPacketCodec.SNAPSHOT_BYTES - 1]));
        assertThrows(IllegalArgumentException.class, () -> AuraPacketCodec.decodeSnapshot(new byte[AuraPacketCodec.SNAPSHOT_BYTES + 1]));
    }
    @Test void valuesAboveTwoSurviveTheNetworkCodec() {
        var settings = dev.trueaura.aura.AuraSettings.DEFAULT.withAppearance(7, 6, 1, dev.trueaura.aura.AuraShape.LOTUS);
        var state = new AuraSnapshot(settings, true);
        assertEquals(state, AuraPacketCodec.decodeUpdate(AuraPacketCodec.encodeUpdate(state)));
    }
}
