
package net.kettlemc.kessentialsforge.service;

import net.minecraft.server.MinecraftServer; import net.minecraft.server.level.ServerPlayer;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;

public class CooldownService {
    private final Map<UUID, Map<String, Long>> lastUse = new ConcurrentHashMap<>();
    private final Map<UUID, Warmup> warmups = new ConcurrentHashMap<>();
    public static class Warmup { public UUID player; public String cmd; public long endAt; public Runnable action; public double startX, startY, startZ; }
    public boolean isOnCooldown(UUID id, String cmd, int cooldownSec) { long now=System.currentTimeMillis(); long until=lastUse.computeIfAbsent(id,k->new HashMap<>()).getOrDefault(cmd,0L)+cooldownSec*1000L; return now<until; }
    public long cooldownLeft(UUID id, String cmd, int cooldownSec) { long now=System.currentTimeMillis(); long until=lastUse.computeIfAbsent(id,k->new HashMap<>()).getOrDefault(cmd,0L)+cooldownSec*1000L; return Math.max(0,(until-now+999)/1000); }
    public void markUsed(UUID id, String cmd){ lastUse.computeIfAbsent(id,k->new HashMap<>()).put(cmd,System.currentTimeMillis()); }
    public void startWarmup(ServerPlayer p, String cmd, int warmupSec, Runnable action){ Warmup w=new Warmup(); w.player=p.getUUID(); w.cmd=cmd; w.endAt=System.currentTimeMillis()+warmupSec*1000L; w.action=action; w.startX=p.getX(); w.startY=p.getY(); w.startZ=p.getZ(); warmups.put(p.getUUID(), w); }
    public void cancelWarmup(UUID id){ warmups.remove(id); }
    public void tick(MinecraftServer server){
        long now=System.currentTimeMillis(); var it=warmups.entrySet().iterator();
        while(it.hasNext()){
            var e=it.next(); var w=e.getValue(); ServerPlayer p=server.getPlayerList().getPlayer(w.player);
            if (p==null){ it.remove(); continue; }
            if (p.getX()!=w.startX || p.getY()!=w.startY || p.getZ()!=w.startZ){ it.remove(); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"warmup_cancelled")), false); continue; }
            if (now>=w.endAt){ it.remove(); server.execute(w.action); }
        }
    }
}
