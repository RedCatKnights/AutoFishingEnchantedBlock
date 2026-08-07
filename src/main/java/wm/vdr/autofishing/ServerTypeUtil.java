package wm.vdr.autofishing;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class ServerTypeUtil {
    private static Boolean isFolia = null;

    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    public static void runAtPlayer(JavaPlugin plugin, org.bukkit.entity.Player player, Runnable runnable, long delay) {
        if (isFolia()) {
            try {
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                runDelayed(scheduler, plugin, runnable, Math.max(1L, delay));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                plugin.getLogger().severe("Could not schedule an entity task on Folia: " + e.getMessage());
            }
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    static void runDelayed(Object scheduler, JavaPlugin plugin, Runnable runnable, long delay)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method runDelayed = scheduler.getClass().getMethod(
                "runDelayed", org.bukkit.plugin.Plugin.class, Consumer.class, Runnable.class, long.class);
        Consumer<Object> task = ignored -> runnable.run();
        runDelayed.invoke(scheduler, plugin, task, null, delay);
    }
}
