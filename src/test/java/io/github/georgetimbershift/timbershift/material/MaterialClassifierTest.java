package io.github.georgetimbershift.timbershift.material;

import io.github.georgetimbershift.timbershift.model.LogFamily;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialClassifierTest {
    private final MaterialClassifier classifier = new MaterialClassifier();

    @Test
    void recognizesNaturalLogsAvailableAtTheCompatibilityFloor() {
        assertEquals(LogFamily.OAK, classifier.logFamily(Material.OAK_LOG));
        assertEquals(LogFamily.CHERRY, classifier.logFamily(Material.CHERRY_LOG));
        assertEquals(LogFamily.MANGROVE, classifier.logFamily(Material.MANGROVE_LOG));
    }

    @Test
    void recognizesNewerPaleOakByRuntimeName() {
        assertEquals(LogFamily.PALE_OAK, MaterialClassifier.logFamilyName("PALE_OAK_LOG"));
        assertEquals(LogFamily.PALE_OAK, MaterialClassifier.leafFamilyName("PALE_OAK_LEAVES"));
    }

    @Test
    void recognizesPoplarAndEveryLeafColorByRuntimeName() {
        assertEquals(LogFamily.POPLAR, MaterialClassifier.logFamilyName("POPLAR_LOG"));
        for (String color : new String[]{"RED", "ORANGE", "YELLOW"}) {
            assertEquals(LogFamily.POPLAR, MaterialClassifier.leafFamilyName(color + "_POPLAR_LEAVES"));
        }
    }

    @Test
    void rejectsProcessedWoodVariants() {
        assertNull(classifier.logFamily(Material.OAK_WOOD));
        assertNull(classifier.logFamily(Material.STRIPPED_OAK_LOG));
        assertNull(MaterialClassifier.logFamilyName("POPLAR_WOOD"));
        assertNull(MaterialClassifier.logFamilyName("STRIPPED_POPLAR_LOG"));
        assertNull(MaterialClassifier.logFamilyName("STRIPPED_POPLAR_WOOD"));
    }

    @Test
    void recognizesLeavesAndAxesWithoutNewApiTags() {
        assertEquals(LogFamily.OAK, classifier.leafFamily(Material.FLOWERING_AZALEA_LEAVES));
        assertTrue(classifier.isAxe(new ItemStack(Material.DIAMOND_AXE)));
        assertFalse(classifier.isAxe(new ItemStack(Material.DIAMOND_PICKAXE)));
    }
}
