package searchengine.enums;

import java.util.regex.Pattern;


public enum Patterns {

    END_LINE(". "),
    EMPTY_STRING(""),
    LINE_BREAK_PLACEHOLDER("..."),
    MAX_SNIPPET_LENGTH("10000"),
    MAX_STRING_LENGTH("100"),
    MIDDLE_STRING_PART(""),
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
    REMOVE_SPACES_AT_LINE_BEGINNING("^\\s+"),
    REMOVE_REDUNDANT_SYMBOLS(""),
    SAMPLE("%s"),
    STRING_SPLITTER("(?<=(.|$))[\\.\\?!]"),
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

            case MAX_STRING_LENGTH, MAX_SNIPPET_LENGTH ->
                    Integer.parseInt(this.pattern);
            default -> 0;
        };
    }

    public String modifyString(String string) {

        return switch(this) {

            case FIRST_STRING_PART -> getFirstStringPart(splitString(string));
            case MIDDLE_STRING_PART -> getMiddleStringPart(splitString(string));
            case LAST_STRING_PART -> getLastStringPart(splitString(string));
            case REMOVE_REDUNDANT_SPACES -> removeRedundantSpaces(string);
            case REMOVE_REDUNDANT_DOTS -> removeRedundantDots(string);
            case REMOVE_REDUNDANT_SYMBOLS -> removeRedundantSymbols(string);
            default -> string;
        };
    }

    private String getFirstStringPart(String[] strings) {

        String string = strings[strings.length - 1];
        string = trimString(string, MAX_STRING_LENGTH.getIntValue(), false);

        return string;
    }

    private String getMiddleStringPart(String[] strings) {

        String string = strings[0];
        int maxStringLength = MAX_STRING_LENGTH.getIntValue();

        if (strings.length < 2) {

            return trimString(string, maxStringLength / 2, true)
                    .concat(trimString(string, maxStringLength / 2, false));
        }

        return trimString(string, maxStringLength / 2, true)
                .concat(trimString(strings[strings.length - 1], maxStringLength / 2, false));
    }

    private String getLastStringPart(String[] strings) {

        String string = strings[0];
        string = trimString(string, MAX_STRING_LENGTH.getIntValue(), true);

        return string;
    }

    private String trimString(String string, int size, boolean trimEndString) {

        if (string.length() > size) {

            int spaceIndex = string.indexOf(Patterns.ONE_SPACE.pattern,
                    trimEndString ? 1 : -1 * (size - string.length()));

            string = trimEndString ? string.substring(0, spaceIndex) :
                    string.substring(spaceIndex);

            string = trimEndString ? string.concat(LINE_BREAK_PLACEHOLDER.getStringValue()) :
                    LINE_BREAK_PLACEHOLDER.getStringValue().concat(string);
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
}
