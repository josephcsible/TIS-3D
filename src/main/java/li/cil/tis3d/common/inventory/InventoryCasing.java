package li.cil.tis3d.common.inventory;

import li.cil.tis3d.Constants;
import li.cil.tis3d.api.API;
import li.cil.tis3d.api.Face;
import li.cil.tis3d.api.module.ModuleProvider;
import li.cil.tis3d.common.tile.TileEntityCasing;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * Inventory implementation for casings, having six slots for modules, one per face.
 */
public final class InventoryCasing extends Inventory implements ISidedInventory {
    private final TileEntityCasing tileEntity;

    public InventoryCasing(final TileEntityCasing tileEntity) {
        super(Constants.InventoryCasingName, Face.VALUES.length);
        this.tileEntity = tileEntity;
    }

    // --------------------------------------------------------------------- //
    // IInventory

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        tileEntity.markDirty();
        if (tileEntity.getWorld() != null) {
            tileEntity.getWorld().markBlockForUpdate(tileEntity.getPos());
        }
    }

    // --------------------------------------------------------------------- //
    // ISidedInventory

    @Override
    public int[] getSlotsForFace(final EnumFacing side) {
        return new int[side.ordinal()];
    }

    @Override
    public boolean canInsertItem(final int index, final ItemStack stack, final EnumFacing side) {
        return side.ordinal() == index &&
                getStackInSlot(index) == null &&
                tileEntity.getModule(Face.fromEnumFacing(side)) == null && // Handles virtual modules.
                canInstall(stack, Face.fromEnumFacing(side));
    }

    @Override
    public boolean canExtractItem(final int index, final ItemStack stack, final EnumFacing side) {
        return side.ordinal() == index && stack == getStackInSlot(index);
    }

    private boolean canInstall(final ItemStack stack, final Face face) {
        return API.providerFor(stack, tileEntity, face) != null;
    }

    // --------------------------------------------------------------------- //
    // Inventory

    @Override
    protected void onItemAdded(final int index) {
        final ItemStack stack = getStackInSlot(index);
        if (stack == null) {
            return;
        }

        final Face face = Face.VALUES[index];
        final ModuleProvider provider = API.providerFor(stack, tileEntity, face);
        if (provider == null) {
            return;
        }

        tileEntity.setModule(Face.VALUES[index], provider.createModule(stack, tileEntity, face));
    }

    @Override
    protected void onItemRemoved(final int index) {
        tileEntity.setModule(Face.VALUES[index], null);
    }

    @Override
    public void writeToNBT(final NBTTagCompound nbt) {
        // TODO Tell modules to save data.
        super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        // TODO Tell modules to load data.
    }
}
