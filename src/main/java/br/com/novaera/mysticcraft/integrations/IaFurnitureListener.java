package br.com.novaera.mysticcraft.integrations;

import br.com.novaera.mysticcraft.MysticCraft;
import br.com.novaera.mysticcraft.core.StructureService;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurniturePlaceEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IaFurnitureListener implements Listener {
    private final StructureService service;
    private final MysticCraft plugin;

    public IaFurnitureListener(StructureService service, MysticCraft plugin){
        this.service = service;
        this.plugin = plugin;
    }

    private boolean isOurAltarId(String id){
        var cfg = service.cfg();
        return cfg.itemsadderEnabled
                && cfg.altarCustomBlockId != null
                && !cfg.altarCustomBlockId.isBlank()
                && cfg.altarCustomBlockId.equalsIgnoreCase(id);
    }

    /** IA compat: tenta pegar a entidade da furniture em várias versões. */
    private Entity furnitureEntityCompat(Object event){
        try {
            Method m = event.getClass().getMethod("getBukkitEntity");
            Object o = m.invoke(event);
            if (o instanceof Entity ent) return ent;
        } catch (Throwable ignored) {}

        try {
            Method m = event.getClass().getMethod("getEntity");
            Object o = m.invoke(event);
            if (o instanceof Entity ent) return ent;
        } catch (Throwable ignored) {}

        try {
            Method m = event.getClass().getMethod("getFurniture");
            Object furn = m.invoke(event);
            if (furn != null) {
                try {
                    Method m2 = furn.getClass().getMethod("getBukkitEntity");
                    Object o2 = m2.invoke(furn);
                    if (o2 instanceof Entity ent) return ent;
                } catch (Throwable ignored2) {}
                try {
                    Method m2 = furn.getClass().getMethod("getEntity");
                    Object o2 = m2.invoke(furn);
                    if (o2 instanceof Entity ent) return ent;
                } catch (Throwable ignored3) {}
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private Block baseBlockOfFurniture(Entity ent){
        return (ent == null) ? null : ent.getLocation().getBlock();
    }

    /** Se o base bruto não bater, estima o offset pelo arredondamento da posição da entidade. */
    private Block approxAltarBlockFromEntity(Block core, Entity ent){
        if (core == null || ent == null) return null;
        Location l = ent.getLocation();
        double dx = l.getX() - (core.getX() + 0.5);
        double dz = l.getZ() - (core.getZ() + 0.5);
        int rdx = (int) Math.round(dx);
        int rdz = (int) Math.round(dz);
        int rdy = l.getBlockY() - core.getY();
        return core.getLocation().add(rdx, rdy, rdz).getBlock();
    }

    private boolean isValidAltarPos(Block core, Block altarBase){
        int dx = altarBase.getX() - core.getX();
        int dy = altarBase.getY() - core.getY();
        int dz = altarBase.getZ() - core.getZ();
        for (int[] off : service.cfg().altarOffsets){
            if (off[0] == dx && off[1] == dy && off[2] == dz) return true;
        }
        return false;
    }

    private void sendGridHint(Player p, Block core, List<int[]> missing){
        int minDx = 0, maxDx = 0, minDz = 0, maxDz = 0;
        for (int[] off : service.cfg().altarOffsets){
            minDx = Math.min(minDx, off[0]); maxDx = Math.max(maxDx, off[0]);
            minDz = Math.min(minDz, off[2]); maxDz = Math.max(maxDz, off[2]);
        }
        Set<String> need = new HashSet<>();
        for (int[] m : missing) need.add(m[0] + "," + m[1] + "," + m[2]);

        for (int z = minDz; z <= maxDz; z++){
            StringBuilder line = new StringBuilder();
            line.append('[');
            for (int x = minDx; x <= maxDx; x++){
                String key = x + ",0," + z;
                line.append(need.contains(key) ? "§aX" : "§7X");
            }
            line.append(']');
            p.sendMessage(line.toString());
        }
        p.sendMessage("§7(Dica: o núcleo fica no centro dessa grade.)");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onFurniturePlace(FurniturePlaceEvent e){
        final String id = e.getNamespacedID();
        if (!isOurAltarId(id)) return;

        Entity ent = furnitureEntityCompat(e);
        Block base = baseBlockOfFurniture(ent);
        if (base == null) return;

        Block core = service.findNearestCore(base.getLocation());
        if (core == null) return;

        // posição válida?
        Block altarBase = base;
        boolean valid = isValidAltarPos(core, altarBase);
        if (!valid) {
            Block approx = approxAltarBlockFromEntity(core, ent);
            if (approx != null && isValidAltarPos(core, approx)) {
                altarBase = approx;
                valid = true;
            }
        }

        if (!valid) {
            // Lugar errado → cancela e mostra grade do que falta
            var missing = service.missingAltarsForCore(core);
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cEste bloco de altar está no lugar errado. Coloque onde o §aX §cestiver marcado:");
            sendGridHint(e.getPlayer(), core, missing);
            return;
        }

        // ✔ posição válida → nunca cancela aqui.
        // Deixa o IA colocar a furniture e pede pro serviço montar/reparar.
        service.ensureIntactOrDisassemble(altarBase, e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onFurnitureBreak(FurnitureBreakEvent e){
        final String id = e.getNamespacedID();
        if (!isOurAltarId(id)) return;

        Entity ent = furnitureEntityCompat(e);
        Block base = baseBlockOfFurniture(ent);
        if (base == null) return;

        service.ensureIntactOrDisassemble(base, e.getPlayer());
    }
}
