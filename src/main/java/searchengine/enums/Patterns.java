package searchengine.enums;

import java.util.Arrays;
import java.util.regex.Pattern;

public enum Patterns {
    COMA(","),
    CONTAINS_RUSSIAN_LETTERS("[ёа-я]+"),
    CONTAINS_ENGLISH_LETTERS("[a-z]+"),
    EMPTY_STRING(""),
    FIRST_STRING_PART(""),
    HIGHLIGHTED_STRING_PART("<b>%s</b>"),
    HTML_TAG_A("a"),
    HTML_TAG_ATTRIBUTE_HREF("href"),
    HTML_TEXT_TAG_NAMES("p,h1,h2,h3,h4,h5,h6,b,strong,i,em,u,pre," +
            "sup,sub,small,address,mark,abbr,kdb,dfn,ins,del,s,q,blockquote,cite"),
    LAST_STRING_PART(""),
    LINE_BREAK_PLACEHOLDER("..."),
    MAX_SNIPPET_LENGTH("500"),
    MAX_STRING_LENGTH("100"),
    MIDDLE_STRING_PART(""),
    NOT_RELEVANT_PAGE_PATH("/.*[?,:].*"),
    ONE_SPACE(" "),
    REMOVE_PUNCTUATION_MARKS("[\\.\\?!,:;]+"),
    ROOT_PATH("/"),
    SAMPLE("%s"),
    STRING_SPLITTER("(?<=[\\.\\?!] )"),
    WORD("\\w+");

    private final String pattern;

    Patterns(String pattern) {
        this.pattern = pattern;
    }

    public Pattern getRedexPattern(String... strings) {
        if (strings.length == 1) {
            return Pattern.compile(String.format(pattern,
                    strings[0]), Pattern.CASE_INSENSITIVE);
        }
        return Pattern.compile(pattern,
                    Pattern.UNICODE_CHARACTER_CLASS);
    }

    public String getStringValue(String... strings) {
        if (strings.length == 1) {
            String string = strings[0];
            return switch(this) {
                case FIRST_STRING_PART, MIDDLE_STRING_PART, LAST_STRING_PART ->
                        trimString(string, MAX_STRING_LENGTH.getIntValue());
                case REMOVE_PUNCTUATION_MARKS ->
                        removePunctuationMarks(string);
                case HIGHLIGHTED_STRING_PART, SAMPLE, WORD ->
                        String.format(pattern, string);
                default -> string;
            };
        }
        return pattern;
    }

    private int getIntValue() {
        return switch (this) {
            case MAX_STRING_LENGTH,
                    MAX_SNIPPET_LENGTH ->
                    Integer.parseInt(this.pattern);
            default -> 0;
        };
    }

    public boolean isMatches(String string) {
        return switch (this) {
            case HTML_TEXT_TAG_NAMES ->
                Arrays.asList(pattern.split(COMA.pattern))
                        .contains(string);
            case NOT_RELEVANT_PAGE_PATH,
                    CONTAINS_RUSSIAN_LETTERS,
                    CONTAINS_ENGLISH_LETTERS ->
                    getRedexPattern().matcher(string).matches();
            default -> false;
        };
    }

    private String trimString(String string, int size) {
        if (string.length() > size) {
            String[] strings = splitString(string);
            String first = strings[0];
            String last = strings[strings.length - 1];
            if (this == FIRST_STRING_PART) {
                return last;
            }
            if (this == MIDDLE_STRING_PART) {
                if (first.equals(last)) {
                    return first;
                }
                return LAST_STRING_PART.trimString(first, size / 2)
                        .concat(FIRST_STRING_PART.trimString(last, size / 2));
            }
            if (this == LAST_STRING_PART) {
                if (first.length() > size) {
                    first = first
                            .substring(
                                    0,
                                    getSpaceIndex(first, size)
                            )
                            .concat(LINE_BREAK_PLACEHOLDER.pattern);
                }
                return first;
            }
        }
        return string;
    }

    private int getSpaceIndex(String string, int indexFrom) {
        int spaceIndex = string.lastIndexOf(
                ONE_SPACE.pattern,
                indexFrom
        );
        if (spaceIndex < 0) {
            spaceIndex = indexFrom;
        }
        return spaceIndex;
    }

    private String[] splitString(String string) {
        return string.split(STRING_SPLITTER.pattern);
    }

    private String removePunctuationMarks(String string) {
        return string.replaceAll(REMOVE_PUNCTUATION_MARKS.pattern, EMPTY_STRING.pattern);
    }
}
