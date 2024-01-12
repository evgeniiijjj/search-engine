package searchengine.enums;

import java.util.regex.Pattern;


public enum Patterns {

    END_LINE(". "),
    EMPTY_STRING(""),
    LINE_BREAK_PLACEHOLDER("..."),
    MAX_SNIPPET_LENGTH("500"),
    MAX_STRING_LENGTH("100"),
    MIDDLE_STRING_PART(""),
    MOST_RELEVANT_INDEXES_COUNT_LIMIT("10000"),
    FIRST_STRING_PART(""),
    LAST_STRING_PART(""),
    LINE_SEPARATOR("\n"),
    HIGHLIGHTED_STRING_PART("<b>%s</b>"),
    HTML_TAG_A("a"),
    HTML_TAG_ATTRIBUTE_HREF("href"),
    ONE_SPACE(" "),
    REMOVE_REDUNDANT_DOTS("\\.\\.\\.+"),
    REMOVE_REDUNDANT_SPACES("\\s+"),
    REMOVE_SPACES_BEFORE_PUNCTUATION_MARKS("\\s+(?=[,\\.\\?!])"),
    REMOVE_PUNCTUATION_MARKS("[\\.\\?!,:;]+"),
    REMOVE_SPACES_AT_LINE_BEGINNING("^\\s+"),
    REMOVE_REDUNDANT_SYMBOLS(""),
    SAMPLE("%s"),
    STRING_SPLITTER("(?<=[\\.\\?!]) "),
    WORD("\\w+");

    private final String pattern;


    Patterns(String pattern) {

        this.pattern = pattern;
    }

    public Pattern getPattern(String... strings) {

        if (strings.length == 1) {

            return Pattern.compile(String.format(pattern,
                    strings[0]), Pattern.CASE_INSENSITIVE);
        }

        return Pattern.compile(pattern,
                    Pattern.UNICODE_CHARACTER_CLASS);
    }

    public String getStringValue(String... strings) {

        if (strings.length == 1) {

            return String.format(pattern, strings[0]);
        }

        return pattern;
    }

    public int getIntValue() {

        return switch (this) {

            case MAX_STRING_LENGTH,
                    MAX_SNIPPET_LENGTH,
                    MOST_RELEVANT_INDEXES_COUNT_LIMIT ->
                    Integer.parseInt(this.pattern);
            default -> 0;
        };
    }

    public String modifyString(String string) {

        return switch(this) {

            case FIRST_STRING_PART -> getFirstStringPart(string);
            case MIDDLE_STRING_PART -> getMiddleStringPart(string);
            case LAST_STRING_PART -> getLastStringPart(string);
            case REMOVE_REDUNDANT_SPACES -> removeRedundantSpaces(string);
            case REMOVE_REDUNDANT_DOTS -> removeRedundantDots(string);
            case REMOVE_REDUNDANT_SYMBOLS -> removeRedundantSymbols(string);
            case REMOVE_PUNCTUATION_MARKS -> removePunctuationMarks(string);
            default -> string;
        };
    }

    private String getFirstStringPart(String string) {

        string = LINE_BREAK_PLACEHOLDER.pattern
                .concat(trimString(string, MAX_STRING_LENGTH.getIntValue(), false));

        return string;
    }

    private String getMiddleStringPart(String string) {

        int maxStringLength = MAX_STRING_LENGTH.getIntValue();

        if (string.length() > maxStringLength) {

            String[] strings = STRING_SPLITTER.splitString(string);

            String first = strings[0];

            String last = strings[strings.length - 1];

            return trimString(first, maxStringLength / 2, true)
                    .concat(ONE_SPACE.pattern)
                    .concat(LINE_BREAK_PLACEHOLDER.pattern)
                    .concat(trimString(last, maxStringLength / 2, false));
        }
        return string;
    }

    private String getLastStringPart(String string) {

        string = trimString(string, MAX_STRING_LENGTH.getIntValue(), true)
                .concat(LINE_BREAK_PLACEHOLDER.pattern);

        return string;
    }

    private String trimString(String string, int size, boolean trimEndString) {

        if (string.length() > size) {

            int spaceIndex = trimEndString ? string.indexOf(Patterns.ONE_SPACE.pattern, size) :
                    string.lastIndexOf(Patterns.ONE_SPACE.pattern, string.length() - size);

            if (spaceIndex < 0) {

                spaceIndex = size;
            }

            string = trimEndString ? string.substring(0, spaceIndex) :
                    string.substring(spaceIndex);
        }

        return string;
    }

    private String[] splitString(String string) {

        return string.split(STRING_SPLITTER.pattern);
    }

    private String removeRedundantSymbols(String string) {

        string = removeRedundantSpaces(string);
        string = removeRedundantDots(string);

        return string;
    }

    private String removeRedundantSpaces(String string) {

        return string.replaceAll(REMOVE_REDUNDANT_SPACES.pattern, ONE_SPACE.pattern)
                .replaceAll(REMOVE_SPACES_BEFORE_PUNCTUATION_MARKS.pattern, EMPTY_STRING.pattern)
                .replaceAll(REMOVE_SPACES_AT_LINE_BEGINNING.pattern, EMPTY_STRING.pattern);
    }

    private String removeRedundantDots(String string) {

        return string.replaceAll(REMOVE_REDUNDANT_DOTS.pattern, LINE_BREAK_PLACEHOLDER.pattern);
    }

    private String removePunctuationMarks(String string) {

        return string.replaceAll(REMOVE_PUNCTUATION_MARKS.pattern, EMPTY_STRING.pattern);
    }
}
