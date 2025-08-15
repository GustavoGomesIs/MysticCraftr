package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.MysticCraft;
import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.*;

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
    public void onPlace(BlockPlaceEvent e){
        Block b = e.getBlockPlaced();

        // 1) ignorar TUDO que não seja bloco de altar (vanilla OU IA)
        if (!sm.matchesAltarBlock(b)) return;

        // 2) precisa ter um core por perto; se não tiver => ignora (deixa colocar livre)
        Block core = service.findNearestCore(b.getLocation());
        if (core == null) return;

        // 3) mesa completa? cancela e avisa
        var missing = service.missingAltarsForCore(core);
        if (missing.isEmpty()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§eA mesa já está completa.");
            return;
        }

        // 4) é uma posição válida da grade?
        if (!isValidAltarPos(core, b)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cEste bloco de altar está no lugar errado. Coloque onde o §aX §cestiver marcado:");
            sendGridHint(e.getPlayer(), core, missing);
            return;
        }

        // 5) posição válida -> deixa colocar e tenta montar/consertar
        service.ensureIntactOrDisassemble(b, e.getPlayer());
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
