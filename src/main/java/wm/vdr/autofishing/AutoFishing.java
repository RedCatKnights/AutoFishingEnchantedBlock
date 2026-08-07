package wm.vdr.autofishing;

import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class AutoFishing extends JavaPlugin {

    public static AutoFishing instance;

    private PlayerDataUtil playerDataUtil;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        messages = new Messages(this);
        playerDataUtil = new PlayerDataUtil();

        getServer().getPluginManager().registerEvents(new Listeners(), this);
        Executors executors = new Executors();
        getCommand("autofishing").setExecutor(executors);
        getCommand("autofishing").setTabCompleter(executors);

        int pluginId = 16050;
        Metrics metrics = new Metrics(this, pluginId);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public PlayerDataUtil getPlayerDataUtil() {
        return playerDataUtil;
    }

    public String getMessage(String path, String... replacements) {
        return messages.get(path, replacements);
    }

    public List<String> getMessageList(String path) {
        return messages.getList(path);
    }

    public void reloadMessages() {
        messages.reload();
    }

    public NamespacedKey key = new NamespacedKey(this, "FishingRod");
    public ItemStack getSpecificRod() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',getConfig().getString("Only_Specific_Rod.Rod_Name")));
        List<String> lore = new ArrayList<>();
        getConfig().getStringList("Only_Specific_Rod.Rod_Lore").forEach(s -> {
            lore.add(ChatColor.translateAlternateColorCodes('&',s));
        });
        meta.setLore(lore);

        int customModelData = getConfig().getInt("Only_Specific_Rod.Rod_Model_Data");
        if(customModelData != 0) {
            meta.setCustomModelData(customModelData);
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;

    }
}
