package org.pynnccnskneuow.mtauctionhouse;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

public final class PGPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private static final String COMMAND_NAME = "auctionhouse";
    private final List<Listing> listings = new ArrayList<>();
    private final Map<UUID, Long> earnings = new HashMap<>();
    private NamespacedKey listingKey;

    @Override public void onEnable() {
        saveDefaultConfig();
        listingKey = new NamespacedKey(this, "listing");
        loadData();
        Objects.requireNonNull(getCommand(COMMAND_NAME)).setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Custom Auction House enabled (currency: " + currencyName() + ").");
    }
    @Override public void onDisable() { saveData(); }

    private String currencyName() { return getConfig().getString("currency", "DIAMONDS").equalsIgnoreCase("POINTS") ? "points" : "diamonds"; }
    private boolean points() { return currencyName().equals("points"); }
    private void loadData() {
        listings.clear(); earnings.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("listings");
        if (section != null) for (String key : section.getKeys(false)) {
            ItemStack item = section.getItemStack(key + ".item");
            String seller = section.getString(key + ".seller"); long price = section.getLong(key + ".price");
            if (item != null && price > 0 && seller != null) try { listings.add(new Listing(UUID.fromString(key), UUID.fromString(seller), item, price)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection paid = getConfig().getConfigurationSection("earnings");
        if (paid != null) for (String key : paid.getKeys(false)) try { earnings.put(UUID.fromString(key), paid.getLong(key)); } catch (IllegalArgumentException ignored) { }
    }
    private void saveData() {
        getConfig().set("listings", null); getConfig().set("earnings", null);
        for (Listing l : listings) { String path = "listings." + l.id; getConfig().set(path + ".seller", l.seller.toString()); getConfig().set(path + ".price", l.price); getConfig().set(path + ".item", l.item); }
        for (Map.Entry<UUID, Long> e : earnings.entrySet()) if (e.getValue() > 0) getConfig().set("earnings." + e.getKey(), e.getValue());
        saveConfig();
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(COMMAND_NAME)) return false;
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(Component.text("Auction House: /auctionhouse open, /auctionhouse sell <price>, /auctionhouse collect"));
            if (!(sender instanceof Player)) sender.sendMessage(Component.text("Listings currently for sale: " + listings.size()));
            return true;
        }
        if (args[0].equalsIgnoreCase("status")) { sender.sendMessage(Component.text("Auction House has " + listings.size() + " active listing(s), using " + currencyName() + ".")); return true; }
        if (!(sender instanceof Player player)) { sender.sendMessage(Component.text("This action requires an online player.")); return true; }
        if (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("browse")) { open(player); return true; }
        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length != 2) { player.sendMessage(Component.text("Usage: /auctionhouse sell <price>")); return true; }
            long price; try { price = Long.parseLong(args[1]); } catch (NumberFormatException e) { price = 0; }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (price <= 0 || held.getType() == Material.AIR) { player.sendMessage(Component.text("Hold an item and enter a positive whole-number price.")); return true; }
            listings.add(new Listing(UUID.randomUUID(), player.getUniqueId(), held.clone(), price)); player.getInventory().setItemInMainHand(null); saveData();
            player.sendMessage(Component.text("Listed " + held.getAmount() + "x " + held.getType().name() + " for " + price + " " + currencyName() + ".")); return true;
        }
        if (args[0].equalsIgnoreCase("collect")) { collect(player); return true; }
        player.sendMessage(Component.text("Unknown option. Try /auctionhouse help.")); return true;
    }

    private void open(Player player) {
        AuctionHolder holder = new AuctionHolder(); Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Custom Auction House")); holder.inventory = inv;
        for (int i = 0; i < Math.min(45, listings.size()); i++) {
            Listing l = listings.get(i); ItemStack display = l.item.clone(); ItemMeta meta = display.getItemMeta();
            List<Component> lore = new ArrayList<>(); lore.add(Component.text("Price: " + l.price + " " + currencyName())); lore.add(Component.text("Click to buy")); meta.lore(lore); meta.getPersistentDataContainer().set(listingKey, PersistentDataType.STRING, l.id.toString()); display.setItemMeta(meta); inv.setItem(i, display);
        }
        player.openInventory(inv);
    }
    private void collect(Player player) { long amount = earnings.getOrDefault(player.getUniqueId(), 0L); if (amount <= 0) { player.sendMessage(Component.text("You have no earnings to collect.")); return; } if (points()) { earnings.remove(player.getUniqueId()); player.sendMessage(Component.text("Collected " + amount + " points.")); } else { long left = amount; for (int i=0;i<36 && left>0;i++) { int n = (int)Math.min(64, left); HashMap<Integer, ItemStack> extra = player.getInventory().addItem(new ItemStack(Material.DIAMOND, n)); left -= n - extra.values().stream().mapToInt(ItemStack::getAmount).sum(); } if (left == 0) earnings.remove(player.getUniqueId()); else earnings.put(player.getUniqueId(), left); player.sendMessage(Component.text("Collected " + (amount-left) + " diamonds.")); } saveData(); }

    @EventHandler public void click(InventoryClickEvent event) { if (!(event.getView().getTopInventory().getHolder() instanceof AuctionHolder)) return; event.setCancelled(true); if (!(event.getWhoClicked() instanceof Player p) || event.getClickedInventory() != event.getView().getTopInventory()) return; ItemStack clicked = event.getCurrentItem(); if (clicked == null || !clicked.hasItemMeta()) return; String id = clicked.getItemMeta().getPersistentDataContainer().get(listingKey, PersistentDataType.STRING); if (id == null) return; Listing l = listings.stream().filter(x -> x.id.toString().equals(id)).findFirst().orElse(null); if (l == null) { p.sendMessage(Component.text("That listing is no longer available.")); open(p); return; } if (l.seller.equals(p.getUniqueId())) { p.sendMessage(Component.text("You cannot buy your own listing.")); return; } if (!charge(p, l.price)) { p.sendMessage(Component.text("You need " + l.price + " " + currencyName() + ".")); return; } listings.remove(l); earnings.merge(l.seller, l.price, Long::sum); p.getInventory().addItem(l.item.clone()); saveData(); p.sendMessage(Component.text("Purchase complete!")); open(p); }
    @EventHandler public void drag(InventoryDragEvent event) { if (event.getView().getTopInventory().getHolder() instanceof AuctionHolder) event.setCancelled(true); }
    private boolean charge(Player p, long amount) { if (points()) { long have = p.getPersistentDataContainer().getOrDefault(new NamespacedKey(this,"points"), PersistentDataType.LONG, 0L); if (have < amount) return false; p.getPersistentDataContainer().set(new NamespacedKey(this,"points"), PersistentDataType.LONG, have-amount); return true; } int have = 0; for (ItemStack i:p.getInventory().getContents()) if(i!=null&&i.getType()==Material.DIAMOND) have+=i.getAmount(); if(have<amount)return false; int left=(int)amount; for(ItemStack i:p.getInventory().getContents()) if(i!=null&&i.getType()==Material.DIAMOND&&left>0){int n=Math.min(left,i.getAmount());i.setAmount(i.getAmount()-n);left-=n;} return true; }
    private static final class Listing { final UUID id,seller; final ItemStack item; final long price; Listing(UUID i,UUID s,ItemStack x,long p){id=i;seller=s;item=x;price=p;} }
    private static final class AuctionHolder implements InventoryHolder { Inventory inventory; @Override public Inventory getInventory(){return inventory;} }
}
