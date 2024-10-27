package eu.pb4.polyfactory.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.*;

import java.util.function.Function;

public record RegistryGetterCodec<T, Y>(RegistryKey<Registry<T>> registryKey, Function<RegistryEntryLookup<T>, Y> function) implements Codec<Y> {
    @Override
    public <T1> DataResult<Pair<Y, T1>> decode(DynamicOps<T1> ops, T1 input) {
        if (ops instanceof RegistryOps<T1> registryOps) {
            try {
                var result = this.function.apply(registryOps.getEntryLookup(registryKey).orElseThrow());
                if (result == null) {
                    throw new NullPointerException("result is null!");
                }
                return DataResult.success(new Pair<>(result, input));
            } catch (Throwable e) {
                return DataResult.error(e::getMessage);
            }

        }


        return DataResult.error(() -> "No registry access!");
    }

    @Override
    public <T1> DataResult<T1> encode(Y input, DynamicOps<T1> ops, T1 prefix) {
        return DataResult.success(prefix);
    }
}
