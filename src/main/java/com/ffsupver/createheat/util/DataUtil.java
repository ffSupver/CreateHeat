package com.ffsupver.createheat.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DataUtil {
    public static<T> Optional<Holder.Reference<T>> getLastMatchData(ResourceKey<Registry<T>> key, RegistryAccess registryAccess, Predicate<Holder.Reference<T>> filter){
        Stream<Holder.Reference<T>> all = registryAccess.lookupOrThrow(key).listElements();
        ArrayDeque<Holder.Reference<T>> deque = all.collect(Collectors.toCollection(ArrayDeque::new));
        return Stream.generate(deque::pollLast) // 从尾部开始取
                .limit(deque.size())
                .filter(filter)
                .findFirst();
    }
}
