package eu.pb4.polyfactory.block.data.util;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class ChanneledDataBlockEntity extends LockableBlockEntity implements ChanneledDataCache, BlockEntityExtraListener, ColorProvider {
    private int color = -2;
    @Nullable
    private AbstractCableBlock.BaseCableModel model;
    protected DataContainer lastData = StringData.EMPTY;
    private int channel;
    @Nullable
    public DataContainer getCachedData() {
        return this.lastData;
    }

    public void setCachedData(DataContainer lastData) {
        this.lastData = lastData;
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.contains("data")) {
            this.lastData = DataContainer.fromNbt(nbt.getCompound("data"), lookup);
        }
        setChannel(nbt.getInt("channel"));
        if (nbt.contains("color")) {
            this.color = nbt.getInt("color");
        } else {
            this.color = AbstractCableBlock.DEFAULT_COLOR;
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("channel", this.channel);
        nbt.put("data", this.lastData.createNbt(lookup));
        nbt.putInt("color", this.color);
    }

    public int channel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
        if (this.hasWorld()) {
            NetworkComponent.Data.updateDataAt(this.world, this.pos);
            this.markDirty();
        }
    }

    @Override
    public void setColor(int color) {
        if (this.color != color) {
            this.color = color;
            if (this.model != null) {
                this.model.setColor(color);
            }
            this.markDirty();
        }
    }

    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public void setColorFromPreviousBlockEntity(int c) {
        setColor(c);
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        var x = BlockBoundAttachment.get(chunk, this.getPos());
        if (x != null && x.holder() instanceof AbstractCableBlock.BaseCableModel baseCableModel) {
            this.model = baseCableModel;
            baseCableModel.setColor(this.color);
        }
    }

    @Override
    public boolean isDefaultColor() {
        return this.color == FactoryItems.CABLE.getDefaultColor();
    }


    public ChanneledDataBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PROVIDER_DATA_CACHE, pos, state);
    }

    protected ChanneledDataBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
