package com.ffsupver.createheat.item.thermalTool;

import com.ffsupver.createheat.util.NbtUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

public class ThermalToolPointServer {
    private static String ID = "thermal_tool_point_server";
    private static PointMapData pointMapData;
    private static boolean needToUpdate = false;

    public static void registerEvent(){
        NeoForge.EVENT_BUS.addListener(ThermalToolPointServer::onPlayerChangeDimension);
        NeoForge.EVENT_BUS.addListener(ThermalToolPointServer::onPlayerLogin);
    }


    private static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event){
            upDateToPlayer(event,event.getTo());
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        upDateToPlayer(event,event.getEntity().level().dimension());
    }

    private static void upDateToPlayer(PlayerEvent event,ResourceKey<Level> levelKey){
        if (event.getEntity() instanceof ServerPlayer serverPlayer){
            sendToPlayer(serverPlayer,getRenderMap(levelKey));
        }
    }

    public static void tick(ServerTickEvent event){
        MinecraftServer server = event.getServer();

        if (pointMapData == null){
            pointMapData = server.overworld().getDataStorage().get(PointMapData.factory(),ID);
            if (pointMapData == null) {
                pointMapData = new PointMapData();
                needToUpdate = true;
            }
        }

        Map<ResourceKey<Level>,Map<BlockPos,ThermalToolPointLogic>> points = pointMapData.pointMaps;

        points.forEach((levelKey,pointMap)->{
            ServerLevel level = server.getLevel(levelKey);
            if (level == null){
                return;
            }
            pointMap.forEach((pos,logic)->{
                if (level.isLoaded(pos)){
                    logic.tick(level,pos);
                }
            });
        });

        if (needToUpdate){
            //向玩家发送变化
            server.getAllLevels().forEach(
                    serverLevel -> {
                        if (points.containsKey(serverLevel.dimension())) {
                            Map<BlockPos,ThermalToolPointType> renderMap = getRenderMap(serverLevel.dimension());
                            serverLevel.players().forEach(
                                    player -> sendToPlayer(player,renderMap)
                            );
                        }
                    }
            );
            //储存到世界
            PointMapData pointMapData = new PointMapData();
            pointMapData.pointMaps.putAll(points);
            server.overworld().getDataStorage().set(ID,pointMapData);
            pointMapData.setDirty(true); //标记需要保存
            needToUpdate = false;
        }
    }

    private static Map<BlockPos,ThermalToolPointType> getRenderMap(ResourceKey<Level> levelKey){
        Map<BlockPos,ThermalToolPointType> renderMap = new HashMap<>();
        if (points().containsKey(levelKey)){
            points().get(levelKey).forEach((pos, logic) -> {
                renderMap.put(pos, logic.getType());
            });
        }
        return renderMap;
    }
    private static Map<ResourceKey<Level>,Map<BlockPos,ThermalToolPointLogic>> points(){
        return pointMapData.pointMaps;
    }

    private static void sendToPlayer(ServerPlayer player,Map<BlockPos,ThermalToolPointType> renderMap){
        ThermalToolPointRenderPack pointRenderPack = new ThermalToolPointRenderPack(renderMap);
        player.connection.send(pointRenderPack);
    }

    public static void tiggerPoint(ServerLevel serverLevel,BlockPos pos,ThermalToolPointLogic logic){
        ResourceKey<Level> key = serverLevel.dimension();
        points().putIfAbsent(key,new HashMap<>());
        Map<BlockPos,ThermalToolPointLogic> levelPointMap = points().get(key);
        if (levelPointMap.containsKey(pos)){
            ThermalToolPointLogic newLogic = levelPointMap.get(pos).nextLogic(serverLevel,pos);
            if (newLogic == null){
                levelPointMap.remove(pos);
            }else {
                levelPointMap.replace(pos,newLogic);
            }
        }else {
            levelPointMap.putIfAbsent(pos,logic);
        }
        onPointsChange();
    }

    private static void onPointsChange(){
        needToUpdate = true;
    }

    public static ThermalToolPointLogic getPoint(ResourceKey<Level> levelKey,BlockPos pos) {
        Map<ResourceKey<Level>,Map<BlockPos,ThermalToolPointLogic>> pM = pointMapData.pointMaps;
        if (pM.containsKey(levelKey)&& pM.get(levelKey).containsKey(pos)){
            return pM.get(levelKey).get(pos);
        }
        return null;
    }

    public static class PointMapData extends SavedData{
        private final Map<ResourceKey<Level>,Map<BlockPos,ThermalToolPointLogic>> pointMaps = new HashMap<>();
        public static PointMapData load(CompoundTag tag, HolderLookup.Provider levelRegistry){
            PointMapData pointMapData = new PointMapData();
            if (tag.contains("thermal_tool_points",Tag.TAG_LIST)){
                pointMapData.pointMaps.clear();
                pointMapData.pointMaps.putAll(NbtUtil.readMapFromNbtList(
                                tag.getList("thermal_tool_points", Tag.TAG_COMPOUND),
                                keyTag -> ResourceKey.create(Registries.DIMENSION, NbtUtil.resourceLocationFromNbt(keyTag)),
                                mapTag -> NbtUtil.readMapFromNbtList((ListTag) mapTag, NbtUtil::blockPosFromNbt, ThermalToolPointLogic::fromNbt)
                        )
                );
            }
            return pointMapData;
        }

        public static Factory<PointMapData> factory(){
            return new SavedData.Factory<>(() -> {
                throw new RuntimeException("No Point Data");
            }, PointMapData::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
        }

        @Override
        public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
            ListTag pointTag = NbtUtil.writeMapToNbtList(
                    pointMaps,
                    key -> NbtUtil.resourceLocationToNbt(key.location()),
                    map->NbtUtil.writeMapToNbtList(map, NbtUtil::blockPosToNbt, ThermalToolPointLogic::toNbt)
            );
            compoundTag.put("thermal_tool_points",pointTag);
            return compoundTag;
        }

        @Override
        public String toString() {
            return "PointMapData{" +
                    "pointMaps=" + pointMaps +
                    '}';
        }
    }
}
