package net.kettlemc.kessentialsforge.ui;

import net.kettlemc.kessentialsforge.event.EnchantingTableEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;

public class CommandEnchantmentMenu extends EnchantmentMenu {
    private final ServerPlayer player;

    public CommandEnchantmentMenu(int id, Inventory inventory, ServerPlayer player) {
        super(id, inventory, ContainerLevelAccess.create(player.level, player.blockPosition()));
        this.player = player;
    }

    @Override
    public void slotsChanged(Container container) {
        EnchantingTableEvents.pushPlayer(player);
        try {
            super.slotsChanged(container);
        } finally {
            EnchantingTableEvents.popPlayer();
        }
    }
}
