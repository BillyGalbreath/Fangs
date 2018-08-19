package net.pl3x.bukkit.fangs.configure;

import net.pl3x.bukkit.fangs.Fangs;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class Lang {
    public static String COMMAND_NO_PERMISSION;

    public static String DEATH_BY_SUMMONS;
    public static String DEATH_BY_PLANT;
    public static String DEATH_BY_ARROW;

    public static void reload(Fangs plugin) {
        String langFile = Config.LANGUAGE_FILE;
        File configFile = new File(plugin.getDataFolder(), langFile);
        if (!configFile.exists()) {
            plugin.saveResource(Config.LANGUAGE_FILE, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        COMMAND_NO_PERMISSION = config.getString("command-no-permission", "&4You do not have permission for that command!");

        DEATH_BY_SUMMONS = config.getString("death-by-summons", "&e{player} killed by {attacker} with fangs");
        DEATH_BY_PLANT = config.getString("death-by-plant", "&e{player} killed by carnivorous plants");
        DEATH_BY_ARROW = config.getString("death-by-arrow", "&e{player} killed by {attacker} with fangs bow");
    }

    public static void send(CommandSender recipient, String message) {
        if (message == null) {
            return; // do not send blank messages
        }
        message = ChatColor.translateAlternateColorCodes('&', message);
        if (ChatColor.stripColor(message).isEmpty()) {
            return; // do not send blank messages
        }

        for (String part : message.split("\n")) {
            recipient.sendMessage(part);
        }
    }
}
