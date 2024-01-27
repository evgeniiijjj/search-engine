package searchengine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import searchengine.models.SearchResult;
import java.util.List;

public enum Messages {
    INTERRUPTED_INDEXING("Индексация прервана"),
    SUCCESS(""),
    SUCCESS_SEARCH(""),
    FAILED_PAGE_INDEX("Данная страница находится за пределами сайтов, " +
            "указанных в конфигурационном файле"),
    FAILED_SEARCH("Задан пустой поисковый запрос"),
    FAILED_START("Индексация уже запущена"),
    FAILED_STOP("Индексация не запущена");

    private final String message;

    Messages(String message) {
        this.message = message;
    }

    public Message getMessage() {
        return switch (this) {
            case SUCCESS -> new Message(true);
            case FAILED_PAGE_INDEX, FAILED_SEARCH, FAILED_START,
                    FAILED_STOP -> new ErrorMessage(message);
            default -> new Message(false);
        };
    }

    public String getStringMessage() {
        return message;
    }

    public Message getMessage(List<SearchResult> result) {
        if (this == SUCCESS_SEARCH) {
            return new SearchResultsMessage(result);
        }
        return getMessage();
    }

    @Getter
    @AllArgsConstructor
    public static class Message {
        private boolean result;
    }

    @Getter
    public static class SearchResultsMessage extends Message {
        private final int count;
        private final List<SearchResult> data;

        public SearchResultsMessage(List<SearchResult> data) {
            super(true);
            this.count = data.size();
            this.data = data;
        }
    }

    @Getter
    public static class ErrorMessage extends Message {
        private final String error;

        public ErrorMessage(String error) {
            super(false);
            this.error = error;
        }
    }
}
