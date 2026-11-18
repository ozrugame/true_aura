package dev.trueaura.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraColorTest {
    @Test void hueStripHasExpectedPrimaryAndSecondaryColors() {
        int[] colors = {0xFF0000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0000FF, 0xFF00FF, 0xFF0000};
        for (int i = 0; i < colors.length; i++) assertEquals(colors[i], AuraColor.rgb(i / 6.0, 1, 1));
    }
    @Test void paletteCornersAndOutOfRangeDragsAreSafe() {
        assertEquals(0xFFFFFF, AuraColor.rgb(.5, 0, 1));
        assertEquals(0, AuraColor.rgb(.5, 1, 0));
        assertEquals(0x00FFFF, AuraColor.rgb(.5, 2, 2));
        assertEquals(0, AuraColor.rgb(Double.NaN, Double.NaN, -2));
    }
    @Test void openingPickerPreservesExistingColorsExactly() {
        for (int rgb : new int[]{0, 0xFFFFFF, 0xFFB30F, 0xB080FF, 0x37CFFF, 0xFF477F, 0x808080, 0x012345}) {
            AuraColor.Hsv hsv = AuraColor.hsv(rgb);
            assertEquals(rgb, AuraColor.rgb(hsv.hue(), hsv.saturation(), hsv.value()));
        }
    }
}
