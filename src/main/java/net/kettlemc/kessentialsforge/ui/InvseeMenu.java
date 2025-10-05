
package net.kettlemc.kessentialsforge.ui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class InvseeMenu extends AbstractContainerMenu {
    private final Inventory target; private final boolean writable;
    public InvseeMenu(int id, Inventory viewerInv, Inventory target, boolean writable) {
        super(MenuType.GENERIC_9x4, id); this.target=target; this.writable=writable;
        int targetRows = 4; int slotXOffset = 8; int slotSpacing = 18; int targetYBase = 18;
        int mainInventoryStart = 9;
        for (int row=0; row<3; row++) for (int col=0; col<9; col++) {
            int index = mainInventoryStart + row*9 + col; addSlot(new ReadSlot(target, index, slotXOffset + col*slotSpacing, targetYBase + row*slotSpacing));
        }
        for (int col=0; col<9; col++) addSlot(new ReadSlot(target, col, slotXOffset + col*slotSpacing, targetYBase + 3*slotSpacing));
        int armorYOffset = targetYBase + targetRows*slotSpacing + 10;
        for (int a=0; a<4; a++) { int src = 39 - a; addSlot(new ReadSlot(target, src, slotXOffset + a*slotSpacing, armorYOffset)); }
        addSlot(new ReadSlot(target, 40, slotXOffset + 9*slotSpacing, armorYOffset));
        int yBase = targetYBase + (targetRows + 1)*slotSpacing + 28;
        for (int row=0; row<3; ++row) for (int col=0; col<9; ++col) this.addSlot(new Slot(viewerInv, col + row * 9 + 9, slotXOffset + col * slotSpacing, yBase + row * slotSpacing));
        for (int col=0; col<9; ++col) this.addSlot(new Slot(viewerInv, col, slotXOffset + col * slotSpacing, yBase + 58));
    }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (writable && inventory == target && target.player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.slotsChanged(inventory);
            serverPlayer.containerMenu.broadcastChanges();
        }
    }
    private class ReadSlot extends Slot { public ReadSlot(Inventory inv, int index, int x, int y){ super(inv, index, x, y); } @Override public boolean mayPlace(ItemStack stack){ return writable; } @Override public boolean mayPickup(Player player){ return writable; } }
}
