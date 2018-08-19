package net.pl3x.bukkit.fangs;

import net.pl3x.bukkit.fangs.command.CmdFangs;
import net.pl3x.bukkit.fangs.configure.Config;
import net.pl3x.bukkit.fangs.configure.Lang;
import net.pl3x.bukkit.fangs.listener.PlantListener;
import net.pl3x.bukkit.fangs.listener.ShootListener;
import net.pl3x.bukkit.fangs.listener.SummonListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Fangs extends JavaPlugin {
    private FangManager fangManager;

    @Override
    public void onEnable() {
        Config.reload(this);
        Lang.reload(this);

        this.fangManager = new FangManager(this);

        getServer().getPluginManager().registerEvents(new PlantListener(this), this);
        getServer().getPluginManager().registerEvents(new ShootListener(this), this);
        getServer().getPluginManager().registerEvents(new SummonListener(this), this);

        getCommand("fangs").setExecutor(new CmdFangs(this));
    }

    @Override
    public void onDisable() {
        if (fangManager != null) {
            fangManager.cancelAllCooldowns();
            fangManager = null;
        }
    }

    public FangManager getFangManager() {
        return fangManager;
    }
}
