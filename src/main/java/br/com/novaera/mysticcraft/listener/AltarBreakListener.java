package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class AltarBreakListener implements Listener {
    private final StructureService service;
    public AltarBreakListener(StructureService service){ this.service = service; }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        Block b = e.getBlock();
        if (b.getType() != service.cfg().altarMat) return;

        // descobrir o núcleo dono deste altar e desmontar tudo
        Block core = service.findOwningCoreOfAltar(b);
        if (core != null){
            service.disassemble(core, e.getPlayer());
        }
    }
}
