package wm.vdr.autofishing;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Executors implements CommandExecutor, TabCompleter {

    private final AutoFishing main = AutoFishing.instance;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) {
            for (String line : main.getMessageList("Help")) sender.sendMessage(line);
            return true;
        }
        if(args[0].equalsIgnoreCase("reload")) {
            if(!sender.hasPermission("autofishing.admin")) {
                sender.sendMessage(main.getMessage("No_Permission"));
                return true;
            }
            main.reloadConfig();
            main.reloadMessages();
            main.getPlayerDataUtil().reloadPlayerData();
            sender.sendMessage(main.getMessage("Reload_Success"));
            return true;
        }
        if(args[0].equalsIgnoreCase("give")) {
            if(!sender.hasPermission("autofishing.admin")) {
                sender.sendMessage(main.getMessage("No_Permission"));
                return true;
            }
            Player player;
            if(args.length < 2) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(main.getMessage("Give_Usage"));
                    return true;
                }
                player = (Player) sender;
            }else {
                player = main.getServer().getPlayer(args[1]);
            }

            if(player == null) {
                sender.sendMessage(main.getMessage("Player_Not_Found"));
                return true;
            }

            player.getInventory().addItem(main.getSpecificRod()).values().forEach(itemStack -> {
                player.getWorld().dropItem(player.getLocation(), itemStack);
            });

            sender.sendMessage(main.getMessage("Rod_Given", "%player%", player.getName()));
            return true;
        }
        if(args[0].equalsIgnoreCase("toggle")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage(main.getMessage("Player_Only"));
                return true;
            }
            Player player = (Player) sender;
            if(!player.hasPermission("autofishing.use")) {
                player.sendMessage(main.getMessage("No_Permission"));
                return true;
            }
            boolean auto = main.getPlayerDataUtil().isAuto(player.getUniqueId());
            main.getPlayerDataUtil().setAuto(player.getUniqueId(), !auto);

            player.sendMessage(main.getMessage(auto ? "AutoFishing_Disabled" : "AutoFishing_Enabled"));
            return true;
        }
        sender.sendMessage(main.getMessage("Unknown_Command"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        final List<String> completions = new ArrayList<>();
        List<String> COMMANDS = new ArrayList<>();
        if(args.length == 1) {
            if(sender.hasPermission("autofishing.use")) {
                COMMANDS.add("toggle");
            }
            if(sender.hasPermission("autofishing.admin")) {
                COMMANDS.add("reload");
                COMMANDS.add("give");
            }
            StringUtil.copyPartialMatches(args[0], COMMANDS, completions);
            Collections.sort(completions);
            return completions;
        }
        if(args.length == 2) {
            if(sender.hasPermission("autofishing.admin")) {
                if(args[0].equalsIgnoreCase("give")) {
                    return null;
                }
            }
        }
        return completions;
    }
}
