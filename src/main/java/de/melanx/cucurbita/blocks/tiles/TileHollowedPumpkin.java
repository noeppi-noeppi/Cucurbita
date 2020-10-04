package de.melanx.cucurbita.blocks.tiles;

import de.melanx.cucurbita.api.recipe.HeatSourcesRecipe;
import de.melanx.cucurbita.blocks.base.ModTile;
import de.melanx.cucurbita.core.Registration;
import de.melanx.cucurbita.util.inventory.BaseItemStackHandler;
import de.melanx.cucurbita.util.inventory.ItemStackHandlerWrapper;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TileHollowedPumpkin extends ModTile {

    public static final int FLUID_CAPACITY = 2000;

    private final LazyOptional<IItemHandlerModifiable> handler = this.createHandler(this::getInventory);
    private final BaseItemStackHandler inventory = new BaseItemStackHandler(16, null, this::isValidStack);
    private final ModdedFluidTank fluidInventory = new ModdedFluidTank(FLUID_CAPACITY, fluidStack -> fluidStack.getFluid().isEquivalentTo(Fluids.WATER));
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> this.fluidInventory);

    private int heat;

    public TileHollowedPumpkin() {
        super(Registration.TILE_HOLLOWED_PUMPKIN.get());
        this.inventory.setInputSlots(IntStream.range(0, 16).toArray());
    }

    /**
     * This can be used to add canExtract or canInsert to the wrapper used as capability. You may not call the supplier
     * now. Always use IItemHandlerModifiable.createLazy. You may call the supplier inside the canExtract and canInsert
     * lambda.
     */
    protected LazyOptional<IItemHandlerModifiable> createHandler(Supplier<IItemHandlerModifiable> inventory) {
        return ItemStackHandlerWrapper.createLazy(inventory);
    }

    @Nonnull
    public BaseItemStackHandler getInventory() {
        return this.inventory;
    }

    public FluidTank getFluidInventory() {
        return this.fluidInventory;
    }

    public boolean isValidStack(int slot, ItemStack stack) {
        return Arrays.stream(this.inventory.getInputSlots()).noneMatch(x -> x == slot);
    }

    public boolean hasHeat() {
        return this.heat > 0;
    }

    public int getHeat() {
        return this.heat;
    }

    @Override
    public void tick() {
        super.tick();
        if (world != null) {
            BlockState state = this.world.getBlockState(this.pos.down());
            this.heat = HeatSourcesRecipe.getHeatValue(state);
            if (!this.world.isRemote) {
                this.getFluidInventory().setFluid(new FluidStack(Registration.FLUID_PLANT_OIL.get(), 1000));
                this.markDirty();
                this.markDispatchable();
            } else {
                if (this.fluidInventory.getFluidAmount() > 0 && this.hasHeat()) {
                    Random rand = this.world.rand;
                    if (rand.nextDouble() < 0.1D) {
                        double x = this.pos.getX() + 2 / 16D + rand.nextDouble() * 12 / 16D;
                        double y = this.pos.getY() + 1 / 16D + (double) this.fluidInventory.getFluidAmount() / TileHollowedPumpkin.FLUID_CAPACITY + rand.nextDouble() * 0.3D;
                        double z = this.pos.getZ() + 2 / 16D + rand.nextDouble() * 12 / 16D;
                        this.world.addParticle(ParticleTypes.AMBIENT_ENTITY_EFFECT, x, y, z, 0.0F, 0.0F, 0.0F);
                    }
                }
            }
        }
    }

    public void onWanded() {
        System.out.println("It's done!");
    }

    @Override
    public void readPacketNBT(CompoundNBT cmp) {
        this.getInventory().deserializeNBT(cmp.getCompound("inventory"));
        this.getFluidInventory().setFluid(FluidStack.loadFluidStackFromNBT(cmp.getCompound("fluid")));
    }

    @Override
    public void writePacketNBT(CompoundNBT cmp) {
        cmp.put("inventory", this.getInventory().serializeNBT());
        final CompoundNBT tankTag = new CompoundNBT();
        this.getFluidInventory().getFluid().writeToNBT(tankTag);
        cmp.put("fluid", tankTag);
    }

    @Nonnull
    @Override
    public <X> LazyOptional<X> getCapability(@Nonnull Capability<X> cap, @Nullable Direction side) {
        if (!this.removed && (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)) {
            return this.handler.cast();
        } else if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return this.fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    private static class ModdedFluidTank extends FluidTank {
        public ModdedFluidTank(int capacity, Predicate<FluidStack> validator) {
            super(capacity, validator);
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}