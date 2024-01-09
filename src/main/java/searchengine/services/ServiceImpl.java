package searchengine.services;

import com.github.demidko.aot.WordformMeaning;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import searchengine.config.SiteList;
import searchengine.dto.IndexLemmaMining;
import searchengine.dto.LemmaMining;
import searchengine.dto.PageLemmas;
import searchengine.dto.SearchResult;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.entities.SiteLemma;
import searchengine.enums.Patterns;
import searchengine.enums.Statuses;
import searchengine.repositories.*;
import searchengine.services.utils.LemmaProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@org.springframework.stereotype.Service
@AllArgsConstructor
public class ServiceImpl implements Service {

    private final SiteList list;
    private final IndexingManager indexingManager;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteLemmaRepository siteLemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public boolean startIndexing() {

        if (indexingManager.isIndexing()) {

            return false;
        }

        list
                .getSites()
                .stream()
                .map(this::setStatusAndTime)
                .peek(siteRepository::insertOrUpdate)
                .map(Site::getUrl)
                .map(siteRepository::findByUrl)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::getRootPage)
                .forEach(indexingManager::startFindPageTask);

        return true;
    }

    private Site setStatusAndTime(Site site) {

        site.setStatus(Statuses.INDEXING);
        site.setStatusTime(
                Instant.now()
                        .truncatedTo(ChronoUnit.SECONDS)
        );
        return site;
    }

    private Page getRootPage(Site site) {

        return new Page(site, "/");
    }

    @Override
    public boolean stopIndexing() {

        if (indexingManager.isIndexing()) {

            indexingManager.stopIndexing();
            return true;
        }

        return false;
    }

    @Override
    public boolean indexPage(String url) {

        if (!url.endsWith("/")) {
            url = url.concat("/");
        }

        String[] urlParts = url.split("/", 4);

        if (urlParts.length < 4) {
            return false;
        }

        String path = "/".concat(urlParts[3]);

        String urlSite = urlParts[0]
                .concat("//")
                .concat(urlParts[2]);

        Optional<Site> optional = siteRepository
                .findByUrl(urlSite);

        if (optional.isEmpty()) {

            return false;
        }

        Site site = optional.get();

        indexingManager
                .startFindPageWithoutSubpagesTask(new Page(site, path));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return indexingManager.isIndexing();
    }

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics totalStatistics = new TotalStatistics(
                (int) siteRepository.count(),
                (int) pageRepository.count(),
                (int) lemmaRepository.count(),
                indexingManager.isIndexing()
        );

        List<DetailedStatisticsItem> details = siteRepository
                .findAll()
                .stream()
                .map(site ->
                        new DetailedStatisticsItem(
                                site.getUrl(),
                                site.getName(),
                                site.getStatus(),
                                site.getStatusTime().toEpochMilli(),
                                site.getLastError(),
                                pageRepository.countBySite(site),
                                siteLemmaRepository.countBySite(site)
                        )
                )
                .toList();

        return new StatisticsResponse(
                true,
                new StatisticsData(totalStatistics, details)
        );
    }

    @Override
    public Optional<List<SearchResult>> getSearchResults(
            String query, String siteUrl, int offset, int limit
    ) {

        Optional<Site> siteOptional = siteRepository
                .findByUrl(siteUrl);

        Map<String, WordformMeaning> meaningMap = LemmaProcessor
                .getRussianLemmas(query)
                .stream()
                .collect(Collectors
                        .toMap(
                                WordformMeaning::toString,
                                meaning -> meaning
                        )
                );

        return Optional.of(meaningMap
                .entrySet()
                .stream()
                .map(entry -> lemmaRepository
                        .findByLemma(entry.getKey())
                        .flatMap(lemma -> siteOptional
                                .map(site -> siteLemmaRepository
                                        .findBySiteAndLemma(site, lemma)
                                        .map(SiteLemma::getLemma)
                                )
                                .orElse(Optional.of(lemma))
                        )
                        .map(lemma ->
                                new DefaultMapEntry<>(
                                        lemma,
                                        entry.getValue()
                                )
                        )
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(entry -> indexRepository
                        .findAllByLemma(entry.getKey())
                        .stream()
                        .filter(index -> siteOptional.map(site ->
                                        site.equals(index.getPage().getSite())).orElse(true)
                        )
                        .map(index ->
                                new IndexLemmaMining(
                                        index,
                                        new LemmaMining(
                                                entry.getKey().getLemma(),
                                                entry.getValue()
                                        )
                                )
                        )
                )
                .collect(Collectors
                        .groupingBy(
                                indexLemmaMining -> indexLemmaMining
                                        .index()
                                        .getPage()
                        )
                )
                .entrySet()
                .stream()
                .map(PageLemmas::new)
                .sorted()
                .skip(offset)
                .limit(limit)
                .map(this::getSearchResult)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .toList());
    }

    private Optional<SearchResult> getSearchResult(PageLemmas pageLemmas) {

        Document document = Jsoup.parse(pageLemmas.page().getContent());

        String textContents = document
                .body()
                .getAllElements()
                .textNodes()
                .stream()
                .filter(textNode -> !textNode.isBlank())
                .map(TextNode::text)
                .filter(text -> pageLemmas
                        .lemmas()
                        .stream()
                        .map(LemmaMining::meaning)
                        .map(WordformMeaning::getTransformations)
                        .flatMap(List::stream)
                        .map(WordformMeaning::toString)
                        .anyMatch(text::contains)
                )
                .reduce(
                        new StringBuilder(),
                        (builder, s) -> builder
                                .append(" ")
                                .append(s),
                        StringBuilder::append
                )
                .toString();

        Map<Integer, Integer> meaningsPosition = pageLemmas
                .lemmas()
                .stream()
                .flatMap(lemmaMining -> lemmaMining.meaning()
                        .getTransformations()
                        .stream()
                        .map(WordformMeaning::toString)
                        .flatMap(meaning ->
                                        Patterns.SAMPLE
                                                .getPattern(meaning)
                                                .matcher(textContents.toLowerCase())
                                                .results()
                                                .map(result ->
                                                        new DefaultMapEntry<>(
                                                                result.start(),
                                                                result.end()
                                                        )
                                                )
                        )
                )
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Integer::max
                        )
                );

        return meaningsPosition.isEmpty() ? Optional.empty() :
                Optional.of(
                        new SearchResult(
                                pageLemmas.page().getSite().getUrl(),
                                pageLemmas.page().getSite().getName(),
                                pageLemmas.page().getPath(),
                                document.title(),
                                buildSnippet(textContents, meaningsPosition),
                                pageLemmas.relevance(),
                                meaningsPosition.size()
                        )
                );
    }

    private String buildSnippet(String textContent, Map<Integer, Integer> meaningsPosition) {

        StringBuilder snippetBuilder = new StringBuilder();

        AtomicInteger prevPos = new AtomicInteger();
        prevPos.set(textContent.length());

        List<String> strings = meaningsPosition
                .entrySet()
                .stream()
                .flatMap(entry -> prevPos.getAndSet(0) == textContent.length() ?
                        Stream.of(
                                entry.getKey(),
                                entry.getValue(),
                                textContent.length()
                        ) :
                        Stream.of(
                                entry.getKey(),
                                entry.getValue()
                        )
                )
                .sorted()
                .map(pos -> textContent
                        .substring(prevPos.getAndSet(pos), pos)
                )
                .toList();

        for (int i = 0; i < strings.size(); i++) {

            if (i % 2 == 0) {

                if (i == strings.size() - 1) {

                    snippetBuilder.append(Patterns.FIRST_STRING_PART
                            .modifyString(strings.get(i)));
                } else if (i > 0){

                    snippetBuilder.append(Patterns.MIDDLE_STRING_PART
                            .modifyString(strings.get(i)));
                } else {

                    snippetBuilder.append(Patterns.LAST_STRING_PART
                            .modifyString(strings.get(i)));
                }

                if (snippetBuilder.length() > Patterns.MAX_SNIPPET_LENGTH.getIntValue()) {

                    break;
                }

            } else {
                snippetBuilder
                        .append(
                                Patterns.HIGHLIGHTED_STRING_PART
                                .getStringValue(strings.get(i))
                        );
            }
        }

        return Patterns.REMOVE_REDUNDANT_SPACES
                .modifyString(snippetBuilder.toString());
    }
}