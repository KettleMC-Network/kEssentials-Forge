
package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson; import com.google.gson.GsonBuilder; import java.io.Reader; import java.io.Writer; import java.nio.file.*;

public class SpawnService {
    private final Path file; private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static class Spawn { public String dim; public double x,y,z; public float yaw,pitch; }
    private Spawn current;
    public SpawnService(Path dataDir){ this.file = dataDir.resolve("spawn.json"); }
    public void load(){ try (Reader r=Files.newBufferedReader(file)) { current=gson.fromJson(r, Spawn.class);} catch(Exception ignored){} }
    public void save(){ try (Writer w=Files.newBufferedWriter(file)) { gson.toJson(current, w);} catch(Exception ignored){} }
    public void set(Spawn s){ current=s; save(); }
    public Spawn get(){ return current; }
}
