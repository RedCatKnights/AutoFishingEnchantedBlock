package wm.vdr.autofishing;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class TimeoutTracker {
    private final ConcurrentMap<UUID, UUID> activeCasts = new ConcurrentHashMap<>();

    void start(UUID playerId, UUID hookId) {
        activeCasts.put(playerId, hookId);
    }

    boolean finish(UUID playerId, UUID hookId) {
        return activeCasts.remove(playerId, hookId);
    }

    boolean hasActiveCast(UUID playerId) {
        return activeCasts.containsKey(playerId);
    }

    void clear(UUID playerId) {
        activeCasts.remove(playerId);
    }
}
