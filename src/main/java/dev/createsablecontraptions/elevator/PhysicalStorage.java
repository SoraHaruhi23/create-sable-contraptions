package dev.createsablecontraptions.elevator;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.AllTags.AllMountedItemStorageTypeTags;
import com.simibubi.create.api.contraption.storage.item.*;
import com.simibubi.create.api.contraption.storage.fluid.*;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Views only. Real block entities remain the sole inventory/fluid owners and save authority. */
public final class PhysicalStorage extends MountedStorageManager {
    private final ServerSubLevel sub;
    private final java.util.List<IItemHandlerModifiable> external = new java.util.ArrayList<>();
    public PhysicalStorage(ServerSubLevel sub) { this.sub = sub; initialize(); }

    private BlockPos physical(BlockPos local) {
        return BlockPos.of(ElevatorBridge.data(sub).getLong("PlotAnchor")).offset(local);
    }
    private IItemHandlerModifiable item(BlockPos local) {
        if (sub.isRemoved()) return null;
        var cap = sub.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, physical(local), null);
        return cap instanceof IItemHandlerModifiable modifiable ? modifiable : null;
    }
    private IFluidHandler fluid(BlockPos local) {
        return sub.isRemoved() ? null : sub.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, physical(local), null);
    }
    public MountedItemStorage itemAt(BlockPos local) {
        var cap = item(local);
        if (cap == null) return null;
        return itemView(local);
    }
    private MountedItemStorage itemView(BlockPos local) {
        var type = MountedItemStorageType.REGISTRY.get(sub.getLevel().getBlockState(physical(local)).getBlock());
        return new ItemView(type == null ? AllMountedStorageTypes.FALLBACK.get() : type, local);
    }
    public MountedFluidStorage fluidAt(BlockPos local) {
        if (fluid(local) == null) return null;
        return fluidView(local);
    }
    private MountedFluidStorage fluidView(BlockPos local) {
        var type = MountedFluidStorageType.REGISTRY.get(sub.getLevel().getBlockState(physical(local)).getBlock());
        return new FluidView(type == null ? AllMountedStorageTypes.FLUID_TANK.get() : type, local);
    }
    @Override public ImmutableMap<BlockPos, MountedItemStorage> getAllItemStorages() {
        var map = ImmutableMap.<BlockPos, MountedItemStorage>builder();
        // Multi-block capabilities may be shared: expose a shared handler only once.
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<IItemHandlerModifiable, Boolean>());
        for (long packed : ElevatorBridge.data(sub).getLongArray("Blocks")) {
            var local = BlockPos.of(packed);
            var cap = item(local);
            if (cap != null && seen.add(cap)) map.put(local, itemView(local));
        }
        return map.build();
    }
    @Override public MountedItemStorageWrapper getMountedItems() {
        var map = ImmutableMap.<BlockPos, MountedItemStorage>builder();
        getAllItemStorages().forEach((pos, storage) -> {
            if (!AllMountedItemStorageTypeTags.INTERNAL.matches(storage)) map.put(pos, storage);
        });
        return new MountedItemStorageWrapper(map.build());
    }
    @Override public CombinedInvWrapper getAllItems() {
        var handlers = new java.util.ArrayList<IItemHandlerModifiable>();
        handlers.add(getMountedItems()); handlers.addAll(external);
        return new CombinedInvWrapper(handlers.toArray(IItemHandlerModifiable[]::new));
    }
    @Override public MountedItemStorageWrapper getFuelItems() {
        var map = ImmutableMap.<BlockPos, MountedItemStorage>builder();
        getAllItemStorages().forEach((pos, storage) -> {
            if (!AllMountedItemStorageTypeTags.INTERNAL.matches(storage)
                    && !AllMountedItemStorageTypeTags.FUEL_BLACKLIST.matches(storage)) map.put(pos, storage);
        });
        return new MountedItemStorageWrapper(map.build());
    }
    @Override public MountedFluidStorageWrapper getFluids() {
        var map = ImmutableMap.<BlockPos, MountedFluidStorage>builder();
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<IFluidHandler, Boolean>());
        for (long packed : ElevatorBridge.data(sub).getLongArray("Blocks")) {
            var local = BlockPos.of(packed); var cap = fluid(local);
            if (cap != null && seen.add(cap)) map.put(local, fluidView(local));
        }
        return new MountedFluidStorageWrapper(map.build());
    }
    @Override public void attachExternal(IItemHandlerModifiable storage) { if (!external.contains(storage)) external.add(storage); }
    @Override public void tick(AbstractContraptionEntity entity) { }
    @Override public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        // Do not serialize capability views through Create's snapshot codecs.
        tag.remove("items"); tag.remove("fluids"); tag.remove("interactable_positions");
    }

    private final class ItemView extends MountedItemStorage {
        private final BlockPos local;
        ItemView(MountedItemStorageType<?> type, BlockPos local) { super(type); this.local = local; }
        private IItemHandlerModifiable cap() { return item(local); }
        private boolean valid(IItemHandlerModifiable cap, int slot) { return cap != null && slot >= 0 && slot < cap.getSlots(); }
        @Override public int getSlots() { var c = cap(); return c == null ? 0 : c.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { var c = cap(); return valid(c, slot) ? c.getStackInSlot(slot) : ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { var c = cap(); return valid(c, slot) ? c.insertItem(slot, stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { var c = cap(); return valid(c, slot) ? c.extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { var c = cap(); return valid(c, slot) ? c.getSlotLimit(slot) : 0; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { var c = cap(); return valid(c, slot) && c.isItemValid(slot, stack); }
        @Override public void setStackInSlot(int slot, ItemStack stack) { var c = cap(); if (!valid(c, slot)) throw new IllegalStateException("Physical inventory disappeared"); c.setStackInSlot(slot, stack); }
        @Override public void unmount(Level level, BlockState state, BlockPos pos, BlockEntity be) { }
    }
    private final class FluidView extends MountedFluidStorage {
        private final BlockPos local;
        FluidView(MountedFluidStorageType<?> type, BlockPos local) { super(type); this.local = local; }
        private IFluidHandler cap() { return fluid(local); }
        @Override public int getTanks() { var c = cap(); return c == null ? 0 : c.getTanks(); }
        @Override public FluidStack getFluidInTank(int tank) { var c = cap(); return c == null || tank >= c.getTanks() ? FluidStack.EMPTY : c.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { var c = cap(); return c == null || tank >= c.getTanks() ? 0 : c.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { var c = cap(); return c != null && tank < c.getTanks() && c.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack stack, FluidAction action) { var c = cap(); return c == null ? 0 : c.fill(stack, action); }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { var c = cap(); return c == null ? FluidStack.EMPTY : c.drain(stack, action); }
        @Override public FluidStack drain(int max, FluidAction action) { var c = cap(); return c == null ? FluidStack.EMPTY : c.drain(max, action); }
        @Override public void unmount(Level level, BlockState state, BlockPos pos, BlockEntity be) { }
    }
}
