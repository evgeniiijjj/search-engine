package searchengine.services.utils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import searchengine.enums.Patterns;

import java.util.List;
import java.util.Properties;
import java.util.regex.MatchResult;


public class LemmaProcessor {

    private static StanfordCoreNLP lemmatizer;

    private static void initLemmatizer() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, pos, lemma");
        lemmatizer = new StanfordCoreNLP(props);
    }

    public static List<WordFormMeaningSpec> getLemmas(String text) {

        return Patterns.WORD.getRedexPattern()
                .matcher(text.toLowerCase())
                .results()
                .map(MatchResult::group)
                .map(WordFormMeaningSpec::new)
                .filter(WordFormMeaningSpec::isMeaningNotEmpty)
                .toList();
    }

    public static CoreLabel getEnglishLemma(String word) {

        if (lemmatizer == null) {
            initLemmatizer();
        }
        Annotation document = new Annotation(word);
        lemmatizer.annotate(document);
        return document.get(CoreAnnotations.TokensAnnotation.class).get(0);
    }
}
