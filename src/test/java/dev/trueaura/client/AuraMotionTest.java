package dev.trueaura.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuraMotionTest {
    @Test void jumpingCannotInvertAShortImportedSurface() {
        var motion=new AuraMotion();motion.tick(0,0,0,0);
        double y=0;
        for(double dy:new double[]{.42,.3332,.248136,.16477328}) { y+=dy;motion.tick(0,y,0,0); }
        var sample=motion.sample(1);
        for(double height:new double[]{.18,.5,3,8}) for(double strength:new double[]{1,2}) {
            double previous=-1;
            for(int row=0;row<=100;row++) {
                float vy=(float)(height*row/100);
                var vertex=sample.deform(.4f,vy,0,height,0,strength);
                assertTrue(vertex[1]>=previous,"vertical profile folded at height "+height);
                assertTrue(vertex[1]>=0,"surface crossed below anchored feet");previous=vertex[1];
            }
        }
    }

    @Test void movementLagsThenSettlesAndTeleportResets() {
        var motion=new AuraMotion(); motion.tick(0,0,0,0);
        for(int i=1;i<=20;i++) motion.tick(i*.2,0,0,0);
        assertTrue(motion.sample(1).x()<-.1);
        assertTrue(motion.sample(1).energy()>0);
        for(int i=0;i<200;i++) motion.tick(4,0,0,0);
        assertEquals(0,motion.sample(1).x(),1e-5);
        assertEquals(0,motion.sample(1).energy(),1e-5);
        motion.tick(100,0,0,0);
        assertEquals(0,motion.sample(1).x()); assertEquals(0,motion.sample(1).energy());
    }
    @Test void turnsWrapAndMalformedInputCannotPoisonSpring() {
        var motion=new AuraMotion(); motion.tick(0,0,0,179); motion.tick(0,0,0,-179);
        assertTrue(Math.abs(motion.sample(1).twist())<.1);
        motion.tick(Double.NaN,0,0,0); motion.tick(1,1,1,0);
        assertEquals(0,motion.sample(1).energy());
        for(int i=0;i<1000;i++) {
            motion.tick(i%2,Math.sin(i),0,i*170);
            var s=motion.sample(.5f);
            assertTrue(Double.isFinite(s.x()+s.y()+s.z()+s.twist()+s.energy()));
            assertTrue(Math.abs(s.x())<=1 && Math.abs(s.y())<=1 && Math.abs(s.twist())<=1);
        }
    }
    @Test void deformationAnchorsFeetAndStrengthZeroIsExactIdentity() {
        var s=new AuraMotion.Sample(.5,.2,-.4,.3,1);
        var feet=s.deform(.4f,0,.2f,3,1,2);
        assertArrayEquals(new float[]{.4f,0,.2f},feet);
        var off=s.deform(.4f,2,.2f,3,1,0);
        assertArrayEquals(new float[]{.4f,2,.2f},off);
        var top=s.deform(.4f,2,.2f,3,1,1);
        assertNotEquals(.4f,top[0]);
    }
    @Test void samplingNeverAdvancesSimulation() {
        var motion=new AuraMotion(); motion.tick(0,0,0,0); motion.tick(.3,0,0,20);
        var before=motion.sample(.5f);
        for(int i=0;i<500;i++) assertEquals(before,motion.sample(.5f));
        assertEquals(motion.sample(0),motion.sample(-10)); assertEquals(motion.sample(1),motion.sample(10));
    }
}
