package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.block.NetworkBehaviour;
import com.ffsupver.createheat.network.NetworkService;
import com.ffsupver.createheat.network.TickingBlockNetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;

/**
 * CreateHeat 模组命令注册 / CreateHeat mod command registration
 * <p>
 * 命令结构 / Command structure:
 * // 不带 services 参数（默认 HEAT）/ Without services parameter (default HEAT)
 * /ch network info <blockPos>
 * /ch network info <uuid>
 * /ch network list
 * /ch network remove <blockPos>
 * /ch network remove <uuid>
 * <p>
 * // 带 services 参数（可选）/ With services parameter (optional)
 * /ch network heat info <blockPos>
 * /ch network heat info <uuid>
 * /ch network heat list
 * /ch network heat remove <blockPos>
 * /ch network heat remove <uuid>
 * /ch network heat_storage info <blockPos>
 * ...
 */
public class CHCommands {

    private static final NetworkService.Services DEFAULT_SERVICES = NetworkService.Services.HEAT;

    /**
     * 命令注册入口 / Command registration entry point
     * 在游戏加载时由 NeoForge 事件总线调用 / Called by NeoForge event bus during game load
     */
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 注册主命令: /ch / Register main command: /ch
        dispatcher.register(
            literal("ch")
                .requires(source -> source.hasPermission(2))  // 权限等级 2 = OP
                // 不带 services 参数的分支（默认 HEAT）
                .then(createNetworkCommand(DEFAULT_SERVICES))
                // 带 services 参数的分支
                .then(createNetworkCommandWithServices())
        );
    }

    // ==================== 子命令创建 / Subcommand Creation ====================

    /**
     * 创建不带 services 参数的 network 子命令（默认 HEAT）
     * Create network command without services parameter (default HEAT)
     */
    private static LiteralArgumentBuilder<CommandSourceStack> createNetworkCommand(NetworkService.Services services) {
        return literal("network")
            // /ch network info
            .then(literal("info")
                .then(argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> executeNetworkInfoAtBlock(ctx, services))
                )
                .then(argument("network", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> suggestNetworkId(ctx, builder, services))
                    .executes(ctx -> executeNetworkInfoByUuid(ctx, services))
                )
            )
            // /ch network list
            .then(literal("list")
                .executes(ctx -> executeListNetworks(ctx, services))
            )
            // /ch network remove
            .then(literal("remove")
                .then(argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> executeRemoveNetworkAtBlock(ctx, services))
                )
                .then(argument("network", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> suggestNetworkId(ctx, builder, services))
                    .executes(ctx -> executeRemoveNetwork(ctx, services))
                )
            );
    }

    /**
     * 创建带 services 参数的 network 子命令
     * Create network command with services parameter
     */
    private static LiteralArgumentBuilder<CommandSourceStack> createNetworkCommandWithServices() {
        LiteralArgumentBuilder<CommandSourceStack> networkBuilder = literal("network");
        
        // 为每个服务类型创建子命令
        for (NetworkService.Services services : NetworkService.Services.values()) {
            final NetworkService.Services svc = services;  // 需要是 effectively final
            LiteralArgumentBuilder<CommandSourceStack> serviceBuilder = literal(svc.name().toLowerCase());
            
            // info 子命令
            serviceBuilder
                .then(literal("info")
                    .then(argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> executeNetworkInfoAtBlock(ctx, svc))
                    )
                    .then(argument("network", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestNetworkId(ctx, builder, svc))
                        .executes(ctx -> executeNetworkInfoByUuid(ctx, svc))
                    )
                )
                // list 子命令
                .then(literal("list")
                    .executes(ctx -> executeListNetworks(ctx, svc))
                )
                // remove 子命令
                .then(literal("remove")
                    .then(argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> executeRemoveNetworkAtBlock(ctx, svc))
                    )
                    .then(argument("network", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestNetworkId(ctx, builder, svc))
                        .executes(ctx -> executeRemoveNetwork(ctx, svc))
                    )
                );
            
            networkBuilder = networkBuilder.then(serviceBuilder);
        }
        
        return networkBuilder;
    }

    /**
     * 建议网络 ID
     * Suggest network IDs
     */
    private static CompletableFuture<Suggestions> suggestNetworkId(
            CommandContext<CommandSourceStack> context, 
            SuggestionsBuilder builder,
            NetworkService.Services services) {
        
        String remaining = builder.getRemaining().toLowerCase();
        ServerLevel level = null;
        try {
            level = ((CommandSourceStack)context.getSource()).getLevel();
        } catch (Exception e) {
            // ignore
        }
        if (level != null) {
            Map<ResourceKey<Level>, Set<UUID>> allNetworks = NetworkService.getAllNetworkId(services);
            Set<UUID> networks = allNetworks.getOrDefault(level.dimension(), Set.of());
            for (UUID uuid : networks) {
                if (uuid.toString().toLowerCase().startsWith(remaining)) {
                    builder.suggest(uuid.toString());
                }
            }
        }
        return builder.buildFuture();
    }

    // ==================== 命令执行方法 / Command Execution Methods ====================

    /**
     * 从方块位置获取网络信息的共用方法 / Common method to get network info from block position
     */
    private static NetworkInfo getNetworkInfoFromBlock(
            CommandContext<CommandSourceStack> context,
            NetworkService.Services services) throws CommandSyntaxException {
        
        BlockPos targetPos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ServerLevel level = context.getSource().getLevel();

        NetworkBehaviour behaviour = NetworkBehaviour.get(level, targetPos, NetworkBehaviour.TYPE);

        if (behaviour == null) {
            context.getSource().sendFailure(Component.translatable("commands.createheat.network.info.not_network_block", targetPos.toString()));
            return null;
        }

        UUID networkId = behaviour.getNetworkId();
        // 检查方块的服务类型是否与传入的一致
        if (!behaviour.checkNetworkType(services)) {
            return null;
        }

        return new NetworkInfo(behaviour, networkId, services, level, targetPos);
    }

    /**
     * /ch network info <blockPos>
     * 显示指定位置方块的网络信息 / Display network info at specified block position
     */
    private static int executeNetworkInfoAtBlock(
            CommandContext<CommandSourceStack> context,
            NetworkService.Services services) throws CommandSyntaxException {
        
        NetworkInfo info = getNetworkInfoFromBlock(context, services);
        if (info == null) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.createheat.network.info.not_found"),
                    true
            );
            return 0;
        }

        if (info.networkId == null) {
            context.getSource().sendSuccess(
                () -> Component.translatable("commands.createheat.network.info.network_id_not_found", info.targetPos.toString()),
                true
            );
            return 0;
        }

        TickingBlockNetwork network = NetworkService.getNetwork(info.level, info.networkId, info.services);

        if (network == null) {
            context.getSource().sendFailure(Component.translatable("commands.createheat.network.info.network_not_found", info.networkId.toString()));
            return 0;
        }

        int blockCount = network.getConnectedBlocks().size();

        context.getSource().sendSuccess(
            () -> Component.translatable(
                    "commands.createheat.network.info.success" ,
                    network.getNetworkID().toString(),
                    blockCount,
                    info.level.dimension().location().toString(),
                    network.toString()
            ),
            true
        );
        return 1;
    }

    /**
     * /ch network info <uuid>
     * 显示指定网络的信息 / Display information about specified network
     */
    private static int executeNetworkInfoByUuid(
            CommandContext<CommandSourceStack> context,
            NetworkService.Services services) throws CommandSyntaxException {
        
        String uuidStr = StringArgumentType.getString(context, "network");
        Optional<UUID> uuidOp = validateUUID(uuidStr, context);
        if (uuidOp.isEmpty()) {
            return 0;
        }
        UUID uuid = uuidOp.get();
        ServerLevel level = context.getSource().getLevel();
        TickingBlockNetwork network = NetworkService.getNetwork(level, uuid, services);

        if (network == null) {
            context.getSource().sendFailure(Component.translatable("commands.createheat.network.info.network_not_found", uuid.toString()));
            return 0;
        }

        // 获取方块数量 / Get block count
        int blockCount = network.getConnectedBlocks().size();
        context.getSource().sendSuccess(
            () -> Component.translatable(
                    "commands.createheat.network.info.success" ,
                    network.getNetworkID().toString(),
                    blockCount,
                    level.dimension().location().toString(),
                    network.toString()
            ),
            true
        );
        return 1;
    }

    /**
     * /ch network list
     * 列出当前维度的所有网络 / List all networks in current dimension
     */
    private static int executeListNetworks(
            CommandContext<CommandSourceStack> context,
            NetworkService.Services services) {
        
        ServerLevel level = context.getSource().getLevel();
        ResourceKey<Level> dimKey = level.dimension();

        Map<ResourceKey<Level>, Set<UUID>> allNetworks = NetworkService.getAllNetworkId(services);
        Set<UUID> networks = allNetworks.getOrDefault(dimKey, Set.of());

        if (networks.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.createheat.network.list.network_no_found", dimKey.location().toString()),
                false
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.createheat.network.list.total", networks.size(), dimKey.location().toString()),
                false
            );
            for (UUID uuid : networks) {
                TickingBlockNetwork network = NetworkService.getNetwork(level, uuid, services);
                if (network != null) {
                    context.getSource().sendSuccess(
                            () -> Component.translatable(
                                    "commands.createheat.network.list.info",
                                    uuid.toString(),
                                    network.getConnectedBlocks().size(),
                                    network.getConnectedBlocks().iterator().hasNext()? network.getConnectedBlocks().iterator().next().toString(): "Not Found"
                            ),
                        true
                    );
                }
            }
        }
        return networks.size();
    }

    /**
     * /ch network remove <blockPos>
     * 移除方块所在网络（通过移除所有方块）/ Remove network at block position (by removing all blocks)
     */
    private static int executeRemoveNetworkAtBlock(
            CommandContext<CommandSourceStack> context,
            NetworkService.Services services) throws CommandSyntaxException {
        
        NetworkInfo info = getNetworkInfoFromBlock(context, services);
        if (info == null) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.createheat.network.info.not_found"),
                    true
            );
            return 0;
        }

        if (info.networkId == null) {
            context.getSource().sendFailure(Component.translatable("commands.createheat.network.info.network_id_not_found", info.targetPos.toString()));
            return 0;
        }

        TickingBlockNetwork network = NetworkService.getNetwork(info.level, info.networkId, info.services);

        if (network == null) {
            context.getSource().sendFailure(Component.translatable("commands.createheat.network.info.network_not_found", info.networkId.toString()));
            return 0;
        }

        Set<BlockPos> blocks = network.getConnectedBlocks();
        int blockCount = blocks.size();

        // 清除网络中的所有方块 / Remove all block data in the network
        for (BlockPos pos : new HashSet<>(blocks)) {
            NetworkService.removeBlockFromNetwork(info.level, pos, info.networkId, info.services);
        }

        context.getSource().sendSuccess(
            () -> Component.translatable("commands.createheat.network.remove.success", blockCount, info.networkId.toString()),
            true
        );
        return blockCount;
    }

    /**
     * /ch network remove <uuid>
     * 移除指定网络（通过移除所有方块）/ Remove specified network (by removing all blocks)
     */
    private static int executeRemoveNetwork(
            CommandContext<CommandSourceStack> context,
            NetworkService.Services services) throws CommandSyntaxException {
        
        String uuidStr = StringArgumentType.getString(context, "network");
        Optional<UUID> uuidOp = validateUUID(uuidStr, context);
        if (uuidOp.isEmpty()) {
            return 0;
        }
        UUID uuid = uuidOp.get();
        ServerLevel level = context.getSource().getLevel();
        TickingBlockNetwork network = NetworkService.getNetwork(level, uuid, services);

        if (network == null) {
            context.getSource().sendFailure(Component.translatable("commands.createheat.network.info.network_not_found", uuid.toString()));
            return 0;
        }

        Set<BlockPos> blocks = network.getConnectedBlocks();
        int blockCount = blocks.size();

        // 清除网络中的所有方块数据 / Clear all block data in the network
        for (BlockPos pos : new HashSet<>(blocks)) {
            NetworkService.removeBlockFromNetwork(level, pos, uuid, services);
        }

        context.getSource().sendSuccess(
            () -> Component.translatable("commands.createheat.network.remove.success", blockCount, uuid.toString()),
            true
        );
        return blockCount;
    }

    private static Optional<UUID> validateUUID(String uuidStr, CommandContext<CommandSourceStack> context) {
        try {
            return Optional.of(UUID.fromString(uuidStr));
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("commands.createheat.network.invalid_uuid", uuidStr));
            return Optional.empty();
        }
    }

    // ==================== 数据类 / Data Classes ====================

    /**
     * 网络信息数据类 / Network info data class
     * 用于在命令方法间传递网络相关信息 / Used to pass network related info between command methods
     */
    private record NetworkInfo(
        NetworkBehaviour behaviour,
        UUID networkId,
        NetworkService.Services services,
        ServerLevel level,
        BlockPos targetPos
    ) {}

    // ==================== 辅助方法 / Helper Methods ====================

    private static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
