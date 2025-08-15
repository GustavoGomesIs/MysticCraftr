package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.MysticCraft;
import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import br.com.novaera.mysticcraft.integrations.ItemsAdderCompat;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashSet;
import java.util.Set;

public final class AltarPlaceListener implements Listener {
    private final StructureManager sm;
    private final StructureService service;
    private final MysticCraft plugin;

    public AltarPlaceListener(StructureManager sm, StructureService service, MysticCraft plugin){
        this.sm = sm;
        this.service = service;
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlace(BlockPlaceEvent e) {
        if (e.getItemInHand() == null) return;

        Block placed = e.getBlockPlaced();

        // 1) NÃO interferir com placement de furniture/custom-block do IA (o IA tem próprios eventos)
        if (ItemsAdderCompat.isEnabled() && ItemsAdderCompat.isAnyIaAt(placed)) {
            return;
        }

        // 2) Só lidamos aqui com altares VANILLA. (IA furniture é tratado pelo IaFurnitureListener)
        if (!sm.matchesAltarBlock(placed)) return;

        // 3) Encontrar um núcleo válido próximo
        Block core = service.findNearestCore(placed.getLocation());
        if (core == null) return; // não há núcleo por perto: não bloqueia

        // 4) Verifica se a posição é um offset válido da grade
        if (!isValidAltarPos(core, placed)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cEste pilar está no lugar errado. Coloque onde o §aX §cestiver marcado:");
            var missing = service.missingAltarsForCore(core);
            sendGridHint(e.getPlayer(), core, missing);
            return;
        }

        // 5) Posição válida -> deixa colocar e tenta montar/consertar
        service.ensureIntactOrDisassemble(placed, e.getPlayer());
    }

    private boolean isValidAltarPos(Block core, Block altar){
        int dx = altar.getX() - core.getX();
        int dy = altar.getY() - core.getY();
        int dz = altar.getZ() - core.getZ();
        for (int[] off : sm.cfg().altarOffsets){
            if (off[0] == dx && off[1] == dy && off[2] == dz) return true;
        }
        return false;
    }

    /** Desenha a grade com X verdes nos offsets que estão faltando (núcleo está no centro). */
    private void sendGridHint(Player p, Block core, java.util.List<int[]> missing){
        // extents da grade com base nos offsets
        int minDx = 0, maxDx = 0, minDz = 0, maxDz = 0;
        for (int[] off : sm.cfg().altarOffsets){
            minDx = Math.min(minDx, off[0]); maxDx = Math.max(maxDx, off[0]);
            minDz = Math.min(minDz, off[2]); maxDz = Math.max(maxDz, off[2]);
        }

        // mapa rápido dos que estão faltando
        Set<String> need = new HashSet<>();
        for (int[] m : missing) need.add(m[0] + "," + m[1] + "," + m[2]);

        // linhas: Norte→Sul = minDz..maxDz ; Oeste→Leste = minDx..maxDx
        for (int z = minDz; z <= maxDz; z++){
            StringBuilder line = new StringBuilder(2 + (maxDx-minDx+1)*2);
            line.append('[');
            for (int x = minDx; x <= maxDx; x++){
                String key = x + ",0," + z;
                // X verde onde precisa pilar; cinza nos demais
                line.append(need.contains(key) ? "§aX" : "§7X");
            }
            line.append(']');
            p.sendMessage(line.toString());
        }
        p.sendMessage("§7(Dica: o núcleo fica no centro dessa grade.)");
    }
}
