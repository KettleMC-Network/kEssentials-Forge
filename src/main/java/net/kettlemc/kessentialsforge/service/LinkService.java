
package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson; import com.google.gson.GsonBuilder; import java.io.Reader; import java.io.Writer; import java.nio.file.*; import java.security.SecureRandom; import java.util.*;

public class LinkService {
    private final Path file; private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, java.util.UUID> pending = new HashMap<>(); private final Map<Long, java.util.UUID> linked = new HashMap<>();
    private final SecureRandom rnd = new SecureRandom();
    public LinkService(Path dataDir){ this.file = dataDir.resolve("links.json"); }
    public void load(){ try (Reader r=Files.newBufferedReader(file)) { LinkService s=gson.fromJson(r, LinkService.class); if(s!=null){ this.linked.clear(); if(s.linked!=null) this.linked.putAll(s.linked);} } catch(Exception ignored){} save(); }
    public void save(){ try (Writer w=Files.newBufferedWriter(file)) { gson.toJson(this, w);} catch(Exception ignored){} }
    public String createCode(java.util.UUID player){ String code; do{ code=Integer.toHexString(rnd.nextInt(0xFFFFF)).toUpperCase(); } while(pending.containsKey(code)); pending.put(code, player); return code; }
    public boolean claim(long discordId, String code){ java.util.UUID id=pending.remove(code); if(id==null) return false; linked.put(discordId, id); save(); return true; }
    public java.util.UUID getLinked(long discordId){ return linked.get(discordId); }
}
