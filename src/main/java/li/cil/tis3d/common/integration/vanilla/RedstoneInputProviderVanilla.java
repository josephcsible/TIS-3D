package li.cil.tis3d.common.integration.vanilla;

import li.cil.tis3d.api.machine.Face;
import li.cil.tis3d.api.module.Redstone;
import li.cil.tis3d.common.integration.redstone.RedstoneInputProvider;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class RedstoneInputProviderVanilla implements RedstoneInputProvider {
    @Override
    public int getInput(final Redstone module) {
        final Face face = module.getFace();
        final EnumFacing facing = Face.toEnumFacing(face);
        final World world = module.getCasing().getCasingWorld();
        final BlockPos inputPos = module.getCasing().getPosition().offset(facing);
        if (!world.isBlockLoaded(inputPos)) {
            return 0;
        }

        final int input = world.getRedstonePower(inputPos, facing);
        if (input >= 15) {
            return (short) input;
        } else {
            final IBlockState state = world.getBlockState(inputPos);
            return (short) Math.max(input, state.getBlock() == Blocks.redstone_wire ? state.getValue(BlockRedstoneWire.POWER) : 0);
        }
    }
}