package eu.pb4.polyfactory.block.other;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("UnstableApiUsage")
public class ContainerBlockEntity extends LockableBlockEntity {
    static  {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.CONTAINER);
    }
    private static final int MAX_SLOT_SIZE = 9 * 4;

    private ItemStack itemStack = ItemStack.EMPTY;
    public ContainerBlockEntity(BlockPos pos, BlockState state) {
        this(FactoryBlockEntities.CONTAINER, pos, state);
    }
    public final SingleItemStorage storage = createStorage();

    protected ContainerBlockEntity(BlockEntityType<? extends ContainerBlockEntity> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    protected SingleItemStorage createStorage() {
        return new SingleItemStorage() {
            @Override
            protected long getCapacity(ItemVariant variant) {
                return ContainerBlockEntity.this.getMaxCount(ContainerBlockEntity.this.itemStack);
            }

            @Override
            public long extract(ItemVariant extractedVariant, long maxAmount, TransactionContext transaction) {
                var v = this.variant;
                var i = super.extract(extractedVariant, maxAmount, transaction);
                this.variant = v;
                return i;
            }

            @Override
            protected void onFinalCommit() {
                super.onFinalCommit();
                ContainerBlockEntity.this.markDirty();
                world.updateComparators(pos, ContainerBlockEntity.this.getCachedState().getBlock());
                updateHologram();
            }
        };
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        this.storage.writeNbt(nbt);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.storage.readNbt(nbt);
        this.itemStack = this.storage.variant.toStack();
        super.readNbt(nbt);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        this.updateHologram();
        return super.toInitialChunkDataNbt();
    }

    private void updateHologram() {
        var type = BlockBoundAttachment.get(this.world, this.pos);
        if (this.itemStack == ItemStack.EMPTY) {
            this.itemStack = this.storage.variant.toStack();
        }

        if (type != null && type.holder() instanceof ContainerBlock.Model model) {
            model.setDisplay(this.itemStack, this.storage.amount);
            model.tick();
        }
    }

    public int getMaxCount(ItemStack stack) {
        return MAX_SLOT_SIZE * stack.getMaxCount();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    public boolean matches(ItemStack stackInHand) {
        return ItemStack.canCombine(this.itemStack, stackInHand);
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setItemStack(ItemStack stack) {
        this.itemStack = stack.copyWithCount(1);
        this.storage.variant = ItemVariant.of(stack);
        this.updateHologram();
    }

    public ItemStack extract(int amount) {
        try (var t = Transaction.openOuter()) {
            var i = this.storage.extract(this.storage.variant, amount, t);
            t.commit();
            this.markDirty();
            return this.itemStack.copyWithCount((int) i);
        }
    }

    public int addItems(int i) {
        try (var t = Transaction.openOuter()) {
            i = (int) this.storage.insert(this.storage.variant, i, t);
            t.commit();
            return i;
        }
    }

    public boolean isEmpty() {
        return this.storage.amount == 0;
    }


}
