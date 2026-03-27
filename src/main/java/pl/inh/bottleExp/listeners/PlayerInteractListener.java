package pl.inh.bottleExp.listeners;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.inh.bottleExp.BottleExp;

public class PlayerInteractListener implements Listener {
  private BottleExp plugin;
  private NamespacedKey expKey;

  public PlayerInteractListener(BottleExp plugin) {
    this.plugin = plugin;
    this.expKey = new NamespacedKey(plugin, "stored_exp");
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e) {
    if (e.getItem() == null) return;
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;

    e.setCancelled(true);

    if (e.getItem().getType() == Material.GLASS_BOTTLE) {
      handleGlassBottle(e);
    } else if (e.getItem().getType() == Material.EXPERIENCE_BOTTLE) {
      handleExpBottle(e);
    }
  }

  public void handleGlassBottle(PlayerInteractEvent e) {
    Player p = e.getPlayer();

    int totalExp = getTrueExp(p);
    int playerLevel = p.getLevel();

    if (totalExp <= 0) return;

    int maxLevels = plugin.getConfig().getInt("bottle.max-levels", 30);

    if (playerLevel > maxLevels) {
      int expAtMax = getTotalExpForLevel(maxLevels);
      int expLeft = totalExp - expAtMax;

      p.setLevel(0);
      p.setExp(0f);
      p.setTotalExperience(0);
      p.giveExp(expLeft);

      totalExp = expAtMax;
      playerLevel = maxLevels;
    } else {
      p.setLevel(0);
      p.setExp(0f);
      p.setTotalExperience(0);
    }

    if (p.getInventory().firstEmpty() == -1) {
      // brak miejsca — oddaj exp z powrotem
      p.giveExp(totalExp);
      return;
    }

    ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
    ItemMeta meta = item.getItemMeta();

    String name = plugin.getConfig().getString("bottle.name");
    List<String> lore = plugin.getConfig().getStringList("bottle.lore");

    final int finalTotalExp = totalExp;
    final int finalPlayerLevel = playerLevel;

    meta.setDisplayName(name
        .replace("%levels%", String.valueOf(finalPlayerLevel))
        .replace("%exp%", String.valueOf(finalTotalExp))
    );
    lore.replaceAll(line -> line
        .replace("%exp%", String.valueOf(finalTotalExp))
        .replace("%levels%", String.valueOf(finalPlayerLevel))
    );
    meta.setLore(lore);
    meta.getPersistentDataContainer().set(expKey, PersistentDataType.INTEGER, finalTotalExp);

    item.setItemMeta(meta);
    p.getInventory().addItem(item);
  }

  public void handleExpBottle(PlayerInteractEvent e) {
    Player p = e.getPlayer();

    ItemMeta meta = e.getItem().getItemMeta();
    if (meta == null) return;

    if (!meta.getPersistentDataContainer().has(expKey, PersistentDataType.INTEGER)) return;

    int storedExp = meta.getPersistentDataContainer().get(expKey, PersistentDataType.INTEGER);
    int currentExp = getTrueExp(p);

    p.setLevel(0);
    p.setExp(0f);
    p.setTotalExperience(0);
    p.giveExp(currentExp + storedExp);

    e.getItem().setAmount(e.getItem().getAmount() - 1);
  }

  private int getTrueExp(Player p) {
    int level = p.getLevel();
    float progress = p.getExp();
    int totalForLevel = getTotalExpForLevel(level);
    int toNextLevel = getXpToLevel(level);
    return totalForLevel + Math.round(progress * toNextLevel);
  }

  private int getTotalExpForLevel(int level) {
    if (level <= 16) {
      return (int)(Math.pow(level, 2) + 6 * level);
    } else if (level <= 31) {
      return (int)(2.5 * Math.pow(level, 2) - 40.5 * level + 360);
    } else {
      return (int)(4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
    }
  }

  private int getXpToLevel(int level) {
    if (level <= 15) return 2 * level + 7;
    else if (level <= 30) return 5 * level - 38;
    else return 9 * level - 158;
  }
}