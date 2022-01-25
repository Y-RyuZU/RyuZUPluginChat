package ryuzupluginchat.ryuzupluginchat.redis;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import ryuzupluginchat.ryuzupluginchat.RyuZUPluginChat;

@RequiredArgsConstructor
public class PlayerUUIDMapContainer {

  private final RyuZUPluginChat plugin;

  private final Jedis jedis;

  private final String groupName;

  private Map<String, String> playerCache;
  private long lastCacheUpdated;

  public void register(String name, UUID uuid) {
    jedis.hset("rpc:" + groupName + ":uuid-map", name.toLowerCase(Locale.ROOT), uuid.toString());
  }

  public void register(Player p) {
    register(p.getName(), p.getUniqueId());
  }

  public void unregister(String name) {
    jedis.hdel("rpc:" + groupName + ":uuid-map", name.toLowerCase());
  }

  public void unregister(Player p) {
    unregister(p.getName());
  }

  public UUID getUUID(String name) {
    String value = jedis.hget("rpc:" + groupName + ":uuid-map", name.toLowerCase());
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      plugin.getLogger()
          .warning("Invalid string uuid has been responded from Redis server. ( " + value + " )");
      return null;
    }
  }

  public Set<String> getAllNames() {
    updateCacheIfOutdated();
    return new HashSet<>(playerCache.keySet());
  }

  public boolean isOnline(UUID uuid) {
    updateCacheIfOutdated();
    return playerCache.containsValue(uuid.toString());
  }

  public String getNameFromUUID(UUID uuid) {
    updateCacheIfOutdated();
    for (String name : playerCache.keySet()) {
      String uuidStr = playerCache.get(name);
      if (uuidStr.equals(uuid.toString())) {
        return name;
      }
    }

    return null;
  }

  private void updateCacheIfOutdated() {
    if (lastCacheUpdated + 5000L < System.currentTimeMillis()) {
      playerCache = jedis.hgetAll("rpc:" + groupName + ":uuid-map");
      lastCacheUpdated = System.currentTimeMillis();
    }
  }
}
