package net.kettlemc.kessentialsforge.ui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmorSeeMenu extends AbstractContainerMenu {
    private final Inventory target;
    private final boolean writable;

    public ArmorSeeMenu(int id, Inventory viewerInv, Inventory target, boolean writable) {
        super(MenuType.GENERIC_9x1, id);
        this.target = target;
        this.writable = writable;

        int baseY = 18;
        int startX = 8;

        int[] armorSlots = {39, 38, 37, 36};
        for (int i = 0; i < armorSlots.length; ++i) {
            this.addSlot(new ReadSlot(target, armorSlots[i], startX + i * 18, baseY));
        }

        this.addSlot(new ReadSlot(target, 40, startX + 5 * 18, baseY));

        int offset = (1 - 4) * 18;
        int yBase = 103 + offset;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(viewerInv, col + row * 9 + 9, 8 + col * 18, yBase + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(viewerInv, col, 8 + col * 18, 161 + offset));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (this.writable && inventory == this.target) {
            Player owner = this.target.player;
            if (owner instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.slotsChanged(inventory);
                serverPlayer.containerMenu.broadcastChanges();
            }
        }
    }

    private class ReadSlot extends Slot {
        public ReadSlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return writable;
        }

        @Override
        public boolean mayPickup(Player player) {
            return writable;
        }
    }
}
