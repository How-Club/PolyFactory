package eu.pb4.polyfactory.data;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record DataType<T extends DataContainer>(String id, MapCodec<T> codec) {
    public static final Map<String, DataType<DataContainer>> TYPES = new HashMap<>();

    @SuppressWarnings("unchecked")
    public DataType {
        TYPES.put(id, (DataType<DataContainer>) this);
    }

    public static final DataType<BoolData> BOOL = new DataType<>("bool", BoolData.TYPE_CODEC);
    public static final DataType<LongData> LONG = new DataType<>("long", LongData.TYPE_CODEC);
    public static final DataType<DoubleData> DOUBLE = new DataType<>("double", DoubleData.TYPE_CODEC);
    public static final DataType<StringData> STRING = new DataType<>("string", StringData.TYPE_CODEC);
    public static final DataType<GameEventData> GAME_EVENT = new DataType<>("game_event", GameEventData.TYPE_CODEC);
    public static final DataType<BlockStateData> BLOCK_STATE = new DataType<>("block_state", BlockStateData.TYPE_CODEC);
    public static final DataType<ItemStackData> ITEM_STACK = new DataType<>("item_stack", ItemStackData.TYPE_CODEC);


}
