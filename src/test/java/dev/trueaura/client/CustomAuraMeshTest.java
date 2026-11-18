package dev.trueaura.client;
import dev.trueaura.aura.CustomAura;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CustomAuraMeshTest {
    @Test void narrowProfileFeaturesRemainVisibleAtBothDetailLevels() {
        var def=CustomAura.parse("""
            {"version":1,"id":"test:narrow","name":"Narrow","pattern":"pulse",
             "layers":[{"profile":[[0,0],[0.51,0],[0.52,1],[0.53,0],[1,0]],"amplitude":0}]}
            """);
        for(boolean distant:new boolean[]{false,true}) {
            double[] radius={0}; int[] count={0};
            CustomAuraMesh.emit(0,1.8f,def,distant,(x,y,z,u,v,l) -> { radius[0]=Math.max(radius[0],Math.hypot(x,z)); count[0]++; });
            assertTrue(radius[0]>.99,"narrow crest lost"); assertTrue(count[0]<=32*26*4);
        }
    }

    @Test void emitsBoundedAnimatedGeometryWithClosedSeam() {
        var def = CustomAura.parse("""
            {"version":1,"id":"test:ring","name":"Ring","pattern":"flow",
             "layers":[{"profile":[[0,0.4],[0.5,0.8],[1,0.2]],"amplitude":0.2,"waves":5}]}
            """);
        var a = new java.util.ArrayList<float[]>();
        var b = new java.util.ArrayList<float[]>();
        CustomAuraMesh.emit(0, 1.8f, def, false, (x,y,z,u,v,l) -> a.add(new float[]{x,y,z,u,v,l}));
        CustomAuraMesh.emit(1, 1.8f, def, false, (x,y,z,u,v,l) -> b.add(new float[]{x,y,z,u,v,l}));
        assertEquals(32*16*4, a.size());
        assertNotEquals(a.get(2)[0], b.get(2)[0]);
        for (var point : a) for (float value : point) assertTrue(Float.isFinite(value));
        var first = a.get(0); var seam = a.get(31*16*4+1);
        assertEquals(first[0], seam[0], 1e-5); assertEquals(first[2], seam[2], 1e-5);
    }
}
