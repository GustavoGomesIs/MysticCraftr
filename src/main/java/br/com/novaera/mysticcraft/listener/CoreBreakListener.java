package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class CoreBreakListener implements Listener {
    private final StructureService service;
    public CoreBreakListener(StructureService service){ this.service = service; }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        Block core = e.getBlock();
        if (core.getType() != service.cfg().coreMat) return;
        service.disassemble(core, e.getPlayer()); // desmonta e avisa
    }
}
