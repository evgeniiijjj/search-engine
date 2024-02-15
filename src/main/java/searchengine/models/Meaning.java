package searchengine.models;

public record Meaning(String word,
                      boolean stopWord) {
    public boolean isEmpty() {
        return word.isEmpty();
    }

    @Override
    public String toString() {
        return word;
    }

    @Override
    public int hashCode() {
        return word.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(Meaning.class)) {
            return false;
        }
        Meaning m = (Meaning) o;
        return word.equals(m.word);
    }
}
