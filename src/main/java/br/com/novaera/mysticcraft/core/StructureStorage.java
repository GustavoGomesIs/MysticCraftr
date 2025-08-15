package br.com.novaera.mysticcraft.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class StructureStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yml;

    private final Set<Location> cores = new HashSet<>();

    public StructureStorage(JavaPlugin plugin){
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.yml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    /* -------------------- Núcleos -------------------- */

    public void add(Location core){
        Location c = toBlockLoc(core);
        cores.add(c);
        yml.set("cores."+coreKey(c), true);
        saveQuiet();
    }

    public void remove(Location core){
        Location c = toBlockLoc(core);
        cores.remove(c);
        yml.set("cores."+coreKey(c), null);
        // apaga itens desse núcleo também
        yml.set("items."+coreKey(c), null);
        saveQuiet();
    }

    public boolean contains(Location core){
        return cores.contains(toBlockLoc(core));
    }

    public Set<Location> all(){
        return Collections.unmodifiableSet(cores);
    }

    /* -------------------- Itens dos altares -------------------- */

    public void saveAltarItem(Location core, int[] off, ItemStack item){
        Location c = toBlockLoc(core);
        String path = "items."+coreKey(c)+"."+offsetKey(off);
        yml.set(path, item); // se item == null, apaga
        saveQuiet();
    }

    public ItemStack getAltarItem(Location core, int[] off){
        Location c = toBlockLoc(core);
        String path = "items."+coreKey(c)+"."+offsetKey(off);
        return yml.getItemStack(path);
    }

    public void clearAltarItems(Location core){
        Location c = toBlockLoc(core);
        yml.set("items."+coreKey(c), null);
        saveQuiet();
    }

    /* -------------------- Helpers -------------------- */

    private void load(){
        cores.clear();

        // modo novo: cores.<world:x:y:z>: true
        if (yml.isConfigurationSection("cores")){
            for (String key : Objects.requireNonNull(yml.getConfigurationSection("cores")).getKeys(false)){
                Location loc = parseCoreKey(key);
                if (loc != null) cores.add(loc);
            }
        }

        // compat: se alguém tiver usado lista no passado
        if (yml.isList("cores_list")){
            for (Object o : yml.getList("cores_list")){
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String,Object> m = (Map<String,Object>) o;
                String world = String.valueOf(m.getOrDefault("world","world"));
                int x = asInt(m.get("x")), y = asInt(m.get("y")), z = asInt(m.get("z"));
                World w = Bukkit.getWorld(world);
                if (w != null){
                    cores.add(new Location(w, x, y, z));
                    yml.set("cores."+coreKey(new Location(w,x,y,z)), true);
                }
            }
            yml.set("cores_list", null);
            saveQuiet();
        }
    }

    private static String coreKey(Location l){
        return l.getWorld().getName()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ();
    }

    private static String offsetKey(int[] off){
        return off[0]+","+off[1]+","+off[2];
    }

    private static Location parseCoreKey(String key){
        try{
            String[] p = key.split(":");
            if (p.length != 4) return null;
            World w = Bukkit.getWorld(p[0]);
            if (w == null) return null;
            int x = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            int z = Integer.parseInt(p[3]);
            return new Location(w, x, y, z);
        }catch (Exception e){ return null; }
    }

    private static Location toBlockLoc(Location l){
        return new Location(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    private static int asInt(Object o){
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) try { return Integer.parseInt(s); } catch (Exception ignored){}
        return 0;
    }

    private void saveQuiet(){
        try { yml.save(file); } catch (IOException ignored) {}
    }
}
