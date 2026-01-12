package org.icetank.module;

import com.github.rfresh2.EventConsumer;
import com.zenith.Globals;
import com.zenith.Proxy;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandSource;
import com.zenith.discord.Embed;
import com.zenith.event.chat.WhisperChatEvent;
import com.zenith.module.api.Module;
import com.zenith.util.ChatUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static org.icetank.ExtraPearlLoader.PLUGIN_CONFIG;
import static com.github.rfresh2.EventConsumer.of;

public class ExtraPearlModule extends Module {
    @Override
    public boolean enabledSetting() {
        return PLUGIN_CONFIG.pearlLoader.enabled;
    }

    public static final List<String> loadSynonyms = List.of("load", "tp", "pearl", "pull", "I AM FUCKING DYING PLEASE LOAD MY PEARL".toLowerCase());

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(WhisperChatEvent.class, this::onWhisper)
        );
    }

    private void onWhisper(WhisperChatEvent event) {
        if (!PLUGIN_CONFIG.pearlLoader.enabled || event.outgoing()) return;

        String msg = event.message().trim().toLowerCase();
        if (msg.startsWith("!")) msg = msg.substring(1).trim(); // Remove prefix if present
        if (!loadSynonyms.stream().anyMatch(msg::startsWith)) return;

        var sender = event.sender();
        String name = sender.getName();
        UUID uuid = sender.getProfileId();

        List<String> allowedList = PLUGIN_CONFIG.pearlLoader.allowed.get(uuid);
        if (allowedList == null || allowedList.isEmpty()) {
            info("No pearls assigned to " + name);
            return;
        }

        // Sample message: "load [pearlId] [noise]"
        String[] parts = msg.trim().split("\\s+");
        if (parts.length < 1) {
            info("Invalid command: " + name + " sent: " + msg);
            return;
        }
        String pearl;

        // If guessPearlId is enabled we look for a pearlId matching the players name if no other pearlId matches
        if (parts.length > 1) {
            // Second part is either noise or a pearlId
            String possiblePearlId = getAllowlistMatch(allowedList, parts[1]);
            if (possiblePearlId != null) {
                pearl = possiblePearlId;
            } else if (PLUGIN_CONFIG.pearlLoader.guessPearlId) {
                String guessedPearlId = getAllowlistMatch(allowedList, name);
                if (guessedPearlId != null) {
                    pearl = guessedPearlId;
                } else {
                    info("No matching pearlId found for " + name + " with arg: " + parts[1]);
                    sendClientPacketAsync(ChatUtil.getWhisperChatPacket(name, responseAndNoise("No pearls found for you. Specify a pearl ID.")));
                    return;
                }
            } else {
                info("No matching pearlId found for " + name + " with arg: " + parts[1]);
                sendClientPacketAsync(ChatUtil.getWhisperChatPacket(name, responseAndNoise("No pearls found for you. Specify a pearl ID.")));
                return;
            }
        } else if (PLUGIN_CONFIG.pearlLoader.guessPearlId) {
            // See if the allow list contains a pearlId with the player's name
            String possiblePearlId = getAllowlistMatch(allowedList, name);
            if (possiblePearlId != null) {
                pearl = possiblePearlId;
            } else {
                info("No pearlId provided and no guess available for " + name);
                sendClientPacketAsync(ChatUtil.getWhisperChatPacket(name, responseAndNoise("No pearls found for you. Specify a pearl ID.")));
                return;
            }
        } else {
            info("No pearlId provided and guessing disabled for " + name);
            sendClientPacketAsync(ChatUtil.getWhisperChatPacket(name, responseAndNoise("Specify a pearl ID to load.")));
            return;
        }

        String zenithPearlId = findPearlIdMatch(pearl);
        if (zenithPearlId == null) {
            info("Something went wrong determining pearlId for " + name + " pearl: " + pearl);
            sendClientPacketAsync(ChatUtil.getWhisperChatPacket(name, responseAndNoise("Internal error determining pearl ID.")));
            return;
        }

        discordAndIngameNotification(Embed.builder()
                        .title("Received Whisper")
            .addField("Sender", name)
            .addField("Pearl", zenithPearlId)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(sender.getProfileId()).toString())
        );

        var ctx = CommandContext.create("pl load " + zenithPearlId, PearlPlusCommandSource.INSTANCE);
        ctx.getData().put("PearlPlusSender", sender);
        Globals.COMMAND.execute(ctx);

        var embed = ctx.getEmbed();
        if (embed == null) {
            error("No embed generated for " + name);
            return;
        }
        String resp = embed.isTitlePresent() ? ChatUtil.sanitizeChatMessage(embed.title()) : "Loaded";
        discordAndIngameNotification(embed);

        sendClientPacketAsync(ChatUtil.getWhisperChatPacket(name, responseAndNoise(resp)));
    }

    /**
     * Appends a random alphanumeric "noise" string to the given base response.
     * <p>
     * The noise is enclosed in square brackets and consists of random alphanumeric characters.
     * The length of the noise is at least 8 characters, or one third of the base response length, whichever is greater.
     * <p>
     * This is used to make responses unique or harder to automatically parse, for anti-bot or anti-spam purposes.
     *
     * @param baseResponse the original response string to which noise will be appended
     * @return the response string with appended noise in the format: {@code baseResponse [randomNoise]}
     */
    private String responseAndNoise(String baseResponse) {
        int length = baseResponse.length();
        baseResponse += " [";
        baseResponse += randomAlphanumeric(Math.max(8, length / 3));
        baseResponse += "]";
        return baseResponse;
    }

    @Nullable
    private String getAllowlistMatch(List<String> allowedList, String pearlId) {
        for (String allowed : allowedList) {
            if (allowed.equalsIgnoreCase(pearlId)) {
                return allowed;
            }
        }
        return null;
    }

    @Nullable
    private String findPearlIdMatch(String pearlId) {
        var pearls = Globals.CONFIG.client.extra.pearlLoader.pearls;
        for (var pearl : pearls) {
            if (pearl.id().equalsIgnoreCase(pearlId)) {
                return pearl.id();
            }
        }
        return null;
    }

    public static class PearlPlusCommandSource implements CommandSource {
        private static final String SENDER_KEY = "PearlPlusSender";

        public static final PearlPlusCommandSource INSTANCE = new PearlPlusCommandSource();
        @Override public String name() { return "Pearl+"; }
        @Override public boolean validateAccountOwner(CommandContext ctx) { return false; }
        @Override
        public void logEmbed(CommandContext ctx, Embed embed) {
        }
    }

    private String randomAlphanumeric(int count) {
        StringBuilder builder = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < count; i++) {
            int index = (int) (Math.random() * chars.length());
            builder.append(chars.charAt(index));
        }
        return builder.toString();
    }
}