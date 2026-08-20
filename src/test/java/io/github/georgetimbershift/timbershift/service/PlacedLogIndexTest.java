package io.github.georgetimbershift.timbershift.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedLogIndexTest {
    @Test
    void packedPositionUsesChunkLocalCoordinatesAndWorldRelativeHeight() {
        int first = PlacedLogIndex.pack(1, -60, 2, -64);
        int sameLocalPositionInAnotherChunk = PlacedLogIndex.pack(17, -60, 18, -64);
        int differentHeight = PlacedLogIndex.pack(1, -59, 2, -64);

        assertTrue(first == sameLocalPositionInAnotherChunk);
        assertFalse(first == differentHeight);
    }

    @Test
    void indexRemainsSortedAndDuplicateFree() {
        int[] values = new int[0];
        values = PlacedLogIndex.add(values, 30);
        values = PlacedLogIndex.add(values, 10);
        values = PlacedLogIndex.add(values, 20);
        values = PlacedLogIndex.add(values, 20);

        assertArrayEquals(new int[]{10, 20, 30}, values);
        assertTrue(PlacedLogIndex.contains(values, 20));
        assertArrayEquals(new int[]{10, 30}, PlacedLogIndex.remove(values, 20));
        assertArrayEquals(values, PlacedLogIndex.remove(values, 99));
    }
}
