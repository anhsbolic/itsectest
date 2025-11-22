package dev.harscode.itsectest.domain.sort;

public class BubbleSortStrategy implements SortStrategy {

    @Override
    public int[] sort(int[] input) {
        // validate input array
        if (input == null || input.length <= 1) {
            return input;
        }

        // clone input array to avoid modifying the original one
        int[] arr = input.clone();

        // start sorting from the end of the array
        boolean swapped;
        for (int i = 0; i < arr.length - 1; i++) {
            swapped = false;
            for (int j = 0; j < arr.length - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int tmp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = tmp;
                    swapped = true;
                }
            }

            // stop sorting if no elements were swapped
            if (!swapped) {
                break;
            }
        }

        // return sorted array
        return arr;
    }
}