package dev.trueaura.client;

import dev.trueaura.aura.CustomAura;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraftforge.fml.loading.FMLPaths;

/** Local imports never write data received from other players to the imports folder. */
public final class CustomAuraLibrary {
    public record Report(List<CustomAura> entries, List<String> errors) {
        public Report { entries=List.copyOf(entries); errors=List.copyOf(errors); }
    }
    private static Report current=new Report(List.of(),List.of());
    public static Path folder() { return FMLPaths.CONFIGDIR.get().resolve("trueaura/auras"); }
    public static Report current() { return current; }
    public static Report reload() {
        var errors=new ArrayList<String>();
        try {
            boolean first=!Files.exists(folder()); Files.createDirectories(folder());
            if(first) for(String example:List.of("moon_petals.json","spiral_crown.json","quiet_shell.json")) {
                try(var stream=CustomAuraLibrary.class.getResourceAsStream("/assets/trueaura/examples/"+example)) {
                    if(stream!=null) Files.copy(stream,folder().resolve(example));
                }
            }
        } catch (Exception ex) { errors.add("Cannot create examples: "+ex.getMessage()); }
        var loaded=load(folder()); errors.addAll(loaded.errors());
        current=new Report(loaded.entries(),errors); return current;
    }
    /** Separate path-based loader so malformed files can be exercised without launching Minecraft. */
    public static Report load(Path directory) {
        var errors=new ArrayList<String>(); var entries=new ArrayList<CustomAura>(); var ids=new HashSet<String>();
        try(var files=Files.list(directory)) {
            var paths=files.filter(p -> p.getFileName().toString().endsWith(".json") && Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))
                .sorted().limit(129).toList();
            if(paths.size()>128) errors.add("Only the first 128 JSON files are loaded");
            for(Path path:paths.subList(0,Math.min(128,paths.size()))) {
                try(var input=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)) {
                    byte[] bytes=input.readNBytes(CustomAura.MAX_BYTES+1);
                    if(bytes.length>CustomAura.MAX_BYTES) throw new IllegalArgumentException("file exceeds 8192 bytes");
                    String json=StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
                    var definition=CustomAura.parse(json);
                    if(!ids.add(definition.id())) throw new IllegalArgumentException("duplicate id: "+definition.id());
                    entries.add(definition);
                } catch(Exception ex) { errors.add(path.getFileName()+": "+ex.getMessage()); }
            }
        } catch(Exception ex) { errors.add("Cannot read aura folder: "+ex.getMessage()); }
        return new Report(entries,errors);
    }
    private CustomAuraLibrary() {}
}
