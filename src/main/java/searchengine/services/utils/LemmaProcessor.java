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

    private static StanfordCoreNLP englishLemmaPipeline;

    private static void initEnglishLemmaPipeline() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, pos, lemma");
        englishLemmaPipeline = new StanfordCoreNLP(props);
    }

    public static List<WordFormMeanings> getLemmas(String text) {
        return Patterns.WORD.getRedexPattern()
                .matcher(text.toLowerCase())
                .results()
                .map(MatchResult::group)
                .map(WordFormMeanings::new)
                .filter(WordFormMeanings::isMeaningNotEmpty)
                .toList();
    }

    public static CoreLabel getEnglishLemma(String word) {
        if (englishLemmaPipeline == null) {
            initEnglishLemmaPipeline();
        }
        Annotation document = new Annotation(word);
        englishLemmaPipeline.annotate(document);
        return document.get(CoreAnnotations.TokensAnnotation.class).get(0);
    }
}
