
package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson; import com.google.gson.GsonBuilder; import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import java.io.Reader; import java.io.Writer; import java.nio.file.*; import java.util.*;

public class HomeService {
    private final Path file; private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static class Home { public String dim; public double x,y,z; public float yaw,pitch; }
    private final Map<UUID, Map<String, Home>> data = new HashMap<>();
    public HomeService(Path dataDir) { this.file = dataDir.resolve("homes.json"); }
    public void load(){ try (Reader r = Files.newBufferedReader(file)) { Map<String, Map<String, Home>> raw = gson.fromJson(r, new TypeToken<Map<String, Map<String, Home>>>(){}.getType()); if (raw!=null) raw.forEach((k,v)->{ try { UUID id = UUID.fromString(k); Map<String, Home> homes = new HashMap<>(); if(v!=null) v.forEach((name, home)->{ if(home!=null){ Home actual = home instanceof Home ? home : gson.fromJson(gson.toJsonTree(home), Home.class); homes.put(name.toLowerCase(Locale.ROOT), actual); }}); data.put(id, homes); } catch(IllegalArgumentException ignored1){} }); } catch (Exception ignored){} }
    public void save(){ try (Writer w = Files.newBufferedWriter(file)) { Map<String, Map<String, Home>> raw=new HashMap<>(); data.forEach((k,v)->raw.put(k.toString(), v)); gson.toJson(raw, w);} catch (Exception ignored){} }
    public void setHome(ServerPlayer p, String name){ Home h=new Home(); h.dim=p.getLevel().dimension().location().toString(); h.x=p.getX(); h.y=p.getY(); h.z=p.getZ(); h.yaw=p.getYRot(); h.pitch=p.getXRot(); data.computeIfAbsent(p.getUUID(), id->new HashMap<>()).put(name.toLowerCase(Locale.ROOT), h); save(); }
    public boolean delHome(UUID id, String name){ Map<String, Home> m=data.get(id); if(m==null) return false; Home removed = m.remove(name.toLowerCase(Locale.ROOT)); if(removed!=null){ if(m.isEmpty()) data.remove(id); save(); } return removed!=null; }
    public Home getHome(UUID id, String name){ return data.getOrDefault(id, Collections.emptyMap()).get(name.toLowerCase(Locale.ROOT)); }
    public Set<String> list(UUID id){ return data.getOrDefault(id, Collections.emptyMap()).keySet(); }
}
