package dev.trueaura.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuraRevealTest {
    @Test void startsAtFeetReachesFullHeightAndKeepsAngularSeamClosed() {
        assertEquals(0,AuraReveal.height(0,0,0,0));
        assertEquals(1,AuraReveal.height(1,.3,2,5));
        for(double p:new double[]{.1,.25,.5,.75}) {
            double h=AuraReveal.height(p,.3,0,5);
            assertTrue(h>0 && h<1); assertTrue(Math.abs(h-p)<.1);
            assertEquals(AuraReveal.height(p,0,1,5),AuraReveal.height(p,1,1,5),1e-6);
        }
        assertNotEquals(AuraReveal.height(.5,.1,0,0),AuraReveal.height(.5,.3,0,0));
    }
    @Test void fullyVisibleGeometryIsUnchangedAndEarlyFlamesStayLow() {
        float[] point=new float[3];
        AuraMesh.VertexSink sink=(x,y,z,u,v,l)->{point[0]=x;point[1]=y;point[2]=z;};
        AuraReveal.wrap(sink,1,3).vertex(.4f,3,.2f,.2f,1,0);
        assertArrayEquals(new float[]{.4f,3,.2f},point);
        AuraReveal.wrap(sink,.1f,3).vertex(.4f,3,.2f,.2f,1,0);
        assertTrue(point[1]>.1 && point[1]<.5);
        AuraReveal.wrap(sink,.5f,3).vertex(.4f,0,.2f,.2f,0,0);
        assertEquals(0,point[1]);
    }
}
