package dev.trueaura.network;

import dev.trueaura.aura.AuraSettings;
import dev.trueaura.aura.CustomAura;
import dev.trueaura.aura.AuraShape;
import dev.trueaura.aura.AuraEffect;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.util.UUID;

/** Bounded wire format. Only custom definitions add variable bytes; identity is server-owned. */
final class AuraPacketCodec {
    static final int EFFECT_BYTES = 4 + AuraEffect.MAX_ID_BYTES + AuraEffect.MAX_PARAMETER_BYTES;
    static final int UPDATE_BYTES = 53 + 2 * EFFECT_BYTES;
    static final int SNAPSHOT_BYTES = 17 + UPDATE_BYTES;
    record PlayerSnapshot(UUID playerId, AuraSnapshot snapshot) {}
    static final int REVISION_BYTES = Long.BYTES + UPDATE_BYTES;
    record RevisionSnapshot(long revision, AuraSnapshot snapshot) {}

    static byte[] encodeRevision(RevisionSnapshot value) {
        ByteBuffer buffer = ByteBuffer.allocate(REVISION_BYTES + customBytes(value.snapshot().settings()).length);
        buffer.putLong(value.revision());
        writeUpdate(buffer, value.snapshot());
        return buffer.array();
    }
    static RevisionSnapshot decodeRevision(byte[] bytes) {
        ByteBuffer buffer = exactBuffer(bytes, REVISION_BYTES);
        long revision = buffer.getLong();
        if (revision < 0) throw new IllegalArgumentException("Invalid aura revision");
        var result = new RevisionSnapshot(revision, readUpdate(buffer));
        finished(buffer); return result;
    }

    static byte[] encodeUpdate(AuraSnapshot value) {
        ByteBuffer buffer = ByteBuffer.allocate(UPDATE_BYTES + customBytes(value.settings()).length);
        writeUpdate(buffer, value);
        return buffer.array();
    }

    static AuraSnapshot decodeUpdate(byte[] bytes) {
        var buffer = exactBuffer(bytes, UPDATE_BYTES);
        var result = readUpdate(buffer); finished(buffer); return result;
    }

    static byte[] encodeSnapshot(PlayerSnapshot value) {
        ByteBuffer buffer = ByteBuffer.allocate(SNAPSHOT_BYTES + (value.snapshot() == null ? 0 : customBytes(value.snapshot().settings()).length));
        buffer.putLong(value.playerId().getMostSignificantBits()).putLong(value.playerId().getLeastSignificantBits());
        buffer.put((byte) (value.snapshot() != null ? 1 : 0));
        // Removal retains the same bounded packet size.
        writeUpdate(buffer, value.snapshot() == null ? new AuraSnapshot(AuraSettings.DEFAULT, false) : value.snapshot());
        return buffer.array();
    }

    static PlayerSnapshot decodeSnapshot(byte[] bytes) {
        ByteBuffer buffer = exactBuffer(bytes, SNAPSHOT_BYTES);
        UUID id = new UUID(buffer.getLong(), buffer.getLong());
        boolean present = buffer.get() != 0;
        AuraSnapshot snapshot = readUpdate(buffer);
        finished(buffer);
        return new PlayerSnapshot(id, present ? snapshot : null);
    }

    private static ByteBuffer exactBuffer(byte[] bytes, int expected) {
        if (bytes.length < expected || bytes.length > expected + CustomAura.MAX_BYTES) throw new IllegalArgumentException("Invalid aura packet length");
        return ByteBuffer.wrap(bytes);
    }

    private static void writeUpdate(ByteBuffer buffer, AuraSnapshot value) {
        AuraSettings settings = value.settings();
        buffer.putInt(settings.mainColor()).putInt(settings.highlightColor()).putInt(settings.baseColor());
        buffer.putDouble(settings.intensity()).putDouble(settings.scale()).putDouble(settings.speed());
        buffer.put((byte) settings.shape().ordinal()).put((byte) (value.enabled() ? 1 : 0));
        writeEffect(buffer, settings.openEffect());
        writeEffect(buffer, settings.closeEffect());
        var animation = settings.animation();
        buffer.put((byte) ((animation.instantAppear() ? 1 : 0) | (animation.instantDisappear() ? 2 : 0) | (animation.riseFromFeet() ? 4 : 0)));
        buffer.putShort((short) animation.appearTicks()).putShort((short) animation.disappearTicks()).putDouble(animation.motionStrength());
        byte[] custom = customBytes(settings);
        buffer.putShort((short) custom.length).put(custom);
    }

    private static AuraSnapshot readUpdate(ByteBuffer buffer) {
        AuraSettings settings = new AuraSettings(buffer.getInt(), buffer.getInt(), buffer.getInt(),
                buffer.getDouble(), buffer.getDouble(), buffer.getDouble(), AuraShape.fromId(Byte.toUnsignedInt(buffer.get())));
        boolean enabled = buffer.get() != 0;
        settings = settings.withEffect(true, readEffect(buffer)).withEffect(false, readEffect(buffer));
        int flags = Byte.toUnsignedInt(buffer.get());
        if ((flags & ~7) != 0) throw new IllegalArgumentException("Invalid animation flags");
        settings = settings.withAnimation(new dev.trueaura.aura.AuraAnimation((flags & 1) != 0, (flags & 2) != 0,
            Short.toUnsignedInt(buffer.getShort()), Short.toUnsignedInt(buffer.getShort()), buffer.getDouble(), (flags & 4) != 0));
        int size = Short.toUnsignedInt(buffer.getShort());
        if (size > CustomAura.MAX_BYTES || size > buffer.remaining()) throw new IllegalArgumentException("Invalid custom aura size");
        if (size > 0) {
            byte[] custom = new byte[size]; buffer.get(custom);
            try {
                String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(custom)).toString();
                settings = settings.withCustom(CustomAura.parse(json));
            } catch (CharacterCodingException ex) { throw new IllegalArgumentException("Invalid custom aura UTF-8", ex); }
        }
        return new AuraSnapshot(settings, enabled);
    }

    private static byte[] customBytes(AuraSettings settings) {
        byte[] bytes = settings.custom() == null ? new byte[0] : settings.custom().toJson().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > CustomAura.MAX_BYTES) throw new IllegalArgumentException("Custom aura is too large");
        return bytes;
    }
    private static void finished(ByteBuffer buffer) {
        if (buffer.hasRemaining()) throw new IllegalArgumentException("Trailing aura packet data");
    }
    private static void writeEffect(ByteBuffer buffer, AuraEffect effect) {
        writeText(buffer, effect.id(), AuraEffect.MAX_ID_BYTES);
        writeText(buffer, effect.parameters(), AuraEffect.MAX_PARAMETER_BYTES);
    }
    private static AuraEffect readEffect(ByteBuffer buffer) {
        return new AuraEffect(readText(buffer, AuraEffect.MAX_ID_BYTES), readText(buffer, AuraEffect.MAX_PARAMETER_BYTES));
    }
    private static void writeText(ByteBuffer buffer, String text, int max) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) bytes.length).put(bytes);
        buffer.put(new byte[max - bytes.length]);
    }
    private static String readText(ByteBuffer buffer, int max) {
        int size = Short.toUnsignedInt(buffer.getShort());
        if (size > max) throw new IllegalArgumentException("Aura effect text too long");
        ByteBuffer text = buffer.slice();
        text.limit(size);
        buffer.position(buffer.position() + max);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(text).toString();
        } catch (CharacterCodingException ex) {
            throw new IllegalArgumentException("Invalid aura effect UTF-8", ex);
        }
    }
}
