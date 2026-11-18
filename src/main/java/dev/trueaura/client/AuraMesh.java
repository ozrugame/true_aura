package dev.trueaura.client;
import dev.trueaura.aura.AuraShape;

public final class AuraMesh {
    @FunctionalInterface
    public interface VertexSink {
        void vertex(float x, float y, float z, float u, float v, float layer);
    }
    public static void emit(float time, float height, VertexSink sink) {
        emit(time, height, AuraShape.FLAME, sink);
    }

    public static void emit(float time, float height, AuraShape shape, VertexSink sink) {
        emit(time, height, shape, false, sink);
    }

    public static void emit(float time, float height, AuraShape shape, boolean distant, VertexSink sink) {
        int sides = distant ? 24 : 48;
        int rows = distant ? 12 : 20;
        for (int layer = 0; layer < 3; layer++) {
            for (int side = 0; side < sides; side++) {
                float u0 = side / (float) sides;
                float u1 = (side + 1f) / sides;
                for (int row = 0; row < rows; row++) {
                    float v0 = row / (float) rows;
                    float v1 = (row + 1f) / rows;
                    point(time, height, u0, v0, layer, shape, sink);
                    point(time, height, u1, v0, layer, shape, sink);
                    point(time, height, u1, v1, layer, shape, sink);
                    point(time, height, u0, v1, layer, shape, sink);
                }
            }
        }
    }

    private static void point(float time, float height, float u, float v, int layer, AuraShape shape, VertexSink sink) {
        double angle = u * Math.PI * 2;
        // Waves travel up the body, rather than only nudging a rigid shell sideways.
        double wave = Math.sin(angle * 5 + v * 8 - time * 4.0)
            + 0.45 * Math.sin(angle * 9 + v * 13 - time * 5.2);
        double tips = 0.26 * Math.sin(angle * 7 - time * 4.2)
            + 0.12 * Math.sin(angle * 11 + time * 3.4);
        double radius;
        double y;
        if (layer == 2) {
            radius = 0.53 + 0.025 * wave + 0.04 * Math.sin(v * Math.PI);
            y = v * (0.48 + tips * 0.7);
        } else {
            double swell = Math.sin(Math.PI * v);
            radius = 0.43 + 0.21 * swell - 0.20 * Math.pow(v, 4)
                + wave * 0.075 * (0.15 + v);
            radius *= layer == 0 ? 1.12 : 0.97;
            y = v * (height + 0.68 + tips * Math.pow(v, 2));
            if (shape == AuraShape.FLOW) {
                double twist = Math.sin(angle * 3 + v * 7 - time * 2.4);
                radius = (0.44 + 0.14 * swell + 0.11 * twist * swell) * (layer == 0 ? 1.12 : 0.97);
                y = v * (height + 0.42 + 0.13 * Math.sin(angle * 3 - time * 2.4));
            } else if (shape == AuraShape.SPIKES) {
                double spike = Math.pow(0.5 + 0.5 * Math.cos(angle * 8 + time * 0.8), 3);
                radius += (0.23 * spike - 0.06) * Math.sin(v * Math.PI);
                y = v * (height + 0.35 + 0.7 * spike + 0.10 * Math.sin(v * 10 - time * 4));
            } else if (shape == AuraShape.VORTEX) {
                double spiral = Math.sin(angle * 3 - v * 10 + time * 4);
                radius = (0.40 + 0.30 * swell + 0.13 * spiral) * (layer == 0 ? 1.12 : 0.97);
                y = v * (height + 0.65 + 0.18 * Math.sin(angle * 3 + time * 4));
            } else if (shape == AuraShape.LIGHTNING) {
                double zigzag = (2 / Math.PI) * Math.asin(Math.sin(v * 26 + angle * 6 - time * 8));
                radius = (0.48 + 0.17 * swell + 0.12 * zigzag) * (layer == 0 ? 1.12 : 0.97);
                y = v * (height + 0.55 + 0.21 * Math.sin(angle * 6 - time * 8));
            } else if (shape == AuraShape.PULSE) {
                double pulse = Math.sin(v * 14 - time * 6);
                radius = (0.38 + 0.40 * swell + 0.12 * pulse * swell) * (layer == 0 ? 1.12 : 0.97);
                y = v * (height + 0.45 + 0.15 * Math.sin(time * 4));
            } else if (shape == AuraShape.INFERNO) {
                double fire = Math.sin(angle * 9 + v * 12 - time * 7);
                radius = (0.43 + .22 * swell - .18 * v * v + .09 * fire) * (layer == 0 ? 1.12 : .97);
                y = v * (height + 1.05 + .30 * Math.sin(angle * 9 - time * 5));
            } else if (shape == AuraShape.LOTUS) {
                double petal = .5 + .5 * Math.cos(angle * 6 + time * .8);
                radius = (.35 + (.38 + .23 * petal) * swell) * (layer == 0 ? 1.12 : .90);
                y = v * (height + .36 + .33 * petal);
            } else if (shape == AuraShape.BLADE) {
                double blade = Math.pow(.5 + .5 * Math.cos(angle * 4 - v * 3 + time * 2), 3);
                radius = (.36 + .44 * blade * swell + .08 * Math.sin(v * 8 - time * 3)) * (layer == 0 ? 1.12 : .95);
                y = v * (height + .65 + .25 * blade);
            } else if (shape == AuraShape.VEIL) {
                double curtain = Math.sin(angle * 5 + v * 5 + Math.sin(v * 9 - time * 3));
                radius = (.54 + .18 * swell + .14 * curtain) * (layer == 0 ? 1.12 : .95);
                y = v * (height + .30 + .18 * Math.sin(angle * 5 - time * 2));
            }
        }
        double lean = Math.pow(v, 3) * (layer == 2 ? 0.025 : 0.12);
        float x = (float) (Math.cos(angle) * radius + lean * Math.sin(v * 6 - time * 3.6));
        float z = (float) (Math.sin(angle) * radius + lean * Math.cos(v * 5 - time * 2.8));
        sink.vertex(x, (float) y, z, u, v, layer);
    }
    private AuraMesh() { }
}
