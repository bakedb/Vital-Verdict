package lol.bkd.vitalVerdict;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import javax.inject.Named;

public final class VitalVerdict extends JavaPlugin {
    private static VitalVerdict instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        this.saveDefaultConfig();
    }

    public static VitalVerdict getInstance() {
        return instance;
    }

    public static class DeathListener implements Listener {
        @EventHandler
        public void onDeath(PlayerDeathEvent event) {
            NamespacedKey lives = new NamespacedKey(VitalVerdict.getInstance(), "lives");
            Player player = event.getEntity();
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            int currentLives = pdc.getOrDefault(lives, PersistentDataType.INTEGER, VitalVerdict.getInstance().getConfig().getInt("lives"));
            currentLives--;
            pdc.set(lives, PersistentDataType.INTEGER, currentLives);
            if (currentLives <= 0) {
                VitalVerdict.onFinalDeath(player);
            }

            if (currentLives >= VitalVerdict.getInstance().getConfig().getInt("colors.dark_green")) {
                VitalVerdict.setNameColor(player, NamedTextColor.DARK_GREEN);
            } else if (currentLives >= VitalVerdict.getInstance().getConfig().getInt("colors.green")) {
                VitalVerdict.setNameColor(player, NamedTextColor.GREEN);
            } else if (currentLives >= VitalVerdict.getInstance().getConfig().getInt("colors.yellow")) {
                VitalVerdict.setNameColor(player, NamedTextColor.YELLOW);
            } else if (currentLives >= VitalVerdict.getInstance().getConfig().getInt("colors.red")) {
                VitalVerdict.setNameColor(player, NamedTextColor.RED);
            }

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
            String teamName = named + "_name";
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                team.color(named);
            }
            team.addEntry(player.getName());
        }
    }

    public class judgementTimer {
        int time = VitalVerdict.getInstance().getConfig().getInt("interval");
        public void startTimer() {
            if (time < 60) time = 1800;
            firstHalf(time / 2);
            minusOneMinute(time - 60);
            fullTimer(time);
        }
        public void firstHalf(int interval) {
            if (VitalVerdict.getInstance().getConfig().getInt("half-interval-warning") == 1) {
                Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                    Bukkit.broadcast(Component.text(VitalVerdict.getInstance().getConfig().getString("half-interval-message", "15 minutes remain until judgment.")));
                }, 20L * interval);
            }

        }
        public void minusOneMinute(int interval) {
            if (VitalVerdict.getInstance().getConfig().getInt("one-minute-warning") == 1) {
                Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                    Bukkit.broadcast(Component.text(VitalVerdict.getInstance().getConfig().getString("one-minute-message", "1 minute remains until judgment.")));
                }, 20L * interval);
            }
        }
        public void fullTimer(int interval) {
            Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                judgementHandler.triggerJudgement();
            }, 20L * interval);
        }
    }

    public class judgementHandler {
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
            }, 20L);

            final Title title1 = Title.title(Component.text("1", NamedTextColor.RED), Component.empty());

            Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                target.showTitle(title1);
                Sound sound1 = Sound.sound(Key.key("block.end_portal_frame.fill"), Sound.Source.MASTER, 1F, 1F);
                target.playSound(sound1);
            }, 40L);

            Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                judgePlayer();
            }, 60L);
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
                Sound judgedSound = Sound.sound(Key.key("entity.warden.agitated"), Sound.Source.MASTER, 1F, 1F);
                judged.playSound(judgedSound);
                Bukkit.getScheduler().runTaskLater(VitalVerdict.getInstance(), () -> {
                    if (!(judged.isDead()) && judged.isOnline()) judged.setHealth(0);
                }, 100L);

                // TODO: more messages for players who survive
                // TODO: break up tied results by killing the player with the highest life count first
            }
        }
    }
}
