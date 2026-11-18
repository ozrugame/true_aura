package dev.trueaura.client;

import dev.trueaura.particle.AuraParticleCatalog;

import dev.trueaura.aura.AuraEffect;
import dev.trueaura.aura.AuraSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Inline browser: the owning screen keeps its draft, camera and Save/Cancel lifecycle. */
final class AuraEffectEditor {
    private final Supplier<AuraSettings> settings;
    private final Consumer<AuraSettings> update;
    private final Runnable rebuild;
    private final String[] raw = new String[2];
    private boolean opening = true;
    private boolean details;
    private String query = "";
    private String status = "";
    private long nextTest;
    private int offset;
    private List<String> entries = List.of();
    private final Button[] rows = new Button[4];
    private Button previous, next;
    private EditBox field;
    private Font font;
    private int x, y, width;

    AuraEffectEditor(Supplier<AuraSettings> settings, Consumer<AuraSettings> update, Runnable rebuild) {
        this.settings = settings; this.update = update; this.rebuild = rebuild;
        resetText();
    }
    void resetText() {
        raw[0] = settings.get().openEffect().parameters();
        raw[1] = settings.get().closeEffect().parameters();
        status = "";
    }
    private int index() { return opening ? 0 : 1; }
    private AuraEffect selected() { return settings.get().effect(opening); }
    private static Component text(String key) { return Component.translatable("screen.trueaura.effects." + key); }
    private Component shortName(String id, int pixels) {
        String name = id.equals(AuraEffect.NONE.id()) ? text("none").getString()
            : id.equals(AuraEffect.BURST.id()) ? text("original").getString() : id.substring(id.indexOf(':') + 1).replace('_', ' ');
        return Component.literal(font.plainSubstrByWidth(name, pixels));
    }
    void build(Font font, int x, int panelY, int width, Consumer<AbstractWidget> add) {
        this.font = font; this.x = x; this.y = panelY; this.width = width;
        int half = (width - 4) / 2;
        Button on = Button.builder(text("opening"), button -> chooseSide(true)).bounds(x, y + 64, half, 18).build();
        Button off = Button.builder(text("closing"), button -> chooseSide(false)).bounds(x + half + 4, y + 64, width - half - 4, 18).build();
        on.active = !opening; off.active = opening;
        on.setTooltip(Tooltip.create(Component.literal(settings.get().openEffect().id())));
        off.setTooltip(Tooltip.create(Component.literal(settings.get().closeEffect().id())));
        add.accept(on); add.accept(off);
        if (details) {
            field = new EditBox(font, x, y + 113, width, 18, text("parameters"));
            field.setMaxLength(AuraEffect.MAX_PARAMETER_BYTES);
            field.setValue(raw[index()]);
            field.setEditable(!selected().builtin());
            field.setResponder(value -> {
                raw[index()] = value;
                status = "";
                if (AuraEffect.validParameters(value)) update.accept(settings.get().withEffect(opening, new AuraEffect(selected().id(), value)));
            });
            field.setTooltip(Tooltip.create(text("parameters_hint")));
            add.accept(field);
            add.accept(Button.builder(text("test"), button -> test()).bounds(x, y + 184, half, 18).build());
            add.accept(Button.builder(text("back"), button -> { details = false; rebuild.run(); })
                .bounds(x + half + 4, y + 184, width - half - 4, 18).build());
            return;
        }
        field = new EditBox(font, x, y + 88, width, 18, text("search"));
        field.setMaxLength(128);
        field.setHint(text("search"));
        field.setValue(query);
        field.setResponder(value -> { query = value; offset = 0; refreshRows(); });
        add.accept(field);
        for (int i = 0; i < rows.length; i++) {
            int row = i;
            rows[i] = Button.builder(Component.empty(), button -> selectRow(row)).bounds(x, y + 110 + i * 18, width, 17).build();
            add.accept(rows[i]);
        }
        previous = Button.builder(Component.literal("<"), button -> page(-1)).bounds(x, y + 184, 20, 18).build();
        next = Button.builder(Component.literal(">"), button -> page(1)).bounds(x + 22, y + 184, 20, 18).build();
        add.accept(previous); add.accept(next);
        add.accept(Button.builder(Component.literal("…"), button -> { details = true; status = ""; rebuild.run(); })
            .bounds(x + 44, y + 184, 22, 18).tooltip(Tooltip.create(text("parameters"))).build());
        add.accept(Button.builder(text("test"), button -> test()).bounds(x + 70, y + 184, width - 70, 18).build());
        refreshRows();
    }
    private void chooseSide(boolean open) {
        opening = open; status = ""; offset = 0; rebuild.run();
    }
    private void refreshRows() {
        entries = AuraParticleCatalog.entries(query);
        offset = Math.max(0, Math.min(offset, Math.max(0, ((entries.size() - 1) / 4) * 4)));
        for (int i = 0; i < rows.length; i++) {
            int at = offset + i;
            rows[i].visible = at < entries.size();
            if (at >= entries.size()) continue;
            String id = entries.get(at);
            String restriction = AuraParticleCatalog.restriction(id);
            rows[i].setMessage(Component.literal(id.equals(selected().id()) ? "✓ " : "").append(shortName(id, width - 18)));
            rows[i].active = restriction.isEmpty();
            rows[i].setTooltip(Tooltip.create(Component.literal(id).append(restriction.isEmpty() ? Component.empty() : Component.literal("\n").append(text(restriction)))));
        }
        previous.active = offset > 0;
        next.active = offset + rows.length < entries.size();
        Component pages = Component.literal((offset / 4 + 1) + " / " + Math.max(1, (entries.size() + 3) / 4));
        previous.setTooltip(Tooltip.create(pages)); next.setTooltip(Tooltip.create(pages));
    }
    private void selectRow(int row) {
        int at = offset + row;
        if (at >= entries.size()) return;
        String id = entries.get(at);
        // Clicking a current choice must not discard its custom arguments.
        if (!id.equals(selected().id())) {
            String parameters = AuraParticleCatalog.defaultParameters(id);
            update.accept(settings.get().withEffect(opening, new AuraEffect(id, parameters)));
            raw[index()] = parameters;
        }
        status = "";
        if (!AuraParticleCatalog.problem(selected()).isEmpty()) { details = true; rebuild.run(); }
        else refreshRows();
    }
    private void page(int direction) { offset += direction * rows.length; refreshRows(); }
    boolean scroll(double amount) {
        if (details) return false;
        page(amount < 0 ? 1 : -1); return true;
    }
    boolean valid() {
        for (int i = 0; i < 2; i++) {
            if (!AuraEffect.validParameters(raw[i])) return false;
            String problem = AuraParticleCatalog.problem(settings.get().effect(i == 0));
            // Keep an existing selection if its mod was removed; playback has an explicit fallback.
            if (!problem.isEmpty() && !problem.equals("missing_mod")) return false;
        }
        return true;
    }
    private void test() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) status = "world_required";
        else if (!valid()) status = "invalid_parameters";
        else if (System.nanoTime() < nextTest) status = "cooldown";
        else {
            nextTest = System.nanoTime() + 500_000_000L;
            status = AuraBurstEffects.start(mc.player, settings.get(), opening);
            if (status.isEmpty()) status = "tested";
        }
        // Show the complete selected ID and feedback; the world preview stays visible.
        details = true; rebuild.run();
    }
    void tick() { if (field != null) field.tick(); }
    void render(GuiGraphics graphics) {
        if (!details) return;
        graphics.drawString(font, shortName(selected().id(), width), x, y + 88, 0xFFE5B3);
        graphics.drawString(font, text("parameters"), x, y + 101, 0xB5C5DF);
        String message = status;
        if (message.isEmpty()) message = AuraParticleCatalog.problem(selected());
        if (message.isEmpty()) message = "parameters_hint";
        graphics.drawWordWrap(font, text(message), x, y + 137, width, message.equals("parameters_hint") || message.equals("tested") ? 0xB5C5DF : 0xFFBB77);
    }
}
