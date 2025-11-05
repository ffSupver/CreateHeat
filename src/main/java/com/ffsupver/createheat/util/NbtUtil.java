package com.ffsupver.createheat.util;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class NbtUtil {
    public static ListTag writeBlockPosToNbtList(Collection<BlockPos> blockPosCollection){
        return writeToNbtList(blockPosCollection, NbtUtil::blockPosToNbt);
    }

    public static Collection<BlockPos> readBlockPosFromNbtList(ListTag listTag){
        return readListFromNbt(listTag, NbtUtil::blockPosFromNbt);
    }

    public static <K,V> ListTag writeMapToNbtList(Map<K,V> map,Function<K,Tag> writeKey,Function<V,Tag> writeValue){
        return writeToNbtList(map.entrySet(),kvEntry -> {
           CompoundTag tag = new CompoundTag();
            Tag keyTag = writeKey.apply(kvEntry.getKey());
           Tag valurTag = writeValue.apply(kvEntry.getValue());
            if (keyTag != null && valurTag != null){
                tag.put("k", keyTag);
                tag.put("v", valurTag);
                return tag;
            }
            return null;
        });
    }

    public static <K,V> Map<K,V> readMapFromNbtList(ListTag listTag,Function<Tag,K> readKey,Function<Tag,V> readValue){
        Map<K,V> result = new HashMap<>();
        readListFromNbt(listTag, tag -> {
            result.put(readKey.apply(((CompoundTag)tag).get("k")),readValue.apply(((CompoundTag)tag).get("v")));
            return null;
        });
        return result;
    }

    public static <T> ListTag writeToNbtList(Collection<T> collection, Function<T,Tag> write){
        ListTag listTag = new ListTag();
        for (T item : collection){
            Tag itemTag = write.apply(item);
            if (itemTag != null){
                listTag.add(itemTag);
            }
        }
        return listTag;
    }

    public static <T> Collection<T> readListFromNbt(ListTag listTag, Function<Tag,T> read){
        Collection<T> collection = new ArrayList<>();
        for (Tag tag : listTag){
            T item = read.apply(tag);
            if (item != null){
                collection.add(item);
            }
        }
        return collection;
    }

    public static BlockPos blockPosFromNbt(Tag tag){
       return NBTHelper.readBlockPos((CompoundTag) tag,"p");
    }

    public static CompoundTag blockPosToNbt(BlockPos pos){
        CompoundTag tag = new CompoundTag();
        tag.put("p",NbtUtils.writeBlockPos(pos));
        return tag;
    }

    public static CompoundTag intToNbt(int i){
        CompoundTag tag = new CompoundTag();
        tag.putInt("i",i);
        return tag;
    }

    public static Integer intFromNbt(Tag tag){
        return ((CompoundTag)tag).getInt("i");
    }
    public static CompoundTag resourceLocationToNbt(ResourceLocation id){
        CompoundTag tag = new CompoundTag();
        tag.putString("id",id.toString());
        return tag;
    }
    
    public static ResourceLocation resourceLocationFromNbt(Tag tag){
        return ResourceLocation.parse(((CompoundTag)tag).getString("id"));
    }
}
