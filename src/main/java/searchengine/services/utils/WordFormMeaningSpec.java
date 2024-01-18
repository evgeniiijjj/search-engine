package searchengine.services.utils;

import com.github.demidko.aot.PartOfSpeech;
import com.github.demidko.aot.WordformMeaning;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import searchengine.enums.Patterns;

import java.util.List;


public class WordFormMeaningSpec {

    private String lemma;
    private WordformMeaning wordformMeaning;

    public WordFormMeaningSpec(String word) {
        setWordformMeaning(word);
    }

    public WordFormMeaningSpec(WordformMeaning wordformMeaning) {
        this.wordformMeaning = wordformMeaning;
    }

    private void setWordformMeaning(String word) {

        if (Patterns.CONTAINS_RUSSIAN_LETTERS.isMatches(word)) {
            List<WordformMeaning> meanings = WordformMeaning.lookupForMeanings(word);
            if (!meanings.isEmpty()) {
                wordformMeaning = meanings.get(0).getLemma();
            }
        } else {
            if (Patterns.CONTAINS_ENGLISH_LETTERS.isMatches(word)) {
                lemma = getEnglishLemma(word);
            }
        }
    }

    public List<WordFormMeaningSpec> getTransformations() {

        if (lemma != null) {
            return List.of(this);
        }

        return wordformMeaning
                .getTransformations()
                .stream()
                .filter(this::partOfSpeechFilter)
                .map(WordFormMeaningSpec::new)
                .toList();
    }

    private boolean partOfSpeechFilter(WordformMeaning meaning) {
        PartOfSpeech partOfSpeech = meaning.getPartOfSpeech();
        return !partOfSpeech.equals(PartOfSpeech.Verb) &&
                !partOfSpeech.equals(PartOfSpeech.Infinitive) &&
                !partOfSpeech.equals(PartOfSpeech.BriefCommunion) &&
                !partOfSpeech.equals(PartOfSpeech.VerbalParticiple) &&
                !partOfSpeech.equals(PartOfSpeech.Pronoun);
    }

    private String getEnglishLemma(String word) {

        CoreLabel label = LemmaProcessor.getEnglishLemma(word);
        String partOfSpeech = label.get(CoreAnnotations.PartOfSpeechAnnotation.class);

        if (partOfSpeech.equals("VB") ||
                partOfSpeech.equals("PRP") ||
                partOfSpeech.equals("IN") ||
                partOfSpeech.equals("TO") ||
                partOfSpeech.equals("DT")
        ) {
            return null;
        }

        return label.lemma();
    }

    public boolean isMeaningNotEmpty() {
        return wordformMeaning != null || lemma != null;
    }

    @Override
    public String toString() {

        if (lemma != null) {
            return lemma;
        }
        return wordformMeaning.toString();
    }
}
