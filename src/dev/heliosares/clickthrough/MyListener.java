package dev.heliosares.clickthrough;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Set;

public class MyListener implements Listener {

    private final Clickthrough plugin;
    private final boolean isChestShopLoaded;
    private final Set<Material> containers;

    public MyListener(Clickthrough plugin, Set<Material> containers) {
        this.plugin = plugin;
        this.isChestShopLoaded = plugin.getServer().getPluginManager().getPlugin("ChestShop") != null;
        this.containers = Collections.unmodifiableSet(containers);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getType() != EntityType.ITEM_FRAME && e.getRightClicked().getType() != EntityType.GLOW_ITEM_FRAME) {
            return;
        }

        if (!e.getPlayer().hasPermission("clickthrough.use")) return;
        if (e.getPlayer().isSneaking()) return;

        ItemStack itemInHand = e.getPlayer().getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.GLOW_INK_SAC) return;
        if (itemInHand.getType().toString().endsWith("_DYE")) return;

        ItemFrame itemframe = (ItemFrame) e.getRightClicked();
        itemframe.getItem();
        if (itemframe.getItem().getType() == Material.AIR) {
            return;
        }
        BlockFace face = itemframe.getAttachedFace();
        Block behind = itemframe.getLocation().getBlock().getRelative(face);
        if (!doClickThroughTo(behind.getType())) return;

        if (tryToOpenChest(e.getPlayer(), behind)) {
            e.setCancelled(true);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean doClickThroughTo(Material material) {
        if (Tag.SHULKER_BOXES.isTagged(material) && containers.contains(Material.SHULKER_BOX)) return true;
        return containers.contains(material);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (!(e.getClickedBlock().getBlockData() instanceof WallSign wallSign)) return;
        if (!e.getPlayer().hasPermission("clickthrough.use")) return;

        Sign sign = (Sign) e.getClickedBlock().getState();

        if (isChestShopLoaded) {
            if (ChestShopSign.isValid(sign)) {
                return;
            }
        }

        Block behind = e.getClickedBlock().getRelative(wallSign.getFacing().getOppositeFace());
        if (!doClickThroughTo(behind.getType())) return;

        if (tryToOpenChest(e.getPlayer(), behind)) {
            e.setCancelled(true);
            e.setUseInteractedBlock(Result.DENY);
        }
    }

    private boolean tryToOpenChest(Player player, Block block) {
        if (block.getType() == Material.CRAFTING_TABLE) {
            return player.openWorkbench(block.getLocation(), false) != null;
        }
        if (!(block.getState() instanceof Container container)) return false;
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, BlockFace.UP);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.useInteractedBlock() == Result.DENY) return false;
        player.openInventory(container.getInventory());
        return true;
    }

}
