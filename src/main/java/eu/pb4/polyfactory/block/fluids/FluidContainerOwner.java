package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface FluidContainerOwner extends FilledStateProvider {
    @Nullable
    FluidContainer getFluidContainer(Direction direction);

    @Nullable
    FluidContainer getMainFluidContainer();


    @Override
    default Text getFilledStateText() {
        var main = getMainFluidContainer();
        return main != null ?
                Text.translatable("text.polyfactory.x_out_of_y", FactoryUtil.fluidText(main.stored()), FactoryUtil.fluidText(main.capacity()))
                : null;
    }
}
