package eu.pb4.polyfactory.fluid.shooting;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public record NoOpFluidShootingBehavior<T>() implements FluidShootingBehavior<T> {


    @Override
    public boolean canShoot(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container) {
        return false;
    }

    @Override
    public void startShooting(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container) {

    }

    @Override
    public void continueShooting(ShooterContext context, FluidInstance<T> fluidInstance, int tick, FluidContainer container) {

    }

    @Override
    public void stopShooting(ShooterContext context, FluidInstance<T> fluidInstance) {

    }
}
