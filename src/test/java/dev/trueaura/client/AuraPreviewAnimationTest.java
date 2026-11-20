package dev.trueaura.client;
import dev.trueaura.aura.AuraAnimation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuraPreviewAnimationTest {
    @Test void replayIncludesAppearanceAndDisappearanceThenRestoresNormalPreview() {
        var preview=new AuraPreviewAnimation();
        preview.start(new AuraAnimation(false,false,20,10,1));
        assertEquals(0,preview.opacity(0));
        for(int i=0;i<10;i++) preview.tick(); assertEquals(.5,preview.opacity(1),1e-5);
        for(int i=0;i<30;i++) preview.tick(); assertEquals(0,preview.opacity(1),1e-5);
        for(int i=0;i<20;i++) preview.tick(); assertFalse(preview.playing()); assertEquals(1,preview.opacity(1));
    }
    @Test void instantReplayAndCancellationDoNotTouchAnotherState() {
        var player=new AuraState(); player.setEnabled(false);
        var preview=new AuraPreviewAnimation(); preview.start(new AuraAnimation(true,true,10,10,1));
        assertEquals(1,preview.opacity(0)); preview.clear();
        assertFalse(player.enabled()); assertEquals(1,preview.opacity(0));
    }
}
