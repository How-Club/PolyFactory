package eu.pb4.polyfactory.block.creative;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.DrainBlockEntity;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CreativeDrainBlockEntity extends DrainBlockEntity {
    public CreativeDrainBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CREATIVE_DRAIN, pos, state);
    }

    @Override
    public long insertFluid(FluidInstance<?> type, long amount, Direction direction) {
        return 0;
    }

    @Override
    public long extractFluid(FluidInstance<?> type, long amount, Direction direction, boolean change) {
        return Math.min(this.getFluidContainer(direction).get(type), amount);
    }
}
