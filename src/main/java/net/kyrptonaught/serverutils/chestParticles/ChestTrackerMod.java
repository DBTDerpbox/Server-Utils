package net.kyrptonaught.serverutils.chestParticles;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class ChestTrackerMod {
    public static HashMap<String, HashSet<BlockPos>> playerUsedChests = new HashMap<>();
    public static HashMap<BlockPos, HashSet<String>> usedChest = new HashMap<>();
    public static boolean enabled = true;
    public static String scoreboardObjective;

    public static void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (enabled && world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof ChestBlock) {
                addChestForPlayer(player, hitResult.getBlockPos());
            }
            return ActionResult.PASS;
        });

        CommandRegistrationCallback.EVENT.register(ChestTrackerMod::registerCommand);
    }

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, boolean b) {
        dispatcher.register(CommandManager.literal("chesttracker")
                .requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("enabled").then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            enabled = BoolArgumentType.getBool(context, "enabled");
                            reset(context.getSource().getServer());
                            return 1;
                        })))
                .then(CommandManager.literal("reset")
                        .executes(context -> {
                            reset(context.getSource().getServer());
                            return 1;
                        }))
                .then(CommandManager.literal("fillChests")
                        .executes(context -> {
                            usedChest.clear();
                            return 1;
                        }))
                .then(CommandManager.literal("scoreboardObjective").then(CommandManager.argument("scoreboardObjective", StringArgumentType.word())
                        .executes(context -> {
                            scoreboardObjective = StringArgumentType.getString(context, "scoreboardObjective");
                            return 1;
                        }))));
    }

    public static void reset(MinecraftServer server) {
        playerUsedChests.clear();
        usedChest.clear();
        ServerScoreboard scoreboard = server.getScoreboard();
        server.getPlayerManager().getPlayerList().forEach(player -> {
            scoreboard.getPlayerScore(player.getEntityName(), scoreboard.getObjective(scoreboardObjective)).setScore(0);
        });
    }

    public static void addChestForPlayer(PlayerEntity player, BlockPos pos) {
        BlockState state = player.world.getBlockState(pos);
        if (state.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT) pos = pos.offset(ChestBlock.getFacing(state));

        String uuid = player.getUuidAsString();

        usedChest.computeIfAbsent(pos, k -> new HashSet<>()).add(uuid);
        playerUsedChests.computeIfAbsent(uuid, k -> new HashSet<>()).add(pos);

        ServerScoreboard scoreboard = (ServerScoreboard) player.getScoreboard();
        scoreboard.getPlayerScore(player.getEntityName(), scoreboard.getObjective(scoreboardObjective)).setScore(playerUsedChests.get(uuid).size());
    }

    public static void spawnParticleTick(World world, BlockPos pos, BlockState state, ChestBlockEntity blockEntity) {
        if (state.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT) pos = pos.offset(ChestBlock.getFacing(state));
        if (enabled && !ChestTrackerMod.usedChest.containsKey(pos)) {
            Random random = world.getRandom();

            //((ServerWorld) world).spawnParticles(ParticleTypes.COMPOSTER, (double) pos.getX() + random.nextDouble(), pos.getY() + 1 + (random.nextDouble() / 2), (double) pos.getZ() + random.nextDouble(), 0, 0.0, 1, 0.0, 1);
            ((ServerWorld) world).spawnParticles(ParticleTypes.GLOW, (double) pos.getX() + random.nextDouble(), pos.getY() + 1 + (random.nextDouble() / 2), (double) pos.getZ() + random.nextDouble(), 0, 0, .2, 0.0, 1);
            //((ServerWorld) world).spawnParticles(ParticleTypes.LANDING_OBSIDIAN_TEAR, (double) pos.getX() + random.nextDouble(), pos.getY() + 1 + (random.nextDouble() / 2), (double) pos.getZ() + random.nextDouble(), 0, 0, .2, 0.0, 1);
        }
    }
}