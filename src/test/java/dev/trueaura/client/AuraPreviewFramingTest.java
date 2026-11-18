package dev.trueaura.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraPreviewFramingTest {
    @Test void stableOrbitProjectsSubjectToFreeAreaAtEveryBodyHeading() {
        for (int[] screen : new int[][]{{480, 300, 262}, {854, 480, 628}, {320, 240, 164}}) {
            for (double heading : new double[]{0, 90, 180, -55}) {
                var orbit = AuraPreviewFraming.orbit(heading, screen[0], screen[1], screen[2], 70);
                // Subject at origin: apply vanilla Y(yaw + 180), then perspective.
                double yaw = Math.toRadians(orbit.yaw() + 180);
                double viewX = -orbit.x() * Math.cos(yaw) - orbit.z() * Math.sin(yaw);
                double viewZ = orbit.x() * Math.sin(yaw) - orbit.z() * Math.cos(yaw);
                double tanH = Math.tan(Math.toRadians(35)) * screen[0] / screen[1];
                assertEquals(screen[2] / 2.0, (viewX / -viewZ / tanH + 1) * screen[0] / 2.0, .001);
                assertTrue(viewZ < 0, "Subject remains in front of preview camera");
            }
        }
    }

    @Test void collisionShorteningPreservesFramingAndPitchIndependence() {
        // Orbit depends on body yaw alone; looking straight up/down cannot collapse its distance.
        var orbit = AuraPreviewFraming.orbit(0, 480, 300, 262, 70);
        assertEquals(4, orbit.z(), .0001);
        for (double fraction : new double[]{1, .7, .25}) {
            double x = orbit.x() * fraction, depth = orbit.z() * fraction;
            double pixel = (-x / depth / (Math.tan(Math.toRadians(35)) * 1.6) + 1) * 240;
            assertEquals(131, pixel, .001);
        }
    }
    @Test void largeAuraMovesCameraBackWithoutChangingHorizontalFraming() {
        var near = AuraPreviewFraming.orbit(37, 854, 480, 628, 70, 4);
        var far = AuraPreviewFraming.orbit(37, 854, 480, 628, 70, 32);
        assertEquals(near.x() * 8, far.x(), .0001);
        assertEquals(near.z() * 8, far.z(), .0001);
        assertEquals(near.yaw(), far.yaw());
    }
}
