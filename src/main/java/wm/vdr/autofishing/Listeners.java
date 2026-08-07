package wm.vdr.autofishing;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class Listeners implements Listener {

    private final AutoFishing main = AutoFishing.instance;
    private final NmsInteraction nmsInteraction = new NmsInteraction(main);

    @EventHandler
    public void onFishing(PlayerFishEvent e) {
        if(e.isCancelled()) return;

        Player player = e.getPlayer();

        if(!player.hasPermission("autofishing.use")) return;
        if(!main.getPlayerDataUtil().isAuto(player.getUniqueId())) return;

        if(e.getState() == State.BITE || e.getState() == State.CAUGHT_FISH) {
            int delay = e.getState() == State.BITE ?
                    main.getConfig().getInt("Ticks_After_Bitten") :
                    main.getConfig().getInt("Ticks_After_Caught");

            ServerTypeUtil.runAtPlayer(main, player, () -> {
                EquipmentSlot hand = getHand(player);
                if(hand == null) return;
                doRightClick(player, hand);
            }, delay);
            return;
        }
    }

    private void doRightClick(Player player, EquipmentSlot hand) {
        if(main.getConfig().getBoolean("Only_Specific_Rod.Enable")) {
            ItemStack item = hand == EquipmentSlot.HAND
                    ? player.getInventory().getItemInMainHand()
                    : player.getInventory().getItemInOffHand();
            if(!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(main.key, PersistentDataType.BYTE)) return;
        }

        nmsInteraction.useItem(player, hand);
    }

    private EquipmentSlot getHand(Player player) {
        return player.getInventory().getItemInMainHand().getType().equals(Material.FISHING_ROD) ?
                EquipmentSlot.HAND : player.getInventory().getItemInOffHand().getType().equals(Material.FISHING_ROD) ?
                EquipmentSlot.OFF_HAND : null;
    }

}
