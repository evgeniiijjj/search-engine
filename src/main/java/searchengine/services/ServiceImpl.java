package searchengine.services;

import com.github.demidko.aot.WordformMeaning;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import searchengine.config.SiteList;
import searchengine.dto.*;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.entities.Index;
import searchengine.entities.Lemma;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Patterns;
import searchengine.enums.Statuses;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.utils.LemmaProcessor;
import searchengine.services.utils.NodeTextProcessor;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@org.springframework.stereotype.Service
@AllArgsConstructor
public class ServiceImpl implements Service {

    private final SiteList list;
    private final IndexingManager indexingManager;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
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

        String siteUrl;
        String path;

        try {
            URI uri = new URI(url);
            siteUrl = uri.getScheme()
                    .concat("://")
                    .concat(uri.getHost());
            path = uri.getPath();

        } catch (URISyntaxException e) {

            return false;
        }

        Optional<Site> optional = siteRepository
                .findByUrl(siteUrl);

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
                                siteRepository.lemmasCount(site)
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

        Map<Lemma, WordformMeaning> lemmas = getLemmasMap(query);

        Map<Page, List<Index>> indexes = getIndexesMap(lemmas, siteUrl);

        List<SearchResult> results = indexes
                .entrySet()
                .stream()
                .map(entry ->
                        new PageLemmas(
                                entry.getKey(),
                                entry
                                        .getValue()
                                        .stream()
                                        .map(Index::getLemma)
                                        .map(lemmas::get)
                                        .toList(),
                                entry
                                        .getValue()
                                        .stream()
                                        .map(Index::getRank)
                                        .reduce(Float::sum)
                                        .orElse(0F)
                        )
                )
                .sorted()
                .skip(offset)
                .limit(limit)
                .map(this::getSearchResult)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .toList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results);
    }

    private Map<Lemma, WordformMeaning> getLemmasMap(String query) {

        return LemmaProcessor
                .getRussianLemmas(Patterns.REMOVE_PUNCTUATION_MARKS.modifyString(query))
                .stream()
                .map(wfm -> lemmaRepository
                        .findByLemma(wfm.toString())
                        .map(lemma ->
                                new DefaultMapEntry<>(
                                        lemma,
                                        wfm
                                )
                        )
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors
                        .toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )
                );
    }

    private Map<Page, List<Index>> getIndexesMap(Map<Lemma, WordformMeaning> lemmas, String siteUrl) {

        return lemmas
                .keySet()
                .stream()
                .flatMap(lemma -> indexRepository
                        .findAllByLemmaOrderByRankDescLimit(
                                lemma,
                                Patterns.MOST_RELEVANT_INDEXES_COUNT_LIMIT.getIntValue()
                        )
                        .stream()
                )
                .filter(index -> siteRepository
                        .findByUrl(siteUrl)
                        .map(site -> site
                                .equals(index.getPage().getSite())
                        )
                        .orElse(true)
                )
                .collect(Collectors
                        .groupingBy(
                                Index::getPage
                        )
                );
    }

    private Optional<SearchResult> getSearchResult(PageLemmas pageLemmas) {

        Document document = Jsoup.parse(pageLemmas.page().getContent());

        return document.body()
                .getAllElements()
                .textNodes()
                .stream()
                .filter(textNode -> !textNode.isBlank())
                .map(TextNode::text)
                .distinct()
                .map(text -> new NodeTextProcessor(text, pageLemmas))
                .filter(NodeTextProcessor::isWordMeaningPositionsNotEmpty)
                .map(ntp ->
                        new Snippet(
                                ntp.getSnippetPart(),
                                ntp.getWordMeaningCount(),
                                ntp.getMaxMeaningsContinuousSequence()
                        )
                )
                .reduce(Snippet::accumulate)
                .map(snippet ->
                        new SearchResult(
                                pageLemmas.page().getSite().getUrl(),
                                pageLemmas.page().getSite().getName(),
                                pageLemmas.page().getPath(),
                                document.title(),
                                snippet.toString(),
                                pageLemmas.relevance(),
                                snippet.wordMeaningCount(),
                                snippet.maxMeaningContinuousSequence()
                        )
                );
    }
}