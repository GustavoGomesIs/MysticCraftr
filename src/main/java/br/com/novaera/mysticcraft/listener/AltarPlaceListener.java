package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class AltarPlaceListener implements Listener {
    private final StructureManager sm;
    private final StructureService service;

    public AltarPlaceListener(StructureManager sm, StructureService service){
        this.sm = sm;
        this.service = service;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e){
        Block placed = e.getBlockPlaced();

        // Só reage se o bloco colocado é um "altar" válido (vanilla OU IA)
        if (!sm.matchesAltarBlock(placed)) return;

        // Procura um núcleo nas posições possíveis em volta desse altar recém-colocado
        for (int[] off : service.cfg().altarOffsets){
            Block core = placed.getLocation().clone().add(-off[0], -off[1], -off[2]).getBlock();
            if (sm.matchesCoreBlock(core)) {
                // Tenta montar (assemble verifica se a estrutura está completa)
                service.assemble(core, e.getPlayer());
                break; // evita mensagens duplicadas se houver múltiplos matches
            }
        }
    }
}
