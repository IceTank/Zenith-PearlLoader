package org.icetank.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.api.minetools.MinetoolsApi;
import com.zenith.feature.api.minetools.model.MinetoolsUuidResponse;
import org.icetank.module.ExtraPearlModule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static org.icetank.ExtraPearlLoader.PLUGIN_CONFIG;

public class PearlLoaderCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
                .name("extraPearlLoader")
                .category(CommandCategory.MODULE)
                .description("""
                        Extra Pearl Loader Command.
                        
                        Aliases: epl, pl++
                        """)
                .usageLines(
                        "on/off",
                        "toggle on/off",
                        "info",
                        "allow add/remove/list <player> <pearlId>"
                )
                .aliases("epl", "pl++")
                .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("examplePlugin")
                .then(argument("toggle", toggle()).executes(c -> {
                    PLUGIN_CONFIG.pearlLoader.enabled = getToggle(c, "toggle");
                    // make sure to sync so the module is actually toggled
                    MODULE.get(ExtraPearlModule.class).syncEnabledFromConfig();
                    c.getSource().getEmbed()
                            // if no title is set, no embed response will be sent
                            // other properties like fields can be left unset without issues
                            .title("Example Plugin " + toggleStrCaps(PLUGIN_CONFIG.pearlLoader.enabled));
                }))
                .then(literal("info").executes(c -> {
                    c.getSource().getEmbed().title("Info for Extra Pearl Loader")
                            .addField("Whisper", "An allowed player can use /msg <proxy> load <pearlId> [noise] to load a pearl")
                            .addField("Configured Players", String.valueOf(PLUGIN_CONFIG.pearlLoader.allowed.size()));
                }))
                .then(literal("allow")
                        .then(literal("add")
                                .then(argument("player", wordWithChars())
                                        .then(argument("pearlId", wordWithChars()).executes(c -> {
                                            String player = c.getArgument("player", String.class);
                                            String pearlId = c.getArgument("pearlId", String.class);
                                            Optional<MinetoolsUuidResponse> result =
                                                    MinetoolsApi.INSTANCE.getProfileFromUsername(player);

                                            if (result.isPresent()) {
                                                UUID uuid = result.get().uuid();
                                                List<String> pearls = PLUGIN_CONFIG.pearlLoader.allowed.computeIfAbsent(uuid, k -> new java.util.ArrayList<>());
                                                if (pearls.contains(pearlId)) {
                                                    c.getSource().getEmbed()
                                                            .title("Player " + player + " already has access to pearlId " + pearlId);
                                                } else {
                                                    pearls.add(pearlId);
                                                    c.getSource().getEmbed()
                                                            .title("Added access to pearlId " + pearlId + " to player " + player)
                                                            .addField("UUID", uuid.toString())
                                                            .addField("PearlId access", String.join(", ", pearls));
                                                }
                                            } else {
                                                c.getSource().getEmbed()
                                                        .title("Player not found: " + player);
                                            }
                                        }))
                                )
                        )
                        .then(literal("del")
                                .then(argument("player", wordWithChars())
                                        .then(argument("pearlId", wordWithChars()).executes(c -> {
                                            String player = c.getArgument("player", String.class);
                                            String pearlId = c.getArgument("pearlId", String.class);
                                            Optional<MinetoolsUuidResponse> result =
                                                    MinetoolsApi.INSTANCE.getProfileFromUsername(player);

                                            if (result.isPresent()) {
                                                UUID uuid = result.get().uuid();

                                                if (pearlId.equalsIgnoreCase("all")) {
                                                    PLUGIN_CONFIG.pearlLoader.allowed.remove(uuid);
                                                    c.getSource().getEmbed()
                                                            .title("Removed all pearls from player " + player)
                                                            .addField("UUID", uuid.toString());
                                                    return;
                                                }

                                                List<String> pearls = PLUGIN_CONFIG.pearlLoader.allowed.get(uuid);
                                                if (pearls == null) {
                                                    c.getSource().getEmbed()
                                                            .title("Player " + player + " does not have access to any pearls");
                                                } else if (!pearls.contains(pearlId)) {
                                                    c.getSource().getEmbed()
                                                            .title("Player " + player + " does not have access to pearlId " + pearlId);
                                                } else {
                                                    pearls.remove(pearlId);
                                                    if (pearls.isEmpty()) {
                                                        PLUGIN_CONFIG.pearlLoader.allowed.remove(uuid);
                                                    }
                                                    c.getSource().getEmbed()
                                                            .title("Removed access to pearlId " + pearlId + " from player " + player)
                                                            .addField("UUID", uuid.toString())
                                                            .addField("PearlId access", pearls.isEmpty() ? "None" : String.join(", ", pearls));
                                                }
                                            } else {
                                                c.getSource().getEmbed()
                                                        .title("Player not found: " + player);
                                            }
                                        }))
                                )
                        )
                        .then(literal("list")
                                .then(argument("player", wordWithChars()).executes(c -> {
                                    String player = c.getArgument("player", String.class);
                                    Optional<MinetoolsUuidResponse> result =
                                            MinetoolsApi.INSTANCE.getProfileFromUsername(player);

                                    if (result.isPresent()) {
                                        UUID uuid = result.get().uuid();
                                        List<String> pearls = PLUGIN_CONFIG.pearlLoader.allowed.get(uuid);
                                        c.getSource().getEmbed()
                                                .title("PearlId access for player " + player)
                                                .addField("UUID", uuid.toString())
                                                .addField("PearlId access", pearls == null || pearls.isEmpty() ? "None" : String.join(", ", pearls));
                                    } else {
                                        c.getSource().getEmbed()
                                                .title("Player not found: " + player);
                                    }
                                }))
                        )
                );
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
                .primaryColor()
                .addField("Enabled", toggleStr(PLUGIN_CONFIG.pearlLoader.enabled))
                .addField("Configured Players", String.valueOf(PLUGIN_CONFIG.pearlLoader.allowed.size()));
    }
}
