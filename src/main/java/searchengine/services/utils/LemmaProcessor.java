package searchengine.services.utils;

import com.github.demidko.aot.WordformMeaning;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import searchengine.dto.WordFormMeaningSpec;
import searchengine.enums.Patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.MatchResult;


public class LemmaProcessor {

    public static List<WordFormMeaningSpec> getRussianLemmas(String text) {

        return Patterns.WORD.getRedexPattern()
                .matcher(text)
                .results()
                .map(MatchResult::group)
                .map(WordformMeaning::lookupForMeanings)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(WordFormMeaningSpec::new)
                .map(WordFormMeaningSpec::getLemma)
                .distinct()
                .toList();
    }

    public static List<String> getEnglishLemmas(String text) {

        List<String> lemmas = new ArrayList<>();
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP lemmatizer = new StanfordCoreNLP(props);
        Annotation document = new Annotation(text);
        lemmatizer.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                lemmas.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return lemmas;
    }
}
