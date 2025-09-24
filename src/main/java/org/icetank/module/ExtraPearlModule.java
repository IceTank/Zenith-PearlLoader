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

import java.util.List;
import java.util.UUID;

import static org.icetank.ExtraPearlLoader.PLUGIN_CONFIG;
import static com.github.rfresh2.EventConsumer.of;

public class ExtraPearlModule extends Module {
    @Override
    public boolean enabledSetting() {
        return PLUGIN_CONFIG.pearlLoader.enabled;
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(WhisperChatEvent.class, this::onWhisper)
        );
    }

    private void onWhisper(WhisperChatEvent event) {
        if (!PLUGIN_CONFIG.pearlLoader.enabled || event.outgoing()) return;

        String msg = event.message().trim().toLowerCase();
        if (!msg.startsWith("load")) return;

        var sender = event.sender();
        String name = sender.getName();
        UUID uuid = sender.getProfileId();

        var allowedList = PLUGIN_CONFIG.pearlLoader.allowed.get(uuid);
        if (allowedList == null || allowedList.isEmpty()) {
            info("No pearls assigned to " + name);
            return;
        }

        String[] parts = msg.split("\\s+");
        String pearl;

        if (!PLUGIN_CONFIG.pearlLoader.allowNoiseAfterPearl) {
            if (parts.length > 2) {
                info("Extra arguments not allowed for " + name);
                return;
            }
        } else {
            if (parts.length > 3) {
                info("Too many arguments from " + name);
                return;
            }
            if (parts.length == 3 && !allowedList.contains(parts[1])) {
                info("Noise before pearl not allowed for " + name);
                return;
            }
        }
        if (parts.length == 1) {
            pearl = allowedList.getFirst();
        } else {
            String candidate = parts[1];
            pearl = (PLUGIN_CONFIG.pearlLoader.allowNoiseAfterPearl && !allowedList.contains(candidate))
                ? allowedList.getFirst()
                : candidate;
        }

        if (!allowedList.contains(pearl)) {
            info("Unauthorized load from " + name + " with arg: " + pearl);
            return;
        }

        discordAndIngameNotification(Embed.builder()
            .title("Recieved Whisper")
            .addField("Sender", name)
            .addField("Pearl", pearl)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(sender.getProfileId()).toString())
        );

        var ctx = CommandContext.create("pl load " + pearl, PearlPlusCommandSource.INSTANCE);
        ctx.getData().put("PearlPlusSender", sender);
        Globals.COMMAND.execute(ctx);

        var embed = ctx.getEmbed();
        if (embed == null) {
            error("No embed generated for " + name);
            return;
        }
        String resp = embed.isTitlePresent() ? ChatUtil.sanitizeChatMessage(embed.title()) : "Loaded";
        discordAndIngameNotification(embed);
        sendClientPacketAsync(ChatUtil.getWhisperChatPacket(name, resp));
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
}