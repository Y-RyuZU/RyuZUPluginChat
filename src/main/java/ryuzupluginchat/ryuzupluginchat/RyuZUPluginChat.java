package ryuzupluginchat.ryuzupluginchat;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import java.util.Objects;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import ryuzupluginchat.ryuzupluginchat.command.RPCCommand;
import ryuzupluginchat.ryuzupluginchat.command.ReplyCommand;
import ryuzupluginchat.ryuzupluginchat.command.TellCommand;
import ryuzupluginchat.ryuzupluginchat.config.RPCConfig;
import ryuzupluginchat.ryuzupluginchat.listener.ChatListener;
import ryuzupluginchat.ryuzupluginchat.listener.JoinQuitListener;
import ryuzupluginchat.ryuzupluginchat.message.JsonDataConverter;
import ryuzupluginchat.ryuzupluginchat.message.MessageDataFactory;
import ryuzupluginchat.ryuzupluginchat.message.MessageProcessor;
import ryuzupluginchat.ryuzupluginchat.redis.MessagePublisher;
import ryuzupluginchat.ryuzupluginchat.redis.MessageSubscriber;
import ryuzupluginchat.ryuzupluginchat.redis.PlayerUUIDMapContainer;
import ryuzupluginchat.ryuzupluginchat.redis.PrivateChatIDGetter;
import ryuzupluginchat.ryuzupluginchat.redis.PrivateChatReachedSubscriber;
import ryuzupluginchat.ryuzupluginchat.redis.PrivateChatResponseWaiter;
import ryuzupluginchat.ryuzupluginchat.redis.ReplyTargetFetcher;
import ryuzupluginchat.ryuzupluginchat.redis.RyuZUPrefixSuffixContainer;

@Getter
public final class RyuZUPluginChat extends JavaPlugin {

  private static TaskChainFactory taskChainFactory;

  private RPCConfig rpcConfig;
  private MessageDataFactory messageDataFactory;
  private MessageProcessor messageProcessor;
  private RyuZUPrefixSuffixContainer prefixSuffixContainer;
  private PlayerUUIDMapContainer playerUUIDMapContainer;
  private ReplyTargetFetcher replyTargetFetcher;
  private JsonDataConverter jsonDataConverter;
  private PrivateChatIDGetter privateChatIDGetter;
  private final PrivateChatResponseWaiter privateChatResponseWaiter = new PrivateChatResponseWaiter();

  private MessagePublisher publisher;
  private MessageSubscriber subscriber;
  private PrivateChatReachedSubscriber privateChatReachedSubscriber;


  @Override
  public void onEnable() {
    taskChainFactory = BukkitTaskChainFactory.create(this);

    rpcConfig = new RPCConfig(this);
    rpcConfig.load();

    messageDataFactory = new MessageDataFactory(this);
    messageProcessor = new MessageProcessor(this);
    jsonDataConverter = new JsonDataConverter(this);

    setupRedisConnections();

    getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);

    registerCommands();

    getLogger().info(getName() + " enabled.");
  }

  @Override
  public void onDisable() {
    Bukkit.getLogger().info(getName() + " disabled.");
  }

  private void setupRedisConnections() {
    Jedis jedis = getConnectedJedis();

    publisher = new MessagePublisher(jedis, jsonDataConverter, rpcConfig.getGroupName());

    // create new jedis instance because subscribe will block other actions like hset etc.
    subscriber = new MessageSubscriber(this, jsonDataConverter, getConnectedJedis(),
        rpcConfig.getGroupName());
    subscriber.subscribe();

    // Same as above
    privateChatReachedSubscriber = new PrivateChatReachedSubscriber(this, getConnectedJedis(),
        rpcConfig.getGroupName());
    privateChatReachedSubscriber.subscribe();

    subscriber.registerFunctions();

    prefixSuffixContainer = new RyuZUPrefixSuffixContainer(jedis,
        rpcConfig.getGroupName());
    playerUUIDMapContainer = new PlayerUUIDMapContainer(this, jedis, rpcConfig.getGroupName());
    replyTargetFetcher = new ReplyTargetFetcher(jedis, rpcConfig.getGroupName());
    privateChatIDGetter = new PrivateChatIDGetter(jedis, rpcConfig.getGroupName());
  }

  private Jedis getConnectedJedis() {
    Jedis jedis = new Jedis(rpcConfig.getHostAndPort());
    jedis.auth(
//        rpcConfig.getRedisUserName(),
        rpcConfig.getRedisPassword());
    return jedis;
  }

  private void registerCommands() {
    RPCCommand rpcCommand = new RPCCommand(this);
    TellCommand tellCommand = new TellCommand(this);
    ReplyCommand replyCommand = new ReplyCommand(this);

    Objects.requireNonNull(getCommand("rpc")).setExecutor(rpcCommand);
    Objects.requireNonNull(getCommand("rpc")).setTabCompleter(rpcCommand);
    Objects.requireNonNull(getCommand("tell")).setExecutor(tellCommand);
    Objects.requireNonNull(getCommand("tell")).setTabCompleter(tellCommand);
    Objects.requireNonNull(getCommand("reply")).setExecutor(replyCommand);
  }

  public static <T> TaskChain<T> newChain() {
    return taskChainFactory.newChain();
  }

  public static <T> TaskChain<T> newSharedChain(String name) {
    return taskChainFactory.newSharedChain(name);
  }
}
