package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkLoadListener implements Listener {
    private final StructureService service;

    public ChunkLoadListener(StructureService service){
        this.service = service;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e){
        Chunk c = e.getChunk();
        for (Location coreLoc : service.coresInChunk(c)){
            service.repairCoreAt(coreLoc);
        }
    }
}
