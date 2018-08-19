package net.pl3x.bukkit.fangs.configure;

import net.pl3x.bukkit.fangs.Fangs;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    public static String LANGUAGE_FILE;

    public static boolean SUMMON_ENABLED;
    public static boolean SUMMON_REQUIRE_SNEAK_SHIFT;
    public static int SUMMON_COOLDOWN;
    public static Material SUMMON_TOOL;

    public static boolean PLANT_ENABLED;
    public static Material PLANT_BASE_TYPE;

    public static void reload(Fangs plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        LANGUAGE_FILE = config.getString("language-file", "lang-en.yml");

        SUMMON_ENABLED = config.getBoolean("summon.enabled", true);
        SUMMON_REQUIRE_SNEAK_SHIFT = config.getBoolean("summon.require-sneak-shift", true);
        SUMMON_COOLDOWN = config.getInt("summon.cooldown", 2);
        try {
            //noinspection ConstantConditions
            SUMMON_TOOL = Material.matchMaterial(config.getString("summon.tool", "IRON_NUGGET"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid summon.tool specified. Defaulting to IRON_NUGGET.");
            SUMMON_TOOL = Material.IRON_NUGGET;
        }

        PLANT_ENABLED = config.getBoolean("plant.enabled", true);
        try {
            //noinspection ConstantConditions
            PLANT_BASE_TYPE = Material.getMaterial(config.getString("plant.base-type", "STONE").toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid plant.base-type specified. Defaulting to STONE.");
            PLANT_BASE_TYPE = Material.STONE;
        }
    }
}
