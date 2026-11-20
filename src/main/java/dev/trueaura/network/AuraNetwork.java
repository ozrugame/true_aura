package dev.trueaura.network;

import dev.trueaura.aura.AuraSettings;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Optional, common-side protocol. All session state is touched on the server thread. */
public final class AuraNetwork {
    private static final String PROTOCOL = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("trueaura", "appearance"), () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));
    private static final AuraServerState STATE = new AuraServerState();
    private static volatile Consumer<AuraSnapshot> commandReceiver = snapshot -> {};
    private static final AuraCommandUpdates CLIENT_COMMANDS = new AuraCommandUpdates();
    private static volatile BiConsumer<UUID, AuraSnapshot> clientReceiver = (id, snapshot) -> {};
    private static volatile WeakReference<Connection> clientConnection = new WeakReference<>(null);
    private static boolean initialized;
    private static dev.trueaura.aura.AuraLimits lastLimits;

    private AuraNetwork() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        CHANNEL.messageBuilder(AuraPacketCodec.RevisionSnapshot.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> buffer.writeBytes(AuraPacketCodec.encodeRevision(message)))
                .decoder(buffer -> AuraPacketCodec.decodeRevision(readExact(buffer, AuraPacketCodec.REVISION_BYTES)))
                .consumerMainThread((message, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null && sender.connection.connection.isConnected()) {
                        // Identity is authenticated by the connection, never supplied by the packet.
                        AuraSnapshot bounded = bound(message.snapshot());
                        if (!STATE.offer(sender.getUUID(), message.revision(), bounded)) sendCommand(sender);
                        else if (!bounded.equals(message.snapshot())) applyCommand(sender, old -> bounded);
                    }
                }).add();
        CHANNEL.messageBuilder(AuraPacketCodec.PlayerSnapshot.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> buffer.writeBytes(AuraPacketCodec.encodeSnapshot(message)))
                .decoder(buffer -> AuraPacketCodec.decodeSnapshot(readExact(buffer, AuraPacketCodec.SNAPSHOT_BYTES)))
                .consumerMainThread((message, context) -> {
                    Connection connection = context.get().getNetworkManager();
                    // Ignore work that was queued immediately before a connection closed.
                    if (connection.isConnected()) clientReceiver.accept(message.playerId(), message.snapshot());
                }).add();
        CHANNEL.messageBuilder(CommandSnapshot.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> buffer.writeBytes(AuraPacketCodec.encodeRevision(message.value())))
                .decoder(buffer -> new CommandSnapshot(AuraPacketCodec.decodeRevision(readExact(buffer, AuraPacketCodec.REVISION_BYTES))))
                .consumerMainThread((message, context) -> {
                    if (context.get().getNetworkManager().isConnected()) {
                        CLIENT_COMMANDS.receive(message.value(), commandReceiver);
                    }
                }).add();
        MinecraftForge.EVENT_BUS.register(AuraNetwork.class);
    }

    private static byte[] readExact(FriendlyByteBuf buffer, int expected) {
        if (buffer.readableBytes() < expected || buffer.readableBytes() > expected + dev.trueaura.aura.CustomAura.MAX_BYTES) throw new IllegalArgumentException("Invalid aura packet length");
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    /** Check the current client connection before sending; also binds it without referencing Minecraft client classes. */
    public static boolean canSync(Connection connection) {
        if (connection != null && connection.getReceiving() == PacketFlow.CLIENTBOUND) {
            clientConnection = new WeakReference<>(connection);
        }
        return connection != null && connection.isConnected() && CHANNEL.isRemotePresent(connection);
    }

    public static void sendToServer(AuraSettings settings, boolean enabled) {
        Connection connection = clientConnection.get();
        if (canSync(connection)) {
            CHANNEL.sendTo(new AuraPacketCodec.RevisionSnapshot(CLIENT_COMMANDS.revision(),
                new AuraSnapshot(Objects.requireNonNull(settings), enabled)), connection, NetworkDirection.PLAY_TO_SERVER);
        }
    }

    /** Installed by client setup; a null snapshot removes the player from the client cache. */
    public static void setClientReceiver(BiConsumer<UUID, AuraSnapshot> receiver) {
        clientReceiver = Objects.requireNonNull(receiver);
    }

    private record CommandSnapshot(AuraPacketCodec.RevisionSnapshot value) {}

    public static void setCommandReceiver(Consumer<AuraSnapshot> receiver) {
        commandReceiver = Objects.requireNonNull(receiver);
    }

    /** Only on connection boundaries, not respawn/dimension changes. */
    public static void resetClientConnection() {
        CLIENT_COMMANDS.reset();
        clientConnection = new WeakReference<>(null);
    }

    public static AuraSnapshot current(ServerPlayer player) { return STATE.current(player.getUUID()); }

    /** Called only by the permission-checked command handler on the server thread. */
    public static void applyCommand(ServerPlayer owner, UnaryOperator<AuraSnapshot> edit) {
        AuraSnapshot snapshot = STATE.command(owner.getUUID(), old -> bound(edit.apply(old)));
        for (ServerPlayer viewer : owner.serverLevel().players()) send(viewer, owner.getUUID(), snapshot);
        sendCommand(owner);
    }

    private static AuraSnapshot bound(AuraSnapshot value) {
        return new AuraSnapshot(dev.trueaura.config.AuraServerConfig.limits().apply(value.settings()), value.enabled());
    }

    private static void sendCommand(ServerPlayer owner) {
        if (canSync(owner.connection.connection)) {
            CHANNEL.sendTo(new CommandSnapshot(new AuraPacketCodec.RevisionSnapshot(
                STATE.revision(owner.getUUID()), STATE.current(owner.getUUID()))),
                owner.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var limits = dev.trueaura.config.AuraServerConfig.limits();
        if (!limits.equals(lastLimits)) {
            lastLimits = limits;
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                AuraSnapshot old = current(player);
                if (!old.equals(bound(old))) applyCommand(player, value -> value);
            }
        }
        STATE.drain(System.nanoTime() / 1_000_000L, (id, snapshot) -> {
            ServerPlayer owner = event.getServer().getPlayerList().getPlayer(id);
            if (owner == null || !owner.connection.connection.isConnected()) return;
            for (ServerPlayer viewer : owner.serverLevel().players()) send(viewer, id, snapshot);
            // A resend after the client clears its dimension cache is also a snapshot request.
            sendDimensionSnapshot(owner);
        });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendDimensionSnapshot(player);
    }

    @SubscribeEvent
    public static void onTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            AuraSnapshot snapshot = STATE.published(target.getUUID());
            if (snapshot != null) send(viewer, target.getUUID(), snapshot);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) resetPlayer(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) resetPlayer(player);
    }

    private static void resetPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        STATE.reset(id);
        AuraSnapshot disabled = STATE.current(id);
        // Remove old-dimension ghosts before publishing the disabled new-dimension state.
        broadcastRemoval(player);
        for (ServerPlayer viewer : player.serverLevel().players()) send(viewer, id, disabled);
        sendDimensionSnapshot(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATE.remove(player.getUUID());
            broadcastRemoval(player);
        }
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        STATE.clear();
        lastLimits = null;
    }

    private static void broadcastRemoval(ServerPlayer player) {
        if (player.getServer() == null) return;
        for (ServerPlayer viewer : player.getServer().getPlayerList().getPlayers()) {
            send(viewer, player.getUUID(), null);
        }
    }

    private static void sendDimensionSnapshot(ServerPlayer viewer) {
        if (!canSync(viewer.connection.connection)) return;
        for (ServerPlayer owner : viewer.serverLevel().players()) {
            if (owner == viewer) continue;
            AuraSnapshot snapshot = STATE.published(owner.getUUID());
            if (snapshot != null) send(viewer, owner.getUUID(), snapshot);
        }
    }

    private static void send(ServerPlayer viewer, UUID id, AuraSnapshot snapshot) {
        Connection connection = viewer.connection.connection;
        if (canSync(connection)) {
            CHANNEL.sendTo(new AuraPacketCodec.PlayerSnapshot(id, snapshot), connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }
}
