package dev.harscode.itsectest.application.sort;

import org.springframework.stereotype.Service;
import dev.harscode.itsectest.domain.sort.BubbleSortStrategy;
import dev.harscode.itsectest.domain.sort.SortStrategy;

@Service
public class SortNumbersUseCase {
    private final SortStrategy sortStrategy;

    public SortNumbersUseCase() {
        this.sortStrategy = new BubbleSortStrategy();
    }

    public int[] sort(int[] numbers) {
        return sortStrategy.sort(numbers);
    }
}
