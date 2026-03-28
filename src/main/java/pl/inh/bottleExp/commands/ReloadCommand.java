package pl.inh.bottleExp.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pl.inh.bottleExp.BottleExp;

public class ReloadCommand implements CommandExecutor {
  BottleExp plugin;

  public ReloadCommand(BottleExp plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String @NotNull [] args) {
    plugin.reloadConfig();
    sender.sendMessage("§7[§2Bottled Experience§7] §aConfig reloaded!");
    return true;
  }
}
