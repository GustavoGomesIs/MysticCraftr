package br.com.novaera.mysticcraft.integrations;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.Nullable;
import dev.lone.itemsadder.api.CustomStack;

public final class ItemsAdderCompat {
    private static final boolean ENABLED = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");

    private ItemsAdderCompat() {}
    public static boolean isEnabled() { return ENABLED; }

    /* ===================== BLOCO/FURNITURE NO MUNDO ===================== */

    /** Tenta obter o namespacedId IA presente naquele bloco (CustomBlock ou Furniture por perto). */
    public static @Nullable String idOf(Block block) {
        if (!ENABLED || block == null) return null;

        // --- IA v4+: CustomBlock diretamente no voxel
        try {
            CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
            if (cb != null) return lower(cb.getNamespacedID());
        } catch (Throwable ignored) {}

        // --- IA v3 (compat): tentar via reflection ItemsAdder.getCustomBlock(Block)
        try {
            var m = ItemsAdder.class.getMethod("getCustomBlock", Block.class);
            Object res = m.invoke(null, block);
            if (res instanceof CustomBlock cb) {
                String id = cb.getNamespacedID();
                if (id != null) return lower(id);
            }
            // Algumas builds retornam ItemStack aqui -> ignora como "não é CustomBlock"
        } catch (Throwable ignored) {}

        // --- Procurar Furniture em volta do centro do voxel (cobre pivôs fora do bloco)
        Location c = block.getLocation().add(0.5, 0.0, 0.5);
        String id =
                furnitureIdNear(c, 1.6); // raio maior primeiro
        if (id == null) id = furnitureIdNear(c.clone().add( 1, 0,  0), 1.2);
        if (id == null) id = furnitureIdNear(c.clone().add(-1, 0,  0), 1.2);
        if (id == null) id = furnitureIdNear(c.clone().add( 0, 0,  1), 1.2);
        if (id == null) id = furnitureIdNear(c.clone().add( 0,-0.5,0), 1.2); // ligeiramente abaixo
        return id;
    }

    /** True se houver CustomBlock OU Furniture IA com o namespacedId naquela posição. */
    public static boolean matchesBlockOrFurniture(Block baseBlock, @Nullable String namespacedId) {
        if (!ENABLED || baseBlock == null || namespacedId == null || namespacedId.isBlank()) return false;
        String got = idOf(baseBlock);
        return got != null && got.equalsIgnoreCase(namespacedId);
    }

    /** Apenas verifica se existe ALGUM objeto IA (CustomBlock ou Furniture) ali. */
    public static boolean isAnyIaAt(Block block) {
        return isEnabled() && idOf(block) != null;
    }

    /* ===================== Helpers ===================== */

    @Nullable
    private static String furnitureIdNear(Location center, double r) {
        if (center == null || center.getWorld() == null) return null;

        for (Entity e : center.getWorld().getNearbyEntities(center, r, r, r)) {
            if (!looksLikeIaFurnitureEntity(e)) continue;
            try {
                CustomFurniture cf = CustomFurniture.byAlreadySpawned(e);
                if (cf != null) {
                    String id = cf.getNamespacedID();
                    if (id != null) return lower(id);
                }
            } catch (Throwable ignored) {
                // versões do IA podem lançar se não for furniture — ignoramos
            }
        }
        return null;
    }

    private static boolean looksLikeIaFurnitureEntity(Entity e) {
        // IA costuma usar ArmorStand, ItemDisplay, Interaction (e *Display em geral)
        if (e instanceof ArmorStand || e instanceof ItemDisplay || e instanceof Interaction) return true;
        String t = e.getType().name();
        return t.endsWith("DISPLAY");
    }

    private static String lower(String s) {
        return (s == null) ? null : s.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static @Nullable String idOfItem(org.bukkit.inventory.ItemStack item) {
        if (!ENABLED || item == null) return null;
        try {
            CustomStack cs = CustomStack.byItemStack(item);
            if (cs != null) return lower(cs.getNamespacedID());
        } catch (Throwable ignored) {
            // versões diferentes do IA podem lançar/variar: retornamos null se não for IA
        }
        return null;
    }
}
