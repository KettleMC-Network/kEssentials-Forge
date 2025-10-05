
package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson; import com.google.gson.GsonBuilder; import java.io.Reader; import java.io.Writer; import java.nio.file.*; import java.util.*;

public class ChatService {
    private final Path file; private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    // `rtpMin` and `rtpMax` describe the inclusive range used for random teleports. When
    // `rtpMin` is greater than or equal to `rtpMax`, the command falls back to using only
    // the maximum value, mimicking the legacy behaviour that ignored the minimum.
    private long rtpMin=2000, rtpMax=6000; private String discordChannelId="";
    private final Map<java.util.UUID, java.util.UUID> lastMsg = new HashMap<>();
    public ChatService(Path dataDir){ this.file = dataDir.resolve("chat.json"); }
    public void load(){ try (Reader r=Files.newBufferedReader(file)) { ChatService s=gson.fromJson(r, ChatService.class); if(s!=null){ this.rtpMin=s.rtpMin; this.rtpMax=s.rtpMax; this.discordChannelId=s.discordChannelId; } } catch(Exception ignored){} save(); }
    public void save(){ try (Writer w=Files.newBufferedWriter(file)) { gson.toJson(this, w);} catch(Exception ignored){} }
    public long getRtpMin(){ return rtpMin; } public long getRtpMax(){ return rtpMax; } public String getDiscordChannelId(){ return discordChannelId; }
    public void setLastMsg(java.util.UUID from, java.util.UUID to){ lastMsg.put(from,to); lastMsg.put(to,from); } public java.util.UUID getLastMsg(java.util.UUID id){ return lastMsg.get(id); }
}
