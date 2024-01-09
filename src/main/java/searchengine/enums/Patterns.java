package searchengine.enums;

import java.util.regex.Pattern;


public enum Patterns {

    EMPTY_STRING(""),
    LINE_BREAK_PLACEHOLDER("..."),
    MAX_SNIPPET_LENGTH("1000"),
    MAX_STRING_LENGTH("100"),
    MIDDLE_STRING_PART(""),
    FIRST_STRING_PART(""),
    LAST_STRING_PART(""),
    HIGHLIGHTED_STRING_PART(" <b>%s</b> "),
    HTML_TAG_A("a"),
    HTML_TAG_ATTRIBUTE_HREF("href"),
    ONE_SPACE(" "),
    REMOVE_REDUNDANT_SPACES("\\s+"),
    REMOVE_SPACES_BEFORE_PUNCTUATION_MARKS("\\s+(?=[,\\.\\?!])"),
    REMOVE_SPACES_AT_LINE_BEGINNING("^\\s+"),
    SAMPLE("%s"),
    STRING_SPLITTER("[\\.\\?!] ?"),
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
            default -> string;
        };
    }

    private String getFirstStringPart(String[] strings) {

        int maxStringLength = MAX_STRING_LENGTH.getIntValue();
        String string = strings[0];
        if (string.length() > maxStringLength) {

            return LINE_BREAK_PLACEHOLDER.getStringValue()
                    .concat(string.substring(string.length() - maxStringLength));
        }
        return string;
    }

    private String getMiddleStringPart(String[] strings) {

        int maxStringLength = MAX_STRING_LENGTH.getIntValue();
        String string = strings[0];

        if (strings.length < 2) {

            if (string.length() > maxStringLength) {

                return string.substring(0, maxStringLength / 2)
                        .concat(LINE_BREAK_PLACEHOLDER.getStringValue())
                        .concat(string.substring(string.length() - maxStringLength / 2));
            }

            return string;

        }

        return getLastStringPart(strings).concat(" ")
                .concat(getFirstStringPart(strings));
    }

    private String getLastStringPart(String[] strings) {

        int maxStringLength = MAX_STRING_LENGTH.getIntValue();
        String string = strings[strings.length - 1];
        if (string.length() > maxStringLength) {

            return string.substring(0, maxStringLength)
                    .concat(LINE_BREAK_PLACEHOLDER.getStringValue());
        }
        return string;
    }

    private String[] splitString(String string) {

        return string.split(Patterns.STRING_SPLITTER.getStringValue());
    }

    private String removeRedundantSpaces(String string) {

        return string.replaceAll(pattern, ONE_SPACE.pattern)
                .replaceAll(REMOVE_SPACES_BEFORE_PUNCTUATION_MARKS.pattern, EMPTY_STRING.pattern)
                .replaceAll(REMOVE_SPACES_AT_LINE_BEGINNING.pattern, EMPTY_STRING.pattern);
    }
}
