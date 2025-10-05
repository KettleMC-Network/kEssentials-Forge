
package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson; import com.google.gson.GsonBuilder; import com.google.gson.reflect.TypeToken; import java.io.Reader; import java.io.Writer; import java.nio.file.*; import java.util.*;

public class WarpService {
    private final Path file; private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static class Warp { public String dim; public double x,y,z; public float yaw,pitch; }
    private final Map<String, Warp> warps = new HashMap<>();
    public WarpService(Path dataDir){ this.file = dataDir.resolve("warps.json"); }
    public void load(){ try (Reader r=Files.newBufferedReader(file)) { Map<String, Warp> raw=gson.fromJson(r, new TypeToken<Map<String, Warp>>(){}.getType()); if(raw!=null) raw.forEach((k,v)->{ if(v!=null){ Warp actual = v instanceof Warp ? v : gson.fromJson(gson.toJsonTree(v), Warp.class); warps.put(k.toLowerCase(Locale.ROOT), actual); }}); } catch(Exception ignored){} }
    public void save(){ try (Writer w=Files.newBufferedWriter(file)) { gson.toJson(warps, w);} catch(Exception ignored){} }
    public void set(String name, Warp w){ warps.put(name.toLowerCase(Locale.ROOT), w); save(); }
    public boolean del(String name){ boolean b = warps.remove(name.toLowerCase(Locale.ROOT))!=null; if(b) save(); return b; }
    public Warp get(String name){ return warps.get(name.toLowerCase(Locale.ROOT)); }
    public Set<String> list(){ return warps.keySet(); }
}
