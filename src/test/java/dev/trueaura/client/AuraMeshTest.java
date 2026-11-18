package dev.trueaura.client;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.trueaura.aura.AuraShape;

class AuraMeshTest {
    private record Vertex(float x, float y, float z, float u, float v, float layer) { }
    private List<Vertex> mesh(float time, float height) {
        List<Vertex> vertices = new ArrayList<>();
        AuraMesh.emit(time, height, (x, y, z, u, v, layer) -> vertices.add(new Vertex(x,y,z,u,v,layer)));
        return vertices;
    }

    @Test void enclosesPlayerWithFiniteGeometryAndNeverAccumulates() {
        int count = mesh(0, 1.8f).size();
        assertTrue(count > 100 && count <= 16384);
        for (float time : new float[]{0, 23, 1200, 72000}) {
            List<Vertex> points = mesh(time, 1.8f);
            assertEquals(count, points.size());
            for (Vertex p : points) {
                assertTrue(Float.isFinite(p.x) && Float.isFinite(p.y) && Float.isFinite(p.z));
                assertTrue(Math.abs(p.x) < 1.25 && Math.abs(p.z) < 1.25);
                assertTrue(p.y >= 0 && p.y < 3.5);
                assertTrue(p.u >= 0 && p.u <= 1 && p.v >= 0 && p.v <= 1);
            }
            assertTrue(points.stream().anyMatch(p -> p.x > 0.4));
            assertTrue(points.stream().anyMatch(p -> p.x < -0.4));
            assertTrue(points.stream().anyMatch(p -> p.z > 0.4));
            assertTrue(points.stream().anyMatch(p -> p.z < -0.4));
            assertTrue(points.stream().anyMatch(p -> p.y > 2.0));
        }
    }

    @Test void wrapSeamIsClosedAtEveryHeightAndLayer() {
        for (AuraShape shape : AuraShape.values()) {
            for (boolean distant : new boolean[]{false, true}) {
                List<Vertex> points = new ArrayList<>();
                AuraMesh.emit(1.25f, 1.8f, shape, distant, (x,y,z,u,v,l) -> points.add(new Vertex(x,y,z,u,v,l)));
                List<Vertex> start = points.stream().filter(p -> p.u == 0).toList();
                List<Vertex> end = points.stream().filter(p -> p.u == 1).toList();
                assertFalse(start.isEmpty());
                for (Vertex p : start) {
                    assertTrue(end.stream().anyMatch(q -> q.layer == p.layer && q.v == p.v
                        && Math.abs(q.x-p.x) < 0.00001 && Math.abs(q.y-p.y) < 0.00001
                        && Math.abs(q.z-p.z) < 0.00001), shape + " distant=" + distant);
                }
            }
        }
    }

    @Test void flamesMoveAndCrouchingLowersTheirEnvelope() {
        List<Vertex> first = mesh(0, 1.8f);
        assertNotEquals(first, mesh(0.4f, 1.8f));
        double standing = first.stream().mapToDouble(Vertex::y).max().orElseThrow();
        double crouched = mesh(0, 1.5f).stream().mapToDouble(Vertex::y).max().orElseThrow();
        assertTrue(crouched < standing - 0.15);
    }

    @Test void flameTipsHaveVisibleMotionOverHalfASecond() {
        List<Vertex> first = mesh(0, 1.8f);
        List<Vertex> later = mesh(0.5f, 1.8f);
        double maxTipTravel = 0;
        for (int i = 0; i < first.size(); i++) {
            Vertex a = first.get(i), b = later.get(i);
            if (a.layer != 2 && a.v > 0.85f) {
                double dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
                maxTipTravel = Math.max(maxTipTravel, Math.sqrt(dx*dx + dy*dy + dz*dz));
            }
        }
        // Sub-centimetre changes pass an inequality check but look static in game.
        assertTrue(maxTipTravel > 0.25, "Tips must move visibly, not merely change floating-point values");
    }

    @Test void allPresetsHaveDistinctAnimatedBoundedGeometry() {
        assertEquals(10, AuraShape.values().length);
        List<List<Vertex>> shapes = new ArrayList<>();
        for (AuraShape shape : AuraShape.values()) {
            List<Vertex> initial = new ArrayList<>(), later = new ArrayList<>();
            AuraMesh.emit(0, 1.8f, shape, (x,y,z,u,v,l) -> initial.add(new Vertex(x,y,z,u,v,l)));
            AuraMesh.emit(0.5f, 1.8f, shape, (x,y,z,u,v,l) -> later.add(new Vertex(x,y,z,u,v,l)));
            assertNotEquals(initial, later, shape.name());
            assertTrue(initial.size() > 100 && initial.size() <= 16384);
            for (Vertex p : later) {
                assertTrue(Float.isFinite(p.x) && Float.isFinite(p.y) && Float.isFinite(p.z));
                assertTrue(Math.abs(p.x) < 1.5 && Math.abs(p.z) < 1.5 && p.y >= 0 && p.y < 3.8);
            }
            shapes.add(initial);
        }
        for (int a = 0; a < shapes.size(); a++) {
            for (int b = a + 1; b < shapes.size(); b++) assertNotEquals(shapes.get(a), shapes.get(b));
        }
    }
}
