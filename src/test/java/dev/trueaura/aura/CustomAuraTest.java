package dev.trueaura.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomAuraTest {
    static final String JSON = """
        {"version":1,"id":"example:petals","name":"กลีบพลัง","pattern":"lotus",
         "layers":[{"height":1.5,"profile":[[0,0.3],[0.5,0.9],[1,0.1]],"waves":6,"amplitude":0.2}]}
        """;
    @Test void canonicalDefinitionRoundTripsAndSurvivesAppearanceEdits() {
        var custom = CustomAura.parse(JSON);
        assertEquals(custom, CustomAura.parse(custom.toJson()));
        var settings = AuraSettings.DEFAULT.withCustom(custom);
        assertEquals(custom, settings.withColors(1,2,3).withEffect(true,AuraEffect.NONE)
            .withAppearance(5,4,2,settings.shape()).custom());
        assertEquals(custom, new AuraLimits(2,2).apply(settings).custom());
        assertNull(settings.withShape(AuraShape.FLAME).custom());
    }
    @Test void rejectsUnsafeAndUnsupportedDefinitions() {
        for (String json : new String[]{JSON.replace("\"version\":1","\"version\":2"),
                JSON.replace("1.5", "1e100"), JSON.replace("0.9", "-1"),
                JSON.replace("example:petals", "../../escape"), JSON.replace("lotus", "unknown"),
                JSON.replace("0.5,0.9", "0,0.9"), JSON.replace("\"waves\":6", "\"waves\":2.5"),
                JSON.replace("\"height\":1.5", "\"height\":1.5,\"typo\":1"),
                "[".repeat(200)+"]".repeat(200), " ".repeat(CustomAura.MAX_BYTES+1)}) {
            assertThrows(IllegalArgumentException.class, () -> CustomAura.parse(json), json);
        }
    }
    @Test void collectionsAreImmutableAndBadFileCannotBecomeEmptyAura() {
        var def = CustomAura.parse(JSON);
        assertThrows(UnsupportedOperationException.class, () -> def.layers().clear());
        assertThrows(IllegalArgumentException.class, () -> CustomAura.parse(JSON.replace("[[0,0.3],[0.5,0.9],[1,0.1]]","[]")));
        assertThrows(IllegalArgumentException.class, () -> CustomAura.parse("{}"));
    }
}
