package br.com.novaera.mysticcraft;

import br.com.novaera.mysticcraft.config.StructureConfig;
import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import br.com.novaera.mysticcraft.core.StructureStorage;
import br.com.novaera.mysticcraft.listener.*;
import br.com.novaera.mysticcraft.task.SpinTask;
import br.com.novaera.mysticcraft.util.Keys;
import br.com.novaera.mysticcraft.command.MysticCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import br.com.novaera.mysticcraft.i18n.Lang;
import br.com.novaera.mysticcraft.listener.ProtectionListener;
import br.com.novaera.mysticcraft.listener.ChunkLoadListener;

public class MysticCraft extends JavaPlugin {
    private StructureManager sm;
    private StructureService service;
    private SpinTask spinTask; // pode ser private

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);
        br.com.novaera.mysticcraft.util.PdcBlocks.init(this);
        saveDefaultConfig();
        Lang.init(this); // <--- NOVO

        StructureConfig cfg = StructureConfig.load(this);
        sm = new StructureManager(cfg);

        StructureStorage storage = new StructureStorage(this);
        service = new StructureService(sm, storage);

        Bukkit.getPluginManager().registerEvents(new CorePlaceListener(sm, service), this);
        Bukkit.getPluginManager().registerEvents(new AltarPlaceListener(sm, service), this);
        Bukkit.getPluginManager().registerEvents(new AltarBreakListener(service), this);
        Bukkit.getPluginManager().registerEvents(new AltarInteractListener(sm, service, this), this);
        Bukkit.getPluginManager().registerEvents(new CoreBreakListener(service), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this, sm, service), this);
        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(service), this);

        service.rebuildAllOnEnable();

        // Registrar /mystic
        MysticCommand mc = new MysticCommand(this, sm, service);
        var cmd = getCommand("mystic");
        if (cmd == null) {
            getLogger().severe("Comando 'mystic' não encontrado no plugin.yml!");
        } else {
            cmd.setExecutor(mc);
            cmd.setTabCompleter(mc);
        }


        spinTask = new SpinTask(this, sm, service);
        spinTask.start();

        getLogger().info("MysticCraft ligado. Mesas persistentes habilitadas.");
    } // <--- FECHA onEnable AQUI

    @Override
    public void onDisable() {
        if (spinTask != null) spinTask.stop();
        getLogger().info("MysticCraft desligado.");
    }

    // usado pelo /mystic reload
    public void restartSpin() {
        if (spinTask != null) spinTask.stop();
        spinTask = new SpinTask(this, sm, service);
        spinTask.start();
    }
}
