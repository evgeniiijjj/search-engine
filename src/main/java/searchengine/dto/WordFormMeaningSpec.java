package searchengine.dto;

import com.github.demidko.aot.PartOfSpeech;
import com.github.demidko.aot.WordformMeaning;
import lombok.AllArgsConstructor;

import java.util.List;


@AllArgsConstructor
public class WordFormMeaningSpec {

    private final WordformMeaning wordformMeaning;

    public List<WordformMeaning> getTransformations() {

        return wordformMeaning
                .getTransformations()
                .stream()
                .filter(this::partOfSpeechFilter)
                .toList();
    }

    public WordFormMeaningSpec getLemma() {

        return new WordFormMeaningSpec(wordformMeaning.getLemma());
    }

    private boolean partOfSpeechFilter(WordformMeaning meaning) {

        PartOfSpeech partOfSpeech = meaning.getPartOfSpeech();

        return !partOfSpeech.equals(PartOfSpeech.Verb) &&
                !partOfSpeech.equals(PartOfSpeech.Infinitive) &&
                !partOfSpeech.equals(PartOfSpeech.BriefCommunion) &&
                !partOfSpeech.equals(PartOfSpeech.VerbalParticiple) &&
                !partOfSpeech.equals(PartOfSpeech.Pronoun);
    }

    @Override
    public String toString() {
        return wordformMeaning.toString();
    }
}
