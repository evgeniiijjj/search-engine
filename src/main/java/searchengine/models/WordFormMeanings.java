package searchengine.models;

import com.github.demidko.aot.PartOfSpeech;
import com.github.demidko.aot.WordformMeaning;
import edu.stanford.nlp.ling.CoreLabel;
import searchengine.enums.Patterns;
import searchengine.services.utils.LemmaProcessor;
import java.util.List;

public class WordFormMeanings {

        private String lemma;
        private WordformMeaning wordformMeaning;

        public WordFormMeanings(String word) {
            setWordformMeaning(word);
        }

        public WordFormMeanings(WordformMeaning wordformMeaning) {
            this.wordformMeaning = wordformMeaning;
        }

        private void setWordformMeaning(String word) {
            if (Patterns.CONTAINS_RUSSIAN_LETTERS.isMatches(word)) {
                List<WordformMeaning> meanings = WordformMeaning.lookupForMeanings(word);
                if (!meanings.isEmpty()) {
                    WordformMeaning meaning = meanings.get(0).getLemma();
                    if (stopWordsFilter(meaning)) {
                        wordformMeaning = meaning;
                    }
                }
            } else {
                if (Patterns.CONTAINS_ENGLISH_LETTERS.isMatches(word)) {
                    lemma = getEnglishLemma(word);
                }
            }
        }

        public List<WordFormMeanings> getTransformations() {
            if (lemma != null) {
                return List.of(this);
            }
            return wordformMeaning
                    .getTransformations()
                    .stream()
                    .filter(this::stopWordsFilter)
                    .map(WordFormMeanings::new)
                    .toList();
        }

        private boolean stopWordsFilter(WordformMeaning meaning) {
            PartOfSpeech partOfSpeech = meaning.getPartOfSpeech();
            return meaning.toString().length() > 2 &&
                    !partOfSpeech.equals(PartOfSpeech.BriefCommunion) &&
                    !partOfSpeech.equals(PartOfSpeech.Union) &&
                    !partOfSpeech.equals(PartOfSpeech.Pronoun);
        }

        private String getEnglishLemma(String word) {
            CoreLabel label = LemmaProcessor.getEnglishLemma(word);
            String lemma = label.lemma();
            if (lemma.length() < 3) {
                return null;
            }
            return lemma;
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