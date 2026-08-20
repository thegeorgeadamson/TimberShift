package io.github.georgetimbershift.timbershift.service;

import java.util.Arrays;

final class PlacedLogIndex {
    private PlacedLogIndex() {
    }

    static int pack(int blockX, int blockY, int blockZ, int minimumWorldHeight) {
        int localY = blockY - minimumWorldHeight;
        if (localY < 0 || localY > 0x7FFFFF) {
            throw new IllegalArgumentException("Block Y is outside the encodable world height");
        }
        return (localY << 8) | ((blockX & 15) << 4) | (blockZ & 15);
    }

    static boolean contains(int[] values, int value) {
        return Arrays.binarySearch(values, value) >= 0;
    }

    static int[] add(int[] values, int value) {
        int index = Arrays.binarySearch(values, value);
        if (index >= 0) {
            return values;
        }
        int insertion = -index - 1;
        int[] result = new int[values.length + 1];
        System.arraycopy(values, 0, result, 0, insertion);
        result[insertion] = value;
        System.arraycopy(values, insertion, result, insertion + 1, values.length - insertion);
        return result;
    }

    static int[] remove(int[] values, int value) {
        int index = Arrays.binarySearch(values, value);
        if (index < 0) {
            return values;
        }
        int[] result = new int[values.length - 1];
        System.arraycopy(values, 0, result, 0, index);
        System.arraycopy(values, index + 1, result, index, values.length - index - 1);
        return result;
    }
}
