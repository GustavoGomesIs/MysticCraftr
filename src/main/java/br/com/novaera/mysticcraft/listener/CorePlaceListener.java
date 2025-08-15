package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class CorePlaceListener implements Listener {
    private final StructureManager sm;
    private final StructureService service;

    public CorePlaceListener(StructureManager sm, StructureService service){
        this.sm = sm;
        this.service = service;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e){
        Block core = e.getBlockPlaced();

        // Só segue se o bloco colocado é um "core" válido (vanilla OU IA)
        if (!sm.matchesCoreBlock(core)) return;

        // Tenta montar (assemble só monta se todos os altares estiverem prontos)
        service.assemble(core, e.getPlayer());
    }
}
