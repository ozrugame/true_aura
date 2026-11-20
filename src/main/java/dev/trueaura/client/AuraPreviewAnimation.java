package dev.trueaura.client;

import dev.trueaura.aura.AuraAnimation;

/** Isolated local opacity replay, never a player's enabled state. */
public final class AuraPreviewAnimation {
    private final AuraState state = new AuraState();
    private boolean playing;
    private int elapsed, holdUntil, finishAt;
    public void start(AuraAnimation settings) {
        state.reset(); state.configure(settings); state.setEnabled(true);
        elapsed=0; holdUntil=(settings.instantAppear()?1:settings.appearTicks())+10;
        finishAt=holdUntil+(settings.instantDisappear()?1:settings.disappearTicks())+10;
        playing=true;
    }
    public void tick() {
        if(!playing) return;
        if(++elapsed==holdUntil) state.setEnabled(false);
        state.tick(true);
        if(elapsed>=finishAt) clear();
    }
    public float opacity(float partial) { return playing?state.opacity(partial):1; }
    public boolean playing() { return playing; }
    public void clear() { playing=false; state.reset(); }
}
