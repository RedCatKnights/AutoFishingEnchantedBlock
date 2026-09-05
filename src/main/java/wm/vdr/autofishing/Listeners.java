package wm.vdr.autofishing;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Listeners implements Listener {

    private final AutoFishing main = AutoFishing.instance;
    private final NmsInteraction nmsInteraction = new NmsInteraction(main);
    private final TimeoutTracker timeoutTracker = new TimeoutTracker();

    @EventHandler
    public void onFishing(PlayerFishEvent e) {
        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();
        UUID hookId = e.getHook().getUniqueId();

        if(e.isCancelled()) return;

        if(!player.hasPermission("autofishing.use") || !main.getPlayerDataUtil().isAuto(playerId)) {
            timeoutTracker.finish(playerId, hookId);
            return;
        }

        if(e.getState() == State.FISHING) {
            scheduleTimeout(player, hookId);
            return;
        }

        if(e.getState().name().equals("LURED")) return;

        if(e.getState() == State.BITE) {
            ServerTypeUtil.runAtPlayer(main, player, () -> {
                        if(!timeoutTracker.finish(playerId, hookId)) return;
                        if(!canAutoFish(player)) return;
                        EquipmentSlot hand = getHand(player);
                        if(hand == null) return;
                        doRightClick(player, hand);
                    }, () -> timeoutTracker.finish(playerId, hookId),
                    main.getConfig().getInt("Ticks_After_Bitten"));
            return;
        }

        timeoutTracker.finish(playerId, hookId);

        if(e.getState() == State.CAUGHT_FISH) {
            ServerTypeUtil.runAtPlayer(main, player, () -> {
                if(!canAutoFish(player) || timeoutTracker.hasActiveCast(playerId)) return;
                EquipmentSlot hand = getHand(player);
                if(hand == null) return;
                doRightClick(player, hand);
            }, main.getConfig().getInt("Ticks_After_Caught"));
            return;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        timeoutTracker.clear(e.getPlayer().getUniqueId());
    }

    // 金床でのブロック判定
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        AnvilInventory inventory = e.getInventory();
        ItemStack firstSlot = inventory.getItem(0); // 左スロット（合成元の竿）
        ItemStack result = e.getResult();           // 合成後の結果

        if (firstSlot == null || result == null) return;

        // 左スロットのアイテムが AutoFishing の PDC タグを持っているか判定
        if (!firstSlot.hasItemMeta() ||
                !firstSlot.getItemMeta().getPersistentDataContainer().has(main.key, PersistentDataType.BYTE)) {
            return;
        }

        // config.yml からブロック対象のエンチャントリストを取得
        List<String> blockedEnchants = main.getConfig().getStringList("Blocked_Enchantments");

        if (blockedEnchants == null || blockedEnchants.isEmpty()) return;

        // 合成結果に禁止エンチャントが含まれているかチェック
        for (Enchantment enchantment : result.getEnchantments().keySet()) {
            String enchantKey = enchantment.getKey().getKey().toUpperCase();

            if (blockedEnchants.contains(enchantKey)) {
                // 結果スロットを null にして取り出しを不可にする
                e.setResult(null);
                break;
            }
        }
    }

    // エンチャントテーブルでの表示ブロック判定
    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent e) {
        ItemStack item = e.getItem();

        if (item == null) return;

        // 対象のアイテムが AutoFishing の PDC タグを持っているか判定
        if (!item.hasItemMeta() ||
                !item.getItemMeta().getPersistentDataContainer().has(main.key, PersistentDataType.BYTE)) {
            return;
        }

        // config.yml からブロック対象のエンチャントリストを取得
        List<String> blockedEnchants = main.getConfig().getStringList("Blocked_Enchantments");

        if (blockedEnchants == null || blockedEnchants.isEmpty()) return;

        // 提示される3つのエンチャント枠をチェック
        EnchantmentOffer[] offers = e.getOffers();
        for (int i = 0; i < offers.length; i++) {
            EnchantmentOffer offer = offers[i];
            if (offer == null || offer.getEnchantment() == null) continue;

            String enchantKey = offer.getEnchantment().getKey().getKey().toUpperCase();

            // 提示されたエンチャントがブロックリストに含まれている場合、枠自体を null にして非表示・選択不可にする
            if (blockedEnchants.contains(enchantKey)) {
                offers[i] = null;
            }
        }
    }

    // 実際にエンチャントが付与される瞬間のブロック判定（30レベルエンチャント時の副次付与を確実に除去）
    @EventHandler
    public void onEnchantItem(EnchantItemEvent e) {
        ItemStack item = e.getItem();

        if (item == null) return;

        // 対象のアイテムが AutoFishing の PDC タグを持っているか判定
        if (!item.hasItemMeta() ||
                !item.getItemMeta().getPersistentDataContainer().has(main.key, PersistentDataType.BYTE)) {
            return;
        }

        // config.yml からブロック対象のエンチャントリストを取得
        List<String> blockedEnchants = main.getConfig().getStringList("Blocked_Enchantments");

        if (blockedEnchants == null || blockedEnchants.isEmpty()) return;

        // 付与予定のエンチャントマップから禁止エンチャントを除外
        Map<Enchantment, Integer> enchantsToAdd = e.getEnchantsToAdd();
        enchantsToAdd.keySet().removeIf(enchantment -> {
            String enchantKey = enchantment.getKey().getKey().toUpperCase();
            return blockedEnchants.contains(enchantKey);
        });
    }

    private void scheduleTimeout(Player player, UUID hookId) {
        UUID playerId = player.getUniqueId();
        timeoutTracker.start(playerId, hookId);

        int timeout = main.getConfig().getInt("Ticks_Before_Timeout");
        if(timeout <= 0) return;

        Runnable retire = () -> timeoutTracker.finish(playerId, hookId);

        ServerTypeUtil.runAtPlayer(main, player, () -> {
            if(!timeoutTracker.finish(playerId, hookId)) return;
            if(!canAutoFish(player)) return;

            EquipmentSlot hand = getHand(player);
            if(hand == null || !doRightClick(player, hand)) return;

            ServerTypeUtil.runAtPlayer(main, player, () -> {
                if(!canAutoFish(player) || timeoutTracker.hasActiveCast(playerId)) return;
                EquipmentSlot recastHand = getHand(player);
                if(recastHand != null) doRightClick(player, recastHand);
            }, main.getConfig().getInt("Ticks_After_Caught"));
        }, retire, timeout);
    }

    private boolean canAutoFish(Player player) {
        return player.isOnline()
                && player.hasPermission("autofishing.use")
                && main.getPlayerDataUtil().isAuto(player.getUniqueId());
    }

    private boolean doRightClick(Player player, EquipmentSlot hand) {
        if(main.getConfig().getBoolean("Only_Specific_Rod.Enable")) {
            ItemStack item = hand == EquipmentSlot.HAND
                    ? player.getInventory().getItemInMainHand()
                    : player.getInventory().getItemInOffHand();
            if(!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(main.key, PersistentDataType.BYTE)) return false;
        }

        return nmsInteraction.useItem(player, hand);
    }

    private EquipmentSlot getHand(Player player) {
        return player.getInventory().getItemInMainHand().getType().equals(Material.FISHING_ROD) ?
                EquipmentSlot.HAND : player.getInventory().getItemInOffHand().getType().equals(Material.FISHING_ROD) ?
                EquipmentSlot.OFF_HAND : null;
    }

}
