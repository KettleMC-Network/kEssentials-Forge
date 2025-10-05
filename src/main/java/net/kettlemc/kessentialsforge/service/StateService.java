
package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson; import com.google.gson.GsonBuilder; import java.io.Reader; import java.io.Writer; import java.nio.file.*; import java.util.*;

public class StateService {
    private final Path file; private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Set<java.util.UUID> god=new HashSet<>(), vanish=new HashSet<>(), socialSpy=new HashSet<>(), tpaBlocked=new HashSet<>(), frozen=new HashSet<>(), muted=new HashSet<>();
    private final Map<java.util.UUID, Long> lastSeen = new HashMap<>();
    public StateService(Path dataDir){ this.file = dataDir.resolve("state.json"); }
    public void load(){ try (Reader r=Files.newBufferedReader(file)) { Map raw=gson.fromJson(r, Map.class); if(raw!=null){ god.addAll(readSet(raw.get("god"))); vanish.addAll(readSet(raw.get("vanish"))); socialSpy.addAll(readSet(raw.get("socialSpy"))); tpaBlocked.addAll(readSet(raw.get("tpaBlocked"))); frozen.addAll(readSet(raw.get("frozen"))); muted.addAll(readSet(raw.get("muted"))); } } catch(Exception ignored){} }
    private Set<java.util.UUID> readSet(Object o){ Set<java.util.UUID> out=new HashSet<>(); if(o instanceof java.util.List list) for(Object s:list){ try{ out.add(java.util.UUID.fromString(String.valueOf(s))); }catch(Exception ignored){} } return out; }
    public void save(){ try (Writer w=Files.newBufferedWriter(file)) { Map<String,Object> raw=new HashMap<>(); raw.put("god",god); raw.put("vanish",vanish); raw.put("socialSpy",socialSpy); raw.put("tpaBlocked",tpaBlocked); raw.put("frozen",frozen); raw.put("muted",muted); gson.toJson(raw, w);} catch(Exception ignored){} }
    public boolean toggleGod(java.util.UUID id){ return toggle(id, god);} public boolean isGod(java.util.UUID id){ return god.contains(id); }
    public boolean toggleVanish(java.util.UUID id){ return toggle(id, vanish);} public boolean isVanished(java.util.UUID id){ return vanish.contains(id); }
    public boolean toggleSpy(java.util.UUID id){ return toggle(id, socialSpy);} public boolean isSpy(java.util.UUID id){ return socialSpy.contains(id); }
    public boolean toggleTpaBlocked(java.util.UUID id){ return toggle(id, tpaBlocked);} public boolean isTpaBlocked(java.util.UUID id){ return tpaBlocked.contains(id); }
    public boolean toggleFrozen(java.util.UUID id){
        boolean result = toggle(id, frozen);
        save();
        return result;
    }
    public boolean isFrozen(java.util.UUID id){ return frozen.contains(id); }
    public boolean toggleMuted(java.util.UUID id){ return toggle(id, muted);} public boolean isMuted(java.util.UUID id){ return muted.contains(id); }
    public void setSeen(java.util.UUID id, long ts){ lastSeen.put(id, ts);} public Long getSeen(java.util.UUID id){ return lastSeen.get(id); }
    public void tick(){}
    private boolean toggle(java.util.UUID id, Set<java.util.UUID> set){ if(set.contains(id)){ set.remove(id); return false; } set.add(id); return true; }
}
