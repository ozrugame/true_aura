package dev.trueaura.aura;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Versioned data only: bounded geometry, no code, paths, textures or external references. */
public record CustomAura(String id, String name, AuraShape pattern, List<Layer> layers) {
    public static final int MAX_BYTES = 8192;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public record Point(double y, double radius) {
        public Point { range(y,0,1,"profile y"); range(radius,0,2,"profile radius"); }
    }
    public record Layer(double height, double offset, List<Point> profile, double amplitude,
                        int waves, double speed, double twist, int role) {
        public Layer {
            range(height,.1,3,"height"); range(offset,0,2,"offset");
            range(amplitude,0,.5,"amplitude"); range(waves,1,16,"waves");
            range(speed,-8,8,"speed"); range(twist,-3,3,"twist"); range(role,0,2,"role");
            profile = List.copyOf(profile);
            if (profile.size()<2 || profile.size()>12 || profile.get(0).y()!=0 || profile.get(profile.size()-1).y()!=1)
                throw bad("profile needs 2–12 points, from y=0 to y=1");
            for (int i=1;i<profile.size();i++) if (profile.get(i).y()<=profile.get(i-1).y())
                throw bad("profile y values must increase strictly");
        }
    }
    public CustomAura {
        if (id==null || id.length()>96 || !id.matches("[a-z0-9_-]+:[a-z0-9_-]+")) throw bad("id must be namespace:name (lowercase)");
        if (name==null || name.isBlank() || name.getBytes(StandardCharsets.UTF_8).length>128
                || name.codePoints().anyMatch(c -> Character.isISOControl(c) || c==0xA7 || Character.getType(c)==Character.FORMAT))
            throw bad("name must be 1–128 UTF-8 bytes without formatting/control characters");
        Objects.requireNonNull(pattern,"pattern");
        layers=List.copyOf(layers);
        if (layers.isEmpty() || layers.size()>4) throw bad("layers must contain 1–4 surfaces");
    }
    public double maxHeight(double playerHeight) {
        return layers.stream().mapToDouble(l -> l.offset()+l.height()*playerHeight).max().orElse(playerHeight);
    }
    public static CustomAura parse(String json) {
        if (json==null || json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES) throw bad("file exceeds 8192 UTF-8 bytes");
        try (var reader=new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            JsonObject root=read(reader,0).getAsJsonObject();
            if (reader.peek()!=JsonToken.END_DOCUMENT) throw bad("trailing JSON data");
            keys(root,"version","id","name","pattern","layers");
            if (number(root,"version",-1)!=1) throw bad("unsupported version (expected 1)");
            AuraShape pattern=AuraShape.valueOf(string(root,"pattern").toUpperCase(Locale.ROOT));
            var layers=new ArrayList<Layer>();
            for (JsonElement entry:root.getAsJsonArray("layers")) {
                var layer=entry.getAsJsonObject();
                keys(layer,"height","offset","profile","amplitude","waves","speed","twist","role");
                var points=new ArrayList<Point>();
                for (JsonElement pair:layer.getAsJsonArray("profile")) {
                    var values=pair.getAsJsonArray();
                    if (values.size()!=2) throw bad("profile points must be [y, radius]");
                    points.add(new Point(numeric(values.get(0)),numeric(values.get(1))));
                }
                layers.add(new Layer(number(layer,"height",1.4),number(layer,"offset",0),points,
                    number(layer,"amplitude",.08),integer(layer,"waves",5),number(layer,"speed",3),
                    number(layer,"twist",0),integer(layer,"role",0)));
            }
            var result=new CustomAura(string(root,"id"),string(root,"name"),pattern,layers);
            if (result.toJson().getBytes(StandardCharsets.UTF_8).length>MAX_BYTES) throw bad("expanded definition exceeds 8192 bytes");
            return result;
        } catch (Exception ex) {
            throw bad("Invalid aura: "+(ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage().replace("Use JsonReader.setLenient(true) to accept malformed JSON", "Invalid JSON syntax")));
        }
    }
    // Strict, bounded reader also rejects duplicate keys and nesting before Gson builds a tree.
    private static JsonElement read(JsonReader reader,int depth) throws java.io.IOException {
        if (depth>8) throw bad("JSON nesting is too deep");
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                var object=new JsonObject(); reader.beginObject();
                while(reader.hasNext()) { String key=reader.nextName(); if(object.has(key)) throw bad("duplicate field: "+key); object.add(key,read(reader,depth+1)); }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                var array=new JsonArray(); reader.beginArray();
                while(reader.hasNext()) array.add(read(reader,depth+1));
                reader.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new java.math.BigDecimal(reader.nextString()));
            default -> throw bad("expected object, array, string or number");
        };
    }
    public String toJson() {
        var root=new JsonObject(); root.addProperty("version",1); root.addProperty("id",id);
        root.addProperty("name",name); root.addProperty("pattern",pattern.name().toLowerCase(Locale.ROOT));
        var array=new JsonArray();
        for (var layer:layers) {
            var entry=new JsonObject(); entry.addProperty("height",layer.height()); entry.addProperty("offset",layer.offset());
            var profile=new JsonArray();
            for(var point:layer.profile()) { var pair=new JsonArray(); pair.add(point.y()); pair.add(point.radius()); profile.add(pair); }
            entry.add("profile",profile); entry.addProperty("amplitude",layer.amplitude()); entry.addProperty("waves",layer.waves());
            entry.addProperty("speed",layer.speed()); entry.addProperty("twist",layer.twist()); entry.addProperty("role",layer.role()); array.add(entry);
        }
        root.add("layers",array); return GSON.toJson(root);
    }
    private static String string(JsonObject object,String key) {
        var value=object.get(key);
        if(value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw bad(key+" must be a string");
        return value.getAsString();
    }
    private static double numeric(JsonElement value) {
        if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw bad("expected numeric value");
        double result=value.getAsDouble(); if(!Double.isFinite(result)) throw bad("numbers must be finite"); return result;
    }
    private static double number(JsonObject object,String key,double fallback) { return object.has(key)?numeric(object.get(key)):fallback; }
    private static int integer(JsonObject object,String key,int fallback) {
        double n=number(object,key,fallback); if(n!=Math.rint(n) || n<Integer.MIN_VALUE || n>Integer.MAX_VALUE) throw bad(key+" must be an integer"); return (int)n;
    }
    private static void keys(JsonObject object,String... allowed) {
        var names=Set.of(allowed); for(String key:object.keySet()) if(!names.contains(key)) throw bad("unknown field: "+key);
    }
    private static void range(double value,double min,double max,String field) {
        if(!Double.isFinite(value) || value<min || value>max) throw bad(field+" must be between "+min+" and "+max);
    }
    private static IllegalArgumentException bad(String message) { return new IllegalArgumentException(message); }
}
