package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.MysticCraft;
import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import br.com.novaera.mysticcraft.i18n.Lang;
import br.com.novaera.mysticcraft.integrations.ItemsAdderCompat;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class AltarInteractListener implements Listener {
    private final MysticCraft plugin;
    private final StructureManager sm;
    private final StructureService service;

    public AltarInteractListener(StructureManager sm, StructureService service, MysticCraft plugin){
        this.sm = sm;
        this.service = service;
        this.plugin = plugin;
    }

    private enum PutTakeMode { CHAT, ACTION_BAR, OFF }

    private PutTakeMode getPutTakeMode() {
        Object raw = plugin.getConfig().get("mysticcraft.messages.put_take");
        if (raw == null) return PutTakeMode.ACTION_BAR;

        if (raw instanceof Boolean b) return b ? PutTakeMode.ACTION_BAR : PutTakeMode.OFF;
        if (raw instanceof Number n)  return n.intValue() == 0 ? PutTakeMode.OFF : PutTakeMode.ACTION_BAR;

        String s = raw.toString().trim().replace(' ', '_').replace('-', '_')
                .toUpperCase(java.util.Locale.ROOT);
        return switch (s) {
            case "OFF", "FALSE", "NO", "NONE", "DISABLE", "DISABLED", "0" -> PutTakeMode.OFF;
            case "CHAT" -> PutTakeMode.CHAT;
            default -> PutTakeMode.ACTION_BAR;
        };
    }

    private void sendPutTake(Player p, String key){
        PutTakeMode mode = getPutTakeMode();
        if (mode == PutTakeMode.OFF) return;
        if (mode == PutTakeMode.ACTION_BAR) {
            p.sendActionBar(Component.text(Lang.raw(key)));
        } else {
            p.sendMessage(Lang.t(key));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent e){
        // Só reagimos a clique com a MÃO PRINCIPAL em BLOCO
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getClickedBlock() == null) return;

        var altarBlock = e.getClickedBlock();

        // ======= LINHA MAIS IMPORTANTE: só processa se for altar MARCADO =======
        if (!sm.isAltar(altarBlock)) return;

        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();

        // Verifica a quem pertence esse altar
        var coreBlock = service.findOwningCoreOfAltar(altarBlock);
        if (coreBlock == null){
            p.sendMessage(Lang.t("altar_no_owner"));
            e.setCancelled(true);
            return;
        }

        // Posição esperada do stand do altar
        Location expected = service.expectedStandLocation(coreBlock, altarBlock);
        ArmorStand as = sm.findStandNear(expected);
        if (as == null){
            // se por algum motivo o stand não estiver lá, não fazemos manutenção por clique
            // apenas avisa e sai; manutenção acontece em place/break/join/chunk
            p.sendMessage(Lang.t("altar_no_stand"));
            e.setCancelled(true);
            return;
        }

        ItemStack current = as.getEquipment().getHelmet();

        // ===== REMOVER (mão vazia ou shift) =====
        boolean wantsPickup = (hand == null || hand.getType() == Material.AIR) || p.isSneaking();
        if (wantsPickup) {
            if (current == null || current.getType() == Material.AIR) {
                p.sendMessage(Lang.t("altar_empty"));
            } else {
                ItemStack toGive = current.clone();
                var leftover = p.getInventory().addItem(toGive);
                if (!leftover.isEmpty()) {
                    as.getWorld().dropItemNaturally(as.getLocation(), toGive);
                }
                as.getEquipment().setHelmet(null);
                service.saveAltarItem(coreBlock, altarBlock, null);
                service.playFx(sm.cfg().fxTake, as.getLocation().add(0, 0.1, 0));
                sendPutTake(p, "altar_pick");
            }
            e.setCancelled(true);
            return;
        }

        // ===== COLOCAR (whitelist vanilla + IA) =====
        if (hand == null || hand.getType() == Material.AIR) {
            e.setCancelled(true);
            return;
        }

        final var cfg = sm.cfg();
        boolean allowed = true;
        String iaId = null;

        if (cfg.itemsadderEnabled && ItemsAdderCompat.isEnabled()) {
            iaId = ItemsAdderCompat.idOfItem(hand); // agora sim: ItemStack -> IA ID
        }

        if (iaId != null) {
            if (!cfg.allowedCustomIds.isEmpty()
                    && !cfg.allowedCustomIds.contains(iaId.toLowerCase(java.util.Locale.ROOT))) {
                allowed = false;
            }
        } else {
            if (!cfg.allowedItems.isEmpty() && !cfg.allowedItems.contains(hand.getType())) {
                allowed = false;
            }
        }

        if (!allowed) {
            p.sendMessage(Lang.t("altar_item_not_allowed"));
            e.setCancelled(true);
            return;
        }

        // se já tem o MESMO item, não faz nada
        if (current != null && current.getType() != Material.AIR && current.isSimilar(hand)) {
            e.setCancelled(true);
            return;
        }

        // se tem outro item, devolve
        if (current != null && current.getType() != Material.AIR) {
            var leftover = p.getInventory().addItem(current);
            if (!leftover.isEmpty()) {
                as.getWorld().dropItemNaturally(as.getLocation(), current);
            }
            as.getEquipment().setHelmet(null);
        }

        // coloca 1 unid. e salva
        ItemStack one = hand.clone(); one.setAmount(1);
        as.getEquipment().setHelmet(one);
        service.saveAltarItem(coreBlock, altarBlock, one);
        service.playFx(sm.cfg().fxPut, as.getLocation().add(0, 0.1, 0));

        hand.setAmount(hand.getAmount() - 1);
        p.getInventory().setItemInMainHand(hand.getAmount() > 0 ? hand : null);

        // NÃO chama reparo/assemble aqui
        sendPutTake(p, "altar_put");
        e.setCancelled(true);
    }
}
