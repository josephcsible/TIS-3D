package li.cil.tis3d.common.block;

import com.google.common.collect.ImmutableMap;
import li.cil.tis3d.api.Face;
import li.cil.tis3d.api.module.Module;
import li.cil.tis3d.api.module.Redstone;
import li.cil.tis3d.common.tile.TileEntityCasing;
import li.cil.tis3d.util.InventoryUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Block for the module casings.
 */
public final class BlockCasing extends Block {
    private static final OBJModel.OBJState[] OBJ_STATE_CACHE = new OBJModel.OBJState[1 << Face.VALUES.length];
    private static final List<String> VISIBLE_GROUPS = new ArrayList<>(6);
    private static final Map<Face, String> FACE_TO_GROUP = ImmutableMap.<Face, String>builder().
            put(Face.Y_NEG, "casing:module.Y_NEG").
            put(Face.Y_POS, "casing:module.Y_POS").
            put(Face.Z_NEG, "casing:module.Z_NEG").
            put(Face.Z_POS, "casing:module.Z_POS").
            put(Face.X_NEG, "casing:module.X_NEG").
            put(Face.X_POS, "casing:module.X_POS").
            build();

    public BlockCasing() {
        super(Material.iron);
    }

    // --------------------------------------------------------------------- //
    // State

    @Override
    public BlockState createBlockState() {
        return new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty[]{OBJModel.OBJProperty.instance});
    }

    @Override
    public IBlockState getExtendedState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        final IExtendedBlockState baseState = (IExtendedBlockState) state;
        final TileEntity tileEntity = world.getTileEntity(pos);
        final int mask = packVisibility(tileEntity);
        return baseState.withProperty(OBJModel.OBJProperty.instance, getCachedObjState(mask));
    }

    private OBJModel.OBJState getCachedObjState(final int mask) {
        synchronized (OBJ_STATE_CACHE) {
            if (OBJ_STATE_CACHE[mask] == null) {
                VISIBLE_GROUPS.clear();
                VISIBLE_GROUPS.add("casing:casing");
                for (final Face face : Face.VALUES) {
                    if ((mask & (1 << face.ordinal())) != 0) {
                        VISIBLE_GROUPS.add(FACE_TO_GROUP.get(face));
                    }
                }
                OBJ_STATE_CACHE[mask] = new OBJModel.OBJState(VISIBLE_GROUPS, true);
            }
            return OBJ_STATE_CACHE[mask];
        }
    }

    private int packVisibility(final TileEntity tileEntity) {
        int mask = 0;
        if (tileEntity instanceof TileEntityCasing) {
            final TileEntityCasing casing = (TileEntityCasing) tileEntity;
            for (final Face face : Face.VALUES) {
                if (casing.getModule(face) != null) {
                    mask |= 1 << face.ordinal();
                }
            }
        }
        return mask;
    }

    // --------------------------------------------------------------------- //
    // Client

    @Override
    public ItemStack getPickBlock(final MovingObjectPosition target, final World world, final BlockPos pos, final EntityPlayer player) {
        // Allow picking modules installed in the casing.
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityCasing) {
            final TileEntityCasing casing = (TileEntityCasing) tileEntity;
            final ItemStack stack = casing.getStackInSlot(target.sideHit.ordinal());
            if (stack != null) {
                return stack.copy();
            }
        }
        return super.getPickBlock(target, world, pos, player);
    }

    // --------------------------------------------------------------------- //
    // Common

    @Override
    public boolean isSideSolid(final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        return false;
    }

    @Override
    public boolean hasTileEntity(final IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(final World world, final IBlockState state) {
        return new TileEntityCasing();
    }

    @Override
    public boolean onBlockActivated(final World world, final BlockPos pos, final IBlockState state, final EntityPlayer player, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (world.isBlockLoaded(pos)) {
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TileEntityCasing) {
                final TileEntityCasing casing = (TileEntityCasing) tileEntity;

                final Module module = casing.getModule(Face.fromEnumFacing(side));
                if (module != null && module.onActivate(player, hitX, hitY, hitZ)) {
                    return true;
                }

                final ItemStack oldModule = casing.getStackInSlot(side.ordinal());
                if (oldModule != null) {
                    if (!world.isRemote) {
                        final EntityItem entity = InventoryUtils.drop(world, pos, casing, side.ordinal(), 1, side);
                        if (entity != null) {
                            entity.setNoPickupDelay();
                            entity.onCollideWithPlayer(player);
                            world.playSoundEffect(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, "tile.piston.in", 0.2f, 0.8f + world.rand.nextFloat() * 0.1f);
                        }
                    }
                    return true;
                } else {
                    final ItemStack newModule = player.getHeldItem();
                    if (casing.canInsertItem(side.ordinal(), newModule, side)) {
                        if (!world.isRemote) {
                            casing.setInventorySlotContents(side.ordinal(), newModule.splitStack(1));
                            world.playSoundEffect(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, "tile.piston.out", 0.2f, 0.8f + world.rand.nextFloat() * 0.1f);
                        }
                        return true;
                    }
                }
            }
        }
        return super.onBlockActivated(world, pos, state, player, side, hitX, hitY, hitZ);
    }

    @Override
    public void breakBlock(final World world, final BlockPos pos, final IBlockState state) {
        final TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof TileEntityCasing) {
            InventoryHelper.dropInventoryItems(world, pos, (TileEntityCasing) tileentity);
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    // --------------------------------------------------------------------- //
    // Redstone

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(final World world, final BlockPos pos) {
        return Container.calcRedstone(world.getTileEntity(pos));
    }

    @Override
    public int getWeakPower(final IBlockAccess world, final BlockPos pos, final IBlockState state, final EnumFacing side) {
        final TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof TileEntityCasing) {
            final TileEntityCasing casing = (TileEntityCasing) tileentity;
            final Module module = casing.getModule(Face.fromEnumFacing(side.getOpposite()));
            if (module instanceof Redstone) {
                return ((Redstone) module).getRedstoneOutput();
            }
        }
        return super.getWeakPower(world, pos, state, side);
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    // --------------------------------------------------------------------- //
    // Networking

    @Override
    public void onNeighborBlockChange(final World world, final BlockPos pos, final IBlockState state, final Block neighborBlock) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityCasing) {
            final TileEntityCasing casing = (TileEntityCasing) tileEntity;
            casing.checkNeighbors();
        }
        super.onNeighborBlockChange(world, pos, state, neighborBlock);
    }
}
