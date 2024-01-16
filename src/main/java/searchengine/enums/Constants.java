package searchengine.enums;

import lombok.Getter;


@Getter
public enum Constants {

    MOST_RELEVANT_INDEXES_COUNT_LIMIT(10000),
    TIMEOUT_150_MS(150),
    WAITING_CYCLES_NUM(100);

    private final int value;

    Constants(int value) {
        this.value = value;
    }
}
