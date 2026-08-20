package io.github.georgetimbershift.timbershift.material;

import io.github.georgetimbershift.timbershift.model.LogFamily;
import io.github.georgetimbershift.timbershift.tree.CellKind;
import io.github.georgetimbershift.timbershift.tree.CellSnapshot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public final class MaterialClassifier {
    private static final Set<Material> NATURAL_SOLIDS = EnumSet.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.PODZOL,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.MUD,
            Material.CLAY,
            Material.SAND,
            Material.RED_SAND,
            Material.GRAVEL,
            Material.STONE,
            Material.DEEPSLATE,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.TUFF,
            Material.CALCITE,
            Material.MOSS_BLOCK,
            Material.SNOW_BLOCK,
            Material.POWDER_SNOW,
            Material.ICE,
            Material.PACKED_ICE,
            Material.BLUE_ICE,
            Material.MANGROVE_ROOTS,
            Material.MUDDY_MANGROVE_ROOTS,
            Material.BEE_NEST
    );

    public boolean isAxe(ItemStack item) {
        return item != null && Tag.ITEMS_AXES.isTagged(item.getType());
    }

    public LogFamily logFamily(Material material) {
        if (!Tag.OVERWORLD_NATURAL_LOGS.isTagged(material)) {
            return null;
        }
        if (Tag.OAK_LOGS.isTagged(material)) {
            return LogFamily.OAK;
        }
        if (Tag.SPRUCE_LOGS.isTagged(material)) {
            return LogFamily.SPRUCE;
        }
        if (Tag.BIRCH_LOGS.isTagged(material)) {
            return LogFamily.BIRCH;
        }
        if (Tag.JUNGLE_LOGS.isTagged(material)) {
            return LogFamily.JUNGLE;
        }
        if (Tag.ACACIA_LOGS.isTagged(material)) {
            return LogFamily.ACACIA;
        }
        if (Tag.DARK_OAK_LOGS.isTagged(material)) {
            return LogFamily.DARK_OAK;
        }
        if (Tag.MANGROVE_LOGS.isTagged(material)) {
            return LogFamily.MANGROVE;
        }
        if (Tag.CHERRY_LOGS.isTagged(material)) {
            return LogFamily.CHERRY;
        }
        if (Tag.PALE_OAK_LOGS.isTagged(material)) {
            return LogFamily.PALE_OAK;
        }
        return null;
    }

    public LogFamily leafFamily(Material material) {
        return switch (material) {
            case OAK_LEAVES, AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES -> LogFamily.OAK;
            case SPRUCE_LEAVES -> LogFamily.SPRUCE;
            case BIRCH_LEAVES -> LogFamily.BIRCH;
            case JUNGLE_LEAVES -> LogFamily.JUNGLE;
            case ACACIA_LEAVES -> LogFamily.ACACIA;
            case DARK_OAK_LEAVES -> LogFamily.DARK_OAK;
            case MANGROVE_LEAVES -> LogFamily.MANGROVE;
            case CHERRY_LEAVES -> LogFamily.CHERRY;
            case PALE_OAK_LEAVES -> LogFamily.PALE_OAK;
            default -> null;
        };
    }

    public CellSnapshot classify(Block block) {
        Material material = block.getType();
        if (material.isAir()) {
            return CellSnapshot.air();
        }

        LogFamily logFamily = logFamily(material);
        if (logFamily != null) {
            return CellSnapshot.log(logFamily, block.getBlockData().getAsString());
        }

        if (Tag.LEAVES.isTagged(material)) {
            LogFamily leafFamily = leafFamily(material);
            BlockData blockData = block.getBlockData();
            if (leafFamily != null && blockData instanceof Leaves leaves) {
                return leaves.isPersistent()
                        ? CellSnapshot.persistentLeaf(leafFamily)
                        : CellSnapshot.naturalLeaf(leafFamily);
            }
            return CellSnapshot.naturalBlock();
        }

        if (!material.isSolid() || NATURAL_SOLIDS.contains(material)) {
            return CellSnapshot.naturalBlock();
        }
        return new CellSnapshot(CellKind.OBSTRUCTION, null, block.getBlockData().getAsString());
    }
}
