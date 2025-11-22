package dev.harscode.itsectest.adapters.web.sort;

import dev.harscode.itsectest.application.sort.SortNumbersUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/utils/sort")
public class SortController {
    private final SortNumbersUseCase sortNumbersUseCase;

    public SortController(SortNumbersUseCase sortNumbersUseCase) {
        this.sortNumbersUseCase = sortNumbersUseCase;
    }

    @PostMapping("/bubble")
    public SortResponse bubbleSort(@RequestBody SortRequest request) {
        int[] input = request.numbers()
                .stream()
                .mapToInt(Integer::intValue)
                .toArray();

        int[] sorted = sortNumbersUseCase.sort(input);

        List<Integer> result = Arrays.stream(sorted)
                .boxed()
                .toList();

        return new SortResponse(result);
    }
}
