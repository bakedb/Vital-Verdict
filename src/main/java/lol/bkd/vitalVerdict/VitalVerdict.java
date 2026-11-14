package lol.bkd.vitalVerdict;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class VitalVerdict extends JavaPlugin {
    private static VitalVerdict instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        this.saveDefaultConfig();
        registerCommand("vv", new VVCommand());
    }

    public static VitalVerdict getInstance() {
        return instance;
    }

    public static int getLives(Player player) {
        NamespacedKey lives = new NamespacedKey(VitalVerdict.getInstance(), "lives");
        return player.getPersistentDataContainer().getOrDefault(lives, PersistentDataType.INTEGER, getInstance().getConfig().getInt("lives"));
    }

    public static void setLives(Player player, int newLives) {
        NamespacedKey lives = new NamespacedKey(VitalVerdict.getInstance(), "lives");
        player.getPersistentDataContainer().set(lives, PersistentDataType.INTEGER, newLives);
        if (newLives <= 0) {
            VitalVerdict.onFinalDeath(player);
        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        updateColor(player, newLives);
    }

    public static void updateColor(Player player, int lives) {
        if (lives >= VitalVerdict.getInstance().getConfig().getInt("colors.dark_green")) {
            VitalVerdict.setNameColor(player, NamedTextColor.DARK_GREEN);
        } else if (lives >= VitalVerdict.getInstance().getConfig().getInt("colors.green")) {
            VitalVerdict.setNameColor(player, NamedTextColor.GREEN);
        } else if (lives >= VitalVerdict.getInstance().getConfig().getInt("colors.yellow")) {
            VitalVerdict.setNameColor(player, NamedTextColor.YELLOW);
        } else if (lives >= VitalVerdict.getInstance().getConfig().getInt("colors.red")) {
            VitalVerdict.setNameColor(player, NamedTextColor.RED);
        } else {
            VitalVerdict.setNameColor(player, NamedTextColor.GRAY);
        }
    }

    public static void refresh() {
        for (Player i : Bukkit.getOnlinePlayers()) {
            AttributeInstance attribute = i.getAttribute(Attribute.MAX_HEALTH);
            if (attribute != null) attribute.setBaseValue(VitalVerdict.getInstance().getConfig().getInt("max-health"));
            VitalVerdict.updateColor(i, VitalVerdict.getLives(i));
        }
    }

    public static class DeathListener implements Listener {
        @EventHandler
        public void onDeath(PlayerDeathEvent event) {
            Player player = event.getEntity();
            VitalVerdict.setLives(player, VitalVerdict.getLives(player) - 1);
        }
    }

    public static void onFinalDeath(Player player) {
        World world = player.getWorld();
        world.strikeLightningEffect(player.getLocation());
        player.setGameMode(GameMode.SPECTATOR);
    }

    public static void setNameColor(Player player, TextColor color) {
        Component name = Component.text(player.getName(), color);

        player.displayName(name);
        player.playerListName(name);

        if (color instanceof NamedTextColor named) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = "vv_" + NamedTextColor.NAMES.key(named);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                team.color(named);
            }
            team.addEntry(player.getName());
        }
    }

    public static class judgementTimer {
        public static void startTimer() {
            int time = VitalVerdict.getInstance().getConfig().getInt("interval");
            if (time < 60) time = 1800;
            firstHalf(time / 2);
            minusOneMinute(time - 60);
            fullTimer(time);
        }
        public static void firstHalf(int interval) {
            if (VitalVerdict.getInstance().getConfig().getInt("half-interval-warning") == 1) {
                Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                    Bukkit.broadcast(Component.text(VitalVerdict.getInstance().getConfig().getString("half-interval-message", "15 minutes remain until judgment.")));
                }, 20L * interval);
            }

        }
        public static void minusOneMinute(int interval) {
            if (VitalVerdict.getInstance().getConfig().getInt("one-minute-warning") == 1) {
                Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                    Bukkit.broadcast(Component.text(VitalVerdict.getInstance().getConfig().getString("one-minute-message", "1 minute remains until judgment.")));
                }, 20L * interval);
            }
        }
        public static void fullTimer(int interval) {
            Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                judgementHandler.triggerJudgement();
            }, 20L * interval);
        }
    }

    public static class judgementHandler {
        public static void triggerJudgement() {
            Audience target = Audience.audience(Bukkit.getOnlinePlayers());
            final Title title3 = Title.title(Component.text("3", NamedTextColor.GREEN), Component.empty());

            target.showTitle(title3);
            Sound sound3 = Sound.sound(Key.key("block.end_portal_frame.fill"), Sound.Source.MASTER, 1F, 1F);
            target.playSound(sound3);

            final Title title2 = Title.title(Component.text("2", NamedTextColor.YELLOW), Component.empty());

            Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                target.showTitle(title2);
                Sound sound2 = Sound.sound(Key.key("block.end_portal_frame.fill"), Sound.Source.MASTER, 1F, 1F);
                target.playSound(sound2);
            }, 60L);

            final Title title1 = Title.title(Component.text("1", NamedTextColor.RED), Component.empty());

            Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                target.showTitle(title1);
                Sound sound1 = Sound.sound(Key.key("block.end_portal_frame.fill"), Sound.Source.MASTER, 1F, 1F);
                target.playSound(sound1);
            }, 120L);

            Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                judgePlayer();
            }, 180L);
        }

        public static void judgePlayer() {
            Player lowest = null;

            for (Player i : Bukkit.getOnlinePlayers()) {
                if (i.getGameMode() == GameMode.SPECTATOR) continue;
                if (i.isDead()) continue;
                if (lowest == null || i.getHealth() < lowest.getHealth()) {
                    lowest = i;
                }
            }

            if (lowest != null) {
                final Player judged = lowest;
                final Title judgedTitle = Title.title(Component.text("You have been judged.", NamedTextColor.RED), Component.empty());
                judged.showTitle(judgedTitle);
                Sound judgedSound = Sound.sound(Key.key("entity.warden.death"), Sound.Source.MASTER, 1F, 1F);
                judged.playSound(judgedSound);
                Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                    if (!(judged.isDead()) && judged.isOnline()) judged.setHealth(0);
                }, 100L);
                for (Player i : Bukkit.getOnlinePlayers()) {
                    if (i.equals(judged)) continue;
                    final Title notJudgedTitle = Title.title(Component.text("You are safe.", NamedTextColor.GREEN), Component.empty());
                    i.showTitle(notJudgedTitle);
                    Sound notJudgedSound = Sound.sound(Key.key("entity.player.levelup"), Sound.Source.MASTER, 1F, 1F);
                    i.playSound(notJudgedSound);
                }

                VitalVerdict.judgementTimer.startTimer();
                // TODO: break up tied results by killing the player with the highest life count first
            }
        }
    }

    public static void resetGame() {
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
            if (VitalVerdict.getInstance().getConfig().getInt("keep-inventory") == 1) {
                world.setGameRule(GameRules.KEEP_INVENTORY, true);
            }
        }

        VitalVerdict.stopGame();
        for (Player i : Bukkit.getOnlinePlayers()) {
            VitalVerdict.setLives(i, VitalVerdict.getInstance().getConfig().getInt("lives"));
        }
        VitalVerdict.refresh();
        for (Player i : Bukkit.getOnlinePlayers()) {
            i.setHealth(i.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
        Bukkit.broadcast(Component.text("Starting...", NamedTextColor.AQUA));
        judgementTimer.startTimer();
    }

    private static long confirmExpiry = 0;

    public static void resetConfirmation(CommandSender sender) {
        confirmExpiry = System.currentTimeMillis() + 30_000L;

        sender.sendMessage(Component.text("This will reset everyone's lives and start the game.", NamedTextColor.RED));
        sender.sendMessage(Component.text("[Click to confirm]").clickEvent(ClickEvent.runCommand("/vv confirm")));
    }

    public static boolean confirmationCheck() {
        if (System.currentTimeMillis() > confirmExpiry) return false;
        confirmExpiry = 0;
        return true;
    }

    public static void stopGame() {
        Bukkit.getScheduler().cancelTasks(getInstance());
    }
}