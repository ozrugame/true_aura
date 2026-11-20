package dev.trueaura.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraCommandsTest {
    private static CommandSourceStack source(int permission) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, permission,
            "test", Component.literal("test"), null, null);
    }
    private static CommandDispatcher<CommandSourceStack> dispatcher() {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        AuraCommands.register(dispatcher);
        return dispatcher;
    }
    @Test void ordinaryPlayersCanParseEverySelfActionButCannotAccessTargetBranch() {
        var dispatcher = dispatcher();
        var player = source(0);
        for (String command : new String[]{"on", "off", "toggle", "status", "reset", "set color main gold",
            "set color highlight FFFFFF", "set color base pink", "set intensity 5", "set size 4", "set size 0.5",
            "set appear instant", "set appear rise", "set disappear fade", "set fade_in 1.5", "set fade_out 0.05", "set motion 0", "set speed 3", "set shape lotus", "set copy_shape Steve", "set open_effect epicfight:ground_slam",
            "set close_effect minecraft:dust 1 0 0 1"}) {
            for (String prefix : new String[]{"aura ", "aura self "}) {
                var parsed = dispatcher.parse(prefix + command, player);
                assertFalse(parsed.getReader().canRead(), prefix + command);
                assertTrue(parsed.getExceptions().isEmpty(), prefix + command);
                assertNotNull(parsed.getContext().getCommand(), prefix + command);
            }
        }
        assertTrue(dispatcher.parse("aura player Steve on", player).getReader().canRead());
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("aura player Steve on", player));
    }
    @Test void operatorAndConsoleCanTargetNamedPlayers() {
        var dispatcher = dispatcher();
        for (String target : new String[]{"Steve", "Alex"}) {
            var parsed = dispatcher.parse("aura player " + target + " set shape inferno", source(2));
            assertFalse(parsed.getReader().canRead());
            assertTrue(parsed.getExceptions().isEmpty());
            assertNotNull(parsed.getContext().getCommand());
        }
        assertTrue(dispatcher.parse("aura player Steve on", source(1)).getReader().canRead());
    }
    @Test void outOfRangeAndNonNumericValuesAreRejected() {
        var dispatcher = dispatcher();
        for (String arg : new String[]{"fade_in 0", "fade_out 5.1", "motion 2.1", "motion NaN", "intensity 0", "intensity 33", "size .4", "size 33", "speed 0", "speed 4", "speed NaN", "speed Infinity"}) {
            var parsed = dispatcher.parse("aura set " + arg, source(0));
            assertFalse(parsed.getExceptions().isEmpty(), arg);
        }
    }
    @Test void colorNamesAndHexWorkButFormattingOrMalformedValuesDoNot() throws Exception {
        assertEquals(0xFFAA00, AuraCommands.parseColor("gold"));
        assertEquals(0xFF66CC, AuraCommands.parseColor("pink"));
        assertEquals(0xABCDEF, AuraCommands.parseColor("abcdef"));
        assertEquals(0xABCDEF, AuraCommands.parseColor("#ABCDEF"));
        for (String invalid : new String[]{"bold", "reset", "123", "GGGGGG", "not_a_color"}) {
            assertThrows(CommandSyntaxException.class, () -> AuraCommands.parseColor(invalid));
        }
    }
    @Test void shapeCompletionIncludesAllTenNames() throws Exception {
        var dispatcher = dispatcher();
        var result = dispatcher.getCompletionSuggestions(dispatcher.parse("aura set shape ", source(0))).get();
        assertEquals(10, result.getList().size());
        assertTrue(result.getList().stream().anyMatch(s -> s.getText().equals("lotus")));
    }
}
