package ryuzupluginchat.ryuzupluginchat;

import com.github.ucchyocean.lc.channel.ChannelPlayer;
import com.github.ucchyocean.lc.event.LunaChatChannelChatEvent;
import com.github.ucchyocean.lc3.LunaChat;
import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatConfig;
import com.github.ucchyocean.lc3.bukkit.event.LunaChatBukkitChannelChatEvent;
import com.github.ucchyocean.lc3.bukkit.event.LunaChatBukkitChannelMessageEvent;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
import com.github.ucchyocean.lc3.member.ChannelMember;
import com.github.ucchyocean.lc3.member.ChannelMemberBukkit;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class RyuZUPluginChat extends JavaPlugin implements PluginMessageListener, Listener {
    public static LunaChatAPI lunachatapi;
    public static HashMap<String , String> prefix = new HashMap<>();
    public static HashMap<String , String> suffix = new HashMap<>();
    public static RyuZUPluginChat ryuzupluginchat;

    @Override
    public void onEnable() {
        ryuzupluginchat = this;
        if ( getServer().getPluginManager().isPluginEnabled("LunaChat") ) {
            lunachatapi = LunaChat.getAPI();
        }
        getServer().getPluginManager().registerEvents(this ,this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "ryuzuchat:ryuzuchat");
        getServer().getMessenger().registerIncomingPluginChannel(this, "ryuzuchat:ryuzuchat", this);
        Command command = new Command();
        Objects.requireNonNull(getCommand("rpc")).setExecutor(command);
        Objects.requireNonNull(getCommand("rpc")).setTabCompleter(command);
        getLogger().info(ChatColor.GREEN + "RyuZUPluginChatが起動しました");
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, @NotNull byte[] message) {
        if (channel.equals("ryuzuchat:ryuzuchat")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String data = in.readUTF();
            Map<String , String> map = (Map<String, String>) jsonToMap(data);
            if(map.get("System").equals("Chat")) {
                boolean ExistsChannel = map.get("ChannelName") != null && lunachatapi.getChannel(map.get("ChannelName")) != null;
                Channel lunachannel =  lunachatapi.getChannel(map.get("ChannelName"));
                String msg = map.get("Format");
                msg = msg.replace("[LuckPermsPrefix]" , (map.get("LuckPermsPrefix") == null ? "" : map.get("LuckPermsPrefix")))
                        .replace("[LunaChatPrefix]" , (map.get("LunaChatPrefix") == null ? "" : map.get("LunaChatPrefix")))
                        .replace("[RyuZUMapPrefix]" , (map.get("RyuZUMapPrefix") == null ? "" : map.get("RyuZUMapPrefix")))
                        .replace("[SendServerName]" , (map.get("SendServerName") == null ? "" : map.get("SendServerName")))
                        .replace("[ReceiveServerName]" , (map.get("ReceiveServerName") == null ? "" : map.get("ReceiveServerName")))
                        .replace("[ChannelName]" , (map.get("ChannelName") == null ? "" : map.get("ChannelName")))
                        .replace("[LunaChatChannelAlias]" , (ExistsChannel ? lunachannel.getAlias() : ""))
                        .replace("[PlayerName]" , (map.get("PlayerName") == null ? "" : map.get("PlayerName")))
                        .replace("[RyuZUMapSuffix]" , (map.get("RyuZUMapSuffix") == null ? "" : map.get("RyuZUMapSuffix")))
                        .replace("[LunaChatSuffix]" , (map.get("LunaChatSuffix") == null ? "" : map.get("LunaChatSuffix")))
                        .replace("[LuckPermsSuffix]" , (map.get("LuckPermsSuffix") == null ? "" : map.get("LuckPermsSuffix")));
                msg = setColor(msg);
                msg = msg.replace("[PreReplaceMessage]" , (Boolean.parseBoolean(map.get("CanJapanese")) ? "(" + map.get("PreReplaceMessage") + ")" : ""))
                        .replace("[Message]" , (map.get("Message") == null ? "" : map.get("Message")));
                if(map.get("ReceivePlayerName") != null) {
                    Player rp = getServer().getPlayer(map.get("ReceivePlayerName"));
                    if(getServer().getPlayer(map.get("ReceivePlayerName")) == null) {return;}
                    msg = ChatColor.YELLOW + "[Private]" + msg;
                    rp.sendMessage(msg);
                    rp.sendMessage(ChatColor.RED + "--- > " + map.get("ReceivePlayerName"));
                    for(Player op : getServer().getOnlinePlayers().stream().filter(p -> p.hasPermission("rpc.op")).filter(p -> !p.equals(rp)).filter(p -> !map.get("PlayerName").equals((p.getName()))).collect(Collectors.toList())) {
                        op.sendMessage(msg);
                        op.sendMessage(ChatColor.RED + "--- > " + map.get("ReceivePlayerName"));
                    }
                    if(map.get("ReceiveServerName").equals(map.get("SendServerName"))) {
                        getLogger().info(msg);
                        getLogger().info(ChatColor.RED + "--- > " + map.get("ReceivePlayerName"));
                    } else {
                        getLogger().info("(" + ChatColor.RED +  map.get("SendServerName") + ChatColor.WHITE + ")" + map.get("PlayerName") + " --> " + map.get("Message") + ChatColor.BLUE + (map.get("ChannelName") == null ? "" : map.get("ChannelName")));
                        getLogger().info(ChatColor.RED + "--- > " + map.get("ReceivePlayerName"));
                    }
                    sendReturnPrivateMessage(map.get("PlayerName") , map);
                } else if (map.get("ReceivedPlayerName") != null) {
                    Player p = getServer().getPlayer(map.get("PlayerName"));
                    msg = ChatColor.YELLOW + "[Private]" + msg;
                    p.sendMessage(msg);
                    p.sendMessage(ChatColor.RED + "--- > " + map.get("ReceivedPlayerName"));
                } else {
                    for(Player p : (ExistsChannel ? lunachannel.getMembers().stream().map(m -> ((ChannelMemberBukkit) m).getPlayer()).collect(Collectors.toList()) : getServer().getOnlinePlayers())) {
                        p.sendMessage(msg);
                    }
                    if(map.get("ReceiveServerName").equals(map.get("SendServerName"))) {
                        getLogger().info(msg);
                    } else {
                        getLogger().info("(" + ChatColor.RED +  map.get("SendServerName") + ChatColor.WHITE + ")" + map.get("PlayerName") + " --> " + map.get("Message") + ChatColor.BLUE + (map.get("ChannelName") == null ? "" : map.get("ChannelName")));
                    }
                }
            } else if(map.get("System").equals("Prefix")) {
                prefix.put(map.get("PlayerName") , map.get("Prefix"));
            } else if(map.get("System").equals("Suffix")) {
                suffix.put(map.get("PlayerName") , map.get("Suffix"));
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        Collection<Channel> channels = lunachatapi.getChannelsByPlayer(p.getName());
        if(channels != null && channels.size() != 0){ return; }
        sendGlobalMessage(p , e.getMessage() , null);
        e.setFormat("");
        e.setCancelled(true);
    }

    @EventHandler
    public void onChat(LunaChatBukkitChannelChatEvent e) {
        ChannelMemberBukkit cp = (ChannelMemberBukkit) e.getMember();
        Player p = cp.getPlayer();
        sendGlobalMessage(p , e.getPreReplaceMessage() , e.getChannelName());
        e.setMessageFormat("");
        e.setCancelled(true);
    }

    public void sendGlobalMessage(Player p , String message , String channel) {
        Map<String , String> map = new HashMap<>();
        map.put("Message" , replaceMessage(message , p));
        map.put("ChannelName" , channel);
        map.put("PreReplaceMessage" , message);
        map.put("CanJapanese" , String.valueOf(canJapanese(message , p)));
        map.put("LuckPermsPrefix" , getPrefix(p));
        map.put("LuckPermsSuffix" , getSuffix(p));
        map.put("RyuZUMapPrefix" , prefix.get(p.getName()));
        map.put("RyuZUMapSuffix" , suffix.get(p.getName()));
        map.put("PlayerName" , p.getName());
        map.put("System" , "Chat");
        sendPluginMessage("ryuzuchat:ryuzuchat" , mapToJson(map));
    }

    public void sendPrivateMessage(Player p , String message , String receiver) {
        ChannelMemberBukkit cp =  ChannelMemberBukkit.getChannelMemberBukkit(p.getName());
        Map<String , String> map = new HashMap<>();
        map.put("Message" , replaceMessage(message , p));
        map.put("PreReplaceMessage" , message);
        map.put("CanJapanese" , String.valueOf(canJapanese(message , p)));
        map.put("ReceivePlayerName" , receiver);
        map.put("LuckPermsPrefix" , getPrefix(p));
        map.put("LuckPermsSuffix" , getSuffix(p));
        map.put("RyuZUMapPrefix" , prefix.get(p.getName()));
        map.put("RyuZUMapSuffix" , suffix.get(p.getName()));
        map.put("PlayerName" , p.getName());
        map.put("System" , "Chat");
        sendPluginMessage("ryuzuchat:ryuzuchat" , mapToJson(map));
    }

    public void sendReturnPrivateMessage(String p , Map<String , String> data) {
        Map<String , String> map = new HashMap<>(data);
        map.put("ReceivedPlayerName" , map.get("ReceivePlayerName"));
        map.remove("ReceivePlayerName");
        map.put("System" , "Chat");
        sendPluginMessage("ryuzuchat:ryuzuchat" , mapToJson(map));
    }

    private String replaceMessage(String msg , Player p) {
        String message = msg;
        LunaChatConfig config = LunaChat.getConfig();
        if(canJapanese(msg , p)) {message = lunachatapi.japanize(message , config.getJapanizeType()); }
        if(config.isEnableNormalChatColorCode() && p.hasPermission("lunachat.allowcc")) {message = setColor(message); }
        return message;
    }

    private boolean canJapanese(String msg , Player p) {
        LunaChatConfig config = LunaChat.getConfig();
        return lunachatapi.isPlayerJapanize(p.getName()) && config.getJapanizeType() != JapanizeType.NONE && msg.getBytes(StandardCharsets.UTF_8).length <= msg.length();
    }

    private String setColor(String msg) {
        String replaced = msg;
        replaced = replaceToHexFromRGB(replaced);
        return ChatColor.translateAlternateColorCodes('&' , replaced);
    }

    private void sendPluginMessage(String channel, String data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(data);
        Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (player != null) {
            player.sendPluginMessage(this, channel, out.toByteArray());
        }
        //getServer().sendPluginMessage(this, channel, out.toByteArray());
    }

    private String getPrefix(Player player) {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            LuckPerms api = provider.getProvider();
            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                return user.getCachedData().getMetaData().getPrefix();
            }
        }
        return null;
    }

    private String getSuffix(Player player) {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            LuckPerms api = provider.getProvider();
            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                return user.getCachedData().getMetaData().getSuffix();
            }
        }
        return null;
    }

    private String mapToJson(Map<String, String> map) {
        Gson gson = new Gson();
        return gson.toJson(map);
    }

    private Map<String,?> jsonToMap(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Map.class);
    }

    private static String replaceToHexFromRGB(String text) {
        String regex = "\\{.+?}";
        List<String> RGBcolors = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            RGBcolors.add(matcher.group());
        }
        for(String hexcolor : RGBcolors) {
            String[] rgbs = hexcolor.replace("{color:" , "").replace("}" , "").split(",");
            for(int i = 0 ; i < 3 ; i++) {
                try {
                    if(Integer.parseInt(rgbs[i]) < 0 || Integer.parseInt(rgbs[i]) > 255) {
                        return text;
                    }
                } catch (NumberFormatException e) {
                    return text;
                }
            }
            Color rgb = new Color(Integer.parseInt(rgbs[0]),Integer.parseInt(rgbs[1]),Integer.parseInt(rgbs[2]));
            String hex = "#"+Integer.toHexString(rgb.getRGB()).substring(2);
            text = text.replace(hexcolor , ChatColor.of(hex) + "");
        }
        return text;
    }

    public void setPrefix(String p , String data) {
        Map<String , String> map = new HashMap<>();
        prefix.put(p , setColor(data));
        map.put("PlayerName" , p);
        map.put("Prefix" , setColor(data));
        map.put("System" , "Prefix");
        sendPluginMessage("ryuzuchat:ryuzuchat" , mapToJson(map));
    }

    public void setSuffix(String p , String data) {
        Map<String , String> map = new HashMap<>();
        suffix.put(p , setColor(data));
        map.put("PlayerName" , p);
        map.put("Suffix" , setColor(data));
        map.put("System" , "Suffix");
        sendPluginMessage("ryuzuchat:ryuzuchat" , mapToJson(map));
    }

}
