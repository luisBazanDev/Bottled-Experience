package pl.inh.bottleExp;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import pl.inh.bottleExp.commands.ReloadCommand;
import pl.inh.bottleExp.listeners.PlayerInteractListener;

public class BottleExp extends JavaPlugin {
  Logger logger;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    logger = getLogger();
    
    getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
    getCommand("bottleexpreload").setExecutor(new ReloadCommand(this));

    logger.info("enabled");
  }

  @Override
  public void onDisable() {
    logger.info("disabled");
  }

  

}
