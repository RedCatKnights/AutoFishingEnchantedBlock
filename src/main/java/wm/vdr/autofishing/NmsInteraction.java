package wm.vdr.autofishing;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;

final class NmsInteraction {
    private final AutoFishing plugin;
    private volatile Access access;
    private volatile boolean failureLogged;

    NmsInteraction(AutoFishing plugin) {
        this.plugin = plugin;
    }

    boolean useItem(Player player, EquipmentSlot bukkitHand) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object world = player.getWorld().getClass().getMethod("getHandle").invoke(player.getWorld());

            Access current = access;
            if (current == null || !current.supports(handle, world)) {
                current = resolve(handle.getClass(), world.getClass());
                access = current;
            }

            int handIndex = bukkitHand == EquipmentSlot.HAND ? 0 : 1;
            current.invoke(handle, world, handIndex);

            if (bukkitHand == EquipmentSlot.HAND) {
                player.swingMainHand();
            } else {
                player.swingOffHand();
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            logFailure(e);
            return false;
        }
    }

    private void logFailure(Exception exception) {
        if (failureLogged) return;
        failureLogged = true;
        plugin.getLogger().log(Level.SEVERE,
                "Auto-fishing could not invoke the server use-item path on Minecraft "
                        + plugin.getServer().getBukkitVersion() + ". This error will only be logged once.",
                exception);
    }

    static Access resolve(Class<?> handleType, Class<?> worldType) throws NoSuchMethodException {
        for (Class<?> owner = handleType; owner != null; owner = owner.getSuperclass()) {
            for (Field field : owner.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                for (Method useItem : field.getType().getMethods()) {
                    Class<?>[] parameters = useItem.getParameterTypes();
                    if (parameters.length != 4
                            || !parameters[0].isAssignableFrom(handleType)
                            || !parameters[1].isAssignableFrom(worldType)
                            || !parameters[3].isEnum()) {
                        continue;
                    }

                    Object[] hands = parameters[3].getEnumConstants();
                    if (hands == null || hands.length < 2) continue;

                    Method getItem = findGetItemInHand(handleType, parameters[2], parameters[3]);
                    if (getItem == null) continue;

                    field.setAccessible(true);
                    useItem.setAccessible(true);
                    getItem.setAccessible(true);
                    return new Access(handleType, worldType, field, useItem, getItem, hands);
                }
            }
        }
        throw new NoSuchMethodException(
                "No compatible NMS use-item method found for " + handleType.getName());
    }

    private static Method findGetItemInHand(Class<?> handleType, Class<?> itemType, Class<?> handType) {
        for (Method method : handleType.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1
                    && parameters[0] == handType
                    && itemType.isAssignableFrom(method.getReturnType())) {
                return method;
            }
        }
        return null;
    }

    static final class Access {
        private final Class<?> handleType;
        private final Class<?> worldType;
        private final Field controllerField;
        private final Method useItem;
        private final Method getItemInHand;
        private final Object[] hands;

        private Access(Class<?> handleType, Class<?> worldType, Field controllerField,
                       Method useItem, Method getItemInHand, Object[] hands) {
            this.handleType = handleType;
            this.worldType = worldType;
            this.controllerField = controllerField;
            this.useItem = useItem;
            this.getItemInHand = getItemInHand;
            this.hands = hands;
        }

        boolean supports(Object handle, Object world) {
            return handleType.isInstance(handle) && worldType.isInstance(world);
        }

        void invoke(Object handle, Object world, int handIndex)
                throws IllegalAccessException, InvocationTargetException {
            Object hand = hands[handIndex];
            Object item = getItemInHand.invoke(handle, hand);
            Object controller = controllerField.get(handle);
            useItem.invoke(controller, handle, world, item, hand);
        }
    }
}
