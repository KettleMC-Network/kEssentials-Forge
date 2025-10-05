package net.kettlemc.kessentialsforge.mixin;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public interface PlayerListBridge {
    void broadcastSystemMessage(Component message, ChatType chatType, UUID sender);
}
