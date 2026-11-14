package lol.bkd.vitalVerdict;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class VVCommand implements BasicCommand {
    private static final List<String> SUBCOMMANDS = List.of("start", "set", "refresh", "stop");

    @Override
    public void execute(final CommandSourceStack source, final String[] args) {
        CommandSender sender = source.getSender();

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length > 1) {
                    sender.sendRichMessage("<red>Unrecognized parameters provided.");
                    return;
                }
                VitalVerdict.resetConfirmation(source.getSender());
            }
            case "refresh" -> {
                if (args.length > 1) {
                    sender.sendRichMessage("<red>Unrecognized parameters provided.");
                    return;
                }
                VitalVerdict.refresh();
                sender.sendRichMessage("<green>Refreshed.");
            }
            case "set" -> {
                if (args.length < 2) {
                    sender.sendRichMessage("<red>A target is required.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendRichMessage("<red>Please specify what to change the life count to.");
                    return;
                }
                if (args.length > 3) {
                    sender.sendRichMessage("<red>Unrecognized parameters provided.");
                    return;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendRichMessage("<red>Player could not be found.");
                    return;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendRichMessage("<red>The life count must be a non-negative whole number.");
                    return;
                }
                if (amount < 0) {
                    sender.sendRichMessage("<red>The life count must be a non-negative whole number.");
                    return;
                }

                VitalVerdict.setLives(target, amount);
                sender.sendRichMessage("<green>Set " + target.getName() + "'s lives to " + amount);
            }
            case "confirm" -> {
                if (args.length > 1) {
                    sender.sendRichMessage("<red>Unrecognized parameters provided.");
                    return;
                }
                if (!VitalVerdict.confirmationCheck()) {
                    sender.sendRichMessage("<red>Nothing to confirm. It may have expired.");
                    return;
                }
                VitalVerdict.resetGame();
            }
            case "stop" -> {
                if (args.length > 1) {
                    sender.sendRichMessage("<red>Unrecognized parameters provided.");
                    return;
                }
                VitalVerdict.stopGame();
                sender.sendRichMessage("<yellow>Game stopped.");
            }
            default -> sendUsage(sender);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /vv <start|refresh|stop|set <player> <lives>>", NamedTextColor.RED));
    }

    // method partially from https://docs.papermc.io/paper/dev/command-api/misc/basic-command/#adding-suggestions
    @Override
    public Collection<String> suggest(final CommandSourceStack source, final String[] args) {
        if (args.length <= 1) {
            String typed = args.length == 0 ? "" : args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(typed)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "vitalverdict.vv.use";
    }
}