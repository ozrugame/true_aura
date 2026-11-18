package dev.trueaura.client;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class CustomAuraLibraryTest {
    @TempDir Path directory;
    private static final String JSON="""
        {"version":1,"id":"test:one","name":"One","pattern":"flame","layers":[{"profile":[[0,0.4],[1,0.1]]}]}
        """;
    @Test void malformedOversizedDuplicateAndSymlinkFilesDoNotHideValidImports() throws Exception {
        Files.writeString(directory.resolve("a.json"),JSON);
        Files.writeString(directory.resolve("b.json"),JSON);
        Files.writeString(directory.resolve("c.json"),"{broken");
        Files.writeString(directory.resolve("d.json")," ".repeat(8193));
        Files.createSymbolicLink(directory.resolve("e.json"),directory.resolve("a.json"));
        var result=CustomAuraLibrary.load(directory);
        assertEquals(1,result.entries().size()); assertEquals(3,result.errors().size());
        Files.writeString(directory.resolve("a.json"),JSON.replace("\"One\"","\"Revised\""));
        assertEquals("Revised",CustomAuraLibrary.load(directory).entries().get(0).name());
    }
    @Test void bundledExamplesHaveUniqueIdsAndValidProfiles() throws Exception {
        var ids=new java.util.HashSet<String>();
        try(var files=Files.list(Path.of("examples/auras"))) {
            for(var file:files.toList()) {
                var def=dev.trueaura.aura.CustomAura.parse(Files.readString(file));
                assertTrue(ids.add(def.id()));
                assertEquals(Files.readString(file),Files.readString(Path.of("src/main/resources/assets/trueaura/examples").resolve(file.getFileName())));
            }
        }
        assertEquals(3,ids.size());
    }
}
