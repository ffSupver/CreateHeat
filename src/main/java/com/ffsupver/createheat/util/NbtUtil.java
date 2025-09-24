package com.ffsupver.createheat.util;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public final class NbtUtil {
    public static ListTag writeBlockPosToNbtList(Collection<BlockPos> blockPosCollection){
        return writeToNbtList(blockPosCollection, pos -> {
            CompoundTag posTag = new CompoundTag();
            posTag.put("p", NbtUtils.writeBlockPos(pos));
            return posTag;
        });
//        ListTag listTag = new ListTag();
//        for (BlockPos connectedPos : blockPosCollection){
//            CompoundTag connectedPosTag = new CompoundTag();
//            connectedPosTag.put("p", NbtUtils.writeBlockPos(connectedPos));
//            listTag.add(connectedPosTag);
//        }
//        return listTag;
    }

    public static Collection<BlockPos> readBlockPosFromNbtList(ListTag listTag){
        return readFromNbt(listTag,tag -> NBTHelper.readBlockPos((CompoundTag) tag,"p"));
//        Collection<BlockPos> collection = new ArrayList<>();
//        for (Tag compoundTag : listTag){
//            collection.add(NBTHelper.readBlockPos((CompoundTag) compoundTag,"p"));
//        }
//        return collection;
    }

    public static <T> ListTag writeToNbtList(Collection<T> collection, Function<T,Tag> write){
        ListTag listTag = new ListTag();
        for (T item : collection){
            listTag.add(write.apply(item));
        }
        return listTag;
    }

    public static <T> Collection<T> readFromNbt(ListTag listTag,Function<Tag,T> read){
        Collection<T> collection = new ArrayList<>();
        for (Tag tag : listTag){
            collection.add(read.apply(tag));
        }
        return collection;
    }
}
