package dev.harscode.itsectest.domain.sort;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BubbleSortStrategyTest {

    @Test
    void sort_shouldSortAscending() {
        BubbleSortStrategy strategy = new BubbleSortStrategy();
        int[] input = {5, 3, 8, 1, 2};
        int[] result = strategy.sort(input);

        assertArrayEquals(new int[]{1, 2, 3, 5, 8}, result);
    }

    @Test
    void sort_shouldHandleEmptyArray() {
        BubbleSortStrategy strategy = new BubbleSortStrategy();
        int[] input = {};
        int[] result = strategy.sort(input);

        assertArrayEquals(new int[]{}, result);
    }

    @Test
    void sort_shouldHandleAlreadySortedArray() {
        BubbleSortStrategy strategy = new BubbleSortStrategy();
        int[] input = {1, 2, 3, 4};
        int[] result = strategy.sort(input);

        assertArrayEquals(new int[]{1, 2, 3, 4}, result);
    }

    @Test
    void sort_shouldNotModifyOriginalArray() {
        BubbleSortStrategy strategy = new BubbleSortStrategy();
        int[] input = {3, 2, 1};
        int[] copy = input.clone();

        strategy.sort(input);

        assertArrayEquals(copy, input);
    }
}