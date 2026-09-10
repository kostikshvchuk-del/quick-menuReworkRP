package xyz.imcodist.data.command_actions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class CommandActionData extends BaseActionData {
    public String command = "";
    public static final String PLAYER_PLACEHOLDER = "{player}";

    @Override
    public String getJsonType() {
        return "cmd";
    }
    @Override
    public String getJsonValue() {
        return command;
    }

    @Override
    public String getTypeString() { return "CMD"; }
    @Override
    public String getString() {
        return command;
    }

    @Override
    public void run() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Run the command.
        String commandToRun = command;

        if (commandToRun != null) {
            // Replace {player} with the name of the player you're looking at.
            // Example: "/pay {player} 450" -> "/pay Steve 450"
            if (commandToRun.contains(PLAYER_PLACEHOLDER)) {
                String targetName = getTargetedPlayerName(client);
                if (targetName == null || targetName.isEmpty()) {
                    player.sendMessage(Text.of("No player targeted!"), true);
                    return;
                }
                commandToRun = commandToRun.replace(PLAYER_PLACEHOLDER, targetName);
            }

            if (commandToRun.startsWith("/")) {
                commandToRun = commandToRun.substring(1);
                player.networkHandler.sendChatCommand(commandToRun);
            } else {
                if (commandToRun.length() >= 256) {
                    commandToRun = commandToRun.substring(0, 256);
                }
                player.networkHandler.sendChatMessage(commandToRun);
            }
        }
    }

    public static String getTargetedPlayerName(MinecraftClient client) {
        if (client == null) return null;

        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof PlayerEntity playerEntity) {
                try {
                    return playerEntity.getGameProfile().getName();
                } catch (Exception ignored) {
                    // Fallback to entity name if profile lookup fails.
                    return entity.getName().getString();
                }
            }
        }

        return null;
    }
}
