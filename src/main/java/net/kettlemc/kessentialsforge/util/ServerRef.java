
package net.kettlemc.kessentialsforge.util;

import net.minecraft.server.MinecraftServer;
public class ServerRef {
    private static volatile MinecraftServer server;
    public static void set(MinecraftServer s){ server = s; }
    public static MinecraftServer get(){ return server; }
}
