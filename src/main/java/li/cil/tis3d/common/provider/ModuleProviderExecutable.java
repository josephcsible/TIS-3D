package li.cil.tis3d.common.provider;

import li.cil.tis3d.Constants;
import li.cil.tis3d.api.Casing;
import li.cil.tis3d.api.Face;
import li.cil.tis3d.api.module.Module;
import li.cil.tis3d.api.module.ModuleProvider;
import li.cil.tis3d.system.module.ModuleExecution;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * The provider for the executable module.
 */
public final class ModuleProviderExecutable implements ModuleProvider {
    private final Item ItemModuleExecutable = GameRegistry.findItem(Constants.MOD_ID, Constants.ItemModuleExecutableName);

    @Override
    public boolean worksWith(final ItemStack stack, final Casing casing, final Face face) {
        return stack.getItem() == ItemModuleExecutable;
    }

    @Override
    public Module createModule(final ItemStack stack, final Casing casing, final Face face) {
        return new ModuleExecution(casing, face);
    }
}
