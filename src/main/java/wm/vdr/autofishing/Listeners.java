package wm.vdr.autofishing;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

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
