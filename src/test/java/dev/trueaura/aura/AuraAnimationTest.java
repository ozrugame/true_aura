package dev.trueaura.aura;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuraAnimationTest {
    @Test void boundsRejectNonfiniteMotionAndClampDurations() {
        var a=new AuraAnimation(true,false,-5,10000,Double.NaN);
        assertEquals(1,a.appearTicks()); assertEquals(100,a.disappearTicks()); assertEquals(1,a.motionStrength());
        assertEquals(0,a.withMotion(-1).motionStrength()); assertEquals(2,a.withMotion(100).motionStrength());
    }
    @Test void allUnrelatedEditsKeepAnimationPreferences() {
        var animation=new AuraAnimation(true,false,32,21,.4);
        var original=AuraSettings.DEFAULT.withAnimation(animation);
        assertEquals(animation,new AuraLimits(2,2).apply(original).animation());
        assertEquals(animation,original.withColors(1,2,3).withShape(AuraShape.LOTUS).withCustom(null)
            .withEffect(true,AuraEffect.NONE).withAppearance(2,3,2,AuraShape.FLOW).animation());
    }
}
