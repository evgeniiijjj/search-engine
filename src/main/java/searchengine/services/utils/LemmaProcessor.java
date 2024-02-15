package searchengine.services.utils;

import com.github.demidko.aot.PartOfSpeech;
import com.github.demidko.aot.WordformMeaning;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import searchengine.enums.Patterns;
import searchengine.models.Meaning;

import java.util.List;
import java.util.Properties;
import java.util.regex.MatchResult;
import java.util.stream.Stream;

public class LemmaProcessor {

    private static StanfordCoreNLP englishLemmaPipeline;

    private static void initEnglishLemmaPipeline() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, pos, lemma");
        englishLemmaPipeline = new StanfordCoreNLP(props);
    }

    public static List<Meaning> getLemmas(String text, boolean lemmaTransformations) {
        return Patterns.WORD.getRedexPattern()
                .matcher(text.toLowerCase())
                .results()
                .map(MatchResult::group)
                .flatMap(word -> Patterns.CONTAINS_ENGLISH_LETTERS.isMatches(word) ?
                        getEnglishLemma(word) : getRussianLemma(word, lemmaTransformations))
                .distinct()
                .filter(meaning -> !meaning.isEmpty())
                .toList();
    }

    public static Stream<Meaning> getEnglishLemma(String word) {
        if (englishLemmaPipeline == null) {
            initEnglishLemmaPipeline();
        }
        Annotation document = new Annotation(word);
        englishLemmaPipeline.annotate(document);
        String lemma = document
                .get(CoreAnnotations.TokensAnnotation.class)
                .get(0)
                .lemma();
        if (lemma.length() < 3) {
            return Stream.of(new Meaning("", false));
        }
        return Stream.of(new Meaning(lemma, false));
    }

    public static Stream<Meaning> getRussianLemma(String word, boolean transformations) {
        List<WordformMeaning> list = WordformMeaning.lookupForMeanings(word);
        if (list.isEmpty()) {
            return Stream.of(new Meaning(word, false));
        }
        if (transformations) {
            return list.stream().map(wfm -> new Meaning(wfm.toString(), isStopWord(wfm)));
        }
        WordformMeaning wfm = list.get(0).getLemma();
        if (isStopWord(wfm)) {
            return Stream.of(new Meaning("", false));
        }
        return Stream.of(new Meaning(wfm.toString(), false));
    }

    private static boolean isStopWord(WordformMeaning wfm) {
        PartOfSpeech partOfSpeech = wfm.getPartOfSpeech();
        return wfm.toString().length() < 3 ||
                partOfSpeech.equals(PartOfSpeech.BriefCommunion) ||
                partOfSpeech.equals(PartOfSpeech.Union) ||
                partOfSpeech.equals(PartOfSpeech.Pronoun);
    }
}
