
package net.kettlemc.kessentialsforge.service;

import java.util.*; import net.minecraft.server.level.ServerPlayer;

public class BackService {
    public static class Loc { public String dim; public double x,y,z; public float yaw,pitch; }
    private final Map<UUID, Loc> last = new HashMap<>();
    public void store(ServerPlayer p){ Loc l=new Loc(); l.dim=p.getLevel().dimension().location().toString(); l.x=p.getX(); l.y=p.getY(); l.z=p.getZ(); l.yaw=p.getYRot(); l.pitch=p.getXRot(); last.put(p.getUUID(), l); }
    public Loc pop(UUID id){ return last.remove(id); }
}
