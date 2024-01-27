package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import searchengine.config.SiteList;
import searchengine.models.PageLemmas;
import searchengine.models.SearchResult;
import searchengine.models.Snippet;
import searchengine.models.statistics.DetailedStatisticsItem;
import searchengine.models.statistics.StatisticsData;
import searchengine.models.statistics.StatisticsResponse;
import searchengine.models.statistics.TotalStatistics;
import searchengine.entities.Index;
import searchengine.entities.Lemma;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Constants;
import searchengine.enums.Messages;
import searchengine.enums.Patterns;
import searchengine.enums.Statuses;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.utils.LemmaProcessor;
import searchengine.services.utils.SnippetGenerator;
import searchengine.services.utils.WordFormMeanings;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

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
        list.getSites().stream()
                .map(this::setStatusSite)
                .toList()
                .stream()
                .map(this::getRootPage)
                .forEach(indexingManager::startIndexingPageTask);
        return true;
    }

    private Site setStatusSite(Site site) {
        return updateStatusSite(site, Statuses.INDEXING, null);
    }

    private Site updateStatusSite(Site site,
                                  Statuses status,
                                  String lastError) {
        Optional<Site> optional = siteRepository.findByUrl(site.getUrl());
        if (optional.isPresent()) {
            site = optional.get();
            indexRepository.deleteAllBySite(site);
            pageRepository.deleteAllBySite(site);
            lemmaRepository.deleteAllBySite(site);
        }
        site.setStatus(status);
        site.setStatusTime(
                Instant.now()
                        .truncatedTo(ChronoUnit.SECONDS)
        );
        site.setLastError(lastError);
        site = siteRepository.save(site);
        return site;
    }

    private Page getRootPage(Site site) {
        return new Page(site, "/");
    }

    @Override
    public boolean stopIndexing() {
        if (indexingManager.isIndexing()) {
            List<Site> sites = siteRepository.findAll();
            sites.forEach(site ->
                    updateStatusSite(site, Statuses.FAILED,
                            Messages.INTERRUPTED_INDEXING.getStringMessage())
            );
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
            siteUrl = uri.getScheme().concat("://")
                    .concat(uri.getHost());
            path = uri.getPath();
        } catch (URISyntaxException e) {
            log.info(e.getMessage());
            return false;
        }
        Optional<Site> optionalSite =
                siteRepository.findByUrl(siteUrl);
        if (optionalSite.isEmpty()) {
            return false;
        }
        Site site = optionalSite.get();
        Page page = new Page(site, path);
        Optional<Page> optionalPage =
                pageRepository.findBySiteAndPath(site, path);
        if (optionalPage.isPresent()) {
            page = optionalPage.get();
            deletePageData(page);
        }
        indexingManager.startIndexingPageTask(page);
        try {
            Thread.sleep(Constants.TIMEOUT_150_MS.getValue());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return indexingManager.isIndexing();
    }

    private void deletePageData(Page page) {
        List<Index> indexes = indexRepository.findAllByPage(page);
        indexRepository.deleteAll(indexes);
        List<Lemma> lemmas = indexes.stream()
                .map(Index::getLemma)
                .toList();
        lemmaRepository.deleteAll(
                lemmas.stream()
                        .filter(lemma -> lemma.getFrequency() == 1)
                        .toList()
        );
        lemmaRepository.saveAllAndFlush(
                lemmas.stream()
                        .filter(lemma -> lemma.getFrequency() > 1)
                        .peek(lemma -> lemma.setFrequency(lemma.getFrequency() - 1))
                        .toList()
        );
        pageRepository.delete(page);
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
                                getSiteStatus(site),
                                site.getStatusTime().toEpochMilli(),
                                site.getLastError() != null ? site.getLastError() : "none",
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

    private String getSiteStatus(Site site) {
        if (indexingManager.isIndexing()) {
            return Statuses.INDEXING.name();
        }
        return site.getStatus();
    }

    @Override
    public List<SearchResult> getSearchResults(
            String query, String siteUrl, int offset, int limit
    ) {
        Map<Lemma, WordFormMeanings> lemmas = getLemmasMap(query);
        Map<Page, List<Index>> indexes = getIndexesMap(lemmas, siteUrl);
        return indexes
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
                .map(this::getSearchResult)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    private Map<Lemma, WordFormMeanings> getLemmasMap(String query) {
        return LemmaProcessor
                .getLemmas(
                        Patterns.REMOVE_PUNCTUATION_MARKS
                                .getStringValue(query)
                )
                .stream()
                .flatMap(wfm -> lemmaRepository
                        .findByLemma(wfm.toString())
                        .stream()
                        .map(lemma ->
                                new AbstractMap.SimpleEntry<>(
                                        lemma,
                                        wfm
                                )
                        )
                )
                .collect(Collectors
                        .toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )
                );
    }

    private Map<Page, List<Index>> getIndexesMap(Map<Lemma, WordFormMeanings> lemmas,
                                                 String siteUrl) {
        return lemmas.keySet()
                .stream()
                .flatMap(lemma -> indexRepository
                        .findAllByLemmaOrderByRankDescLimit(
                                lemma,
                                Constants.MOST_RELEVANT_INDEXES_COUNT_LIMIT
                                        .getValue()
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
                .stream()
                .map(this::getTextFromElement)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(text -> new SnippetGenerator(text, pageLemmas))
                .filter(SnippetGenerator::isMeaningPositionsNotEmpty)
                .map(SnippetGenerator::getSnippet)
                .max(Snippet::compareTo)
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

    private Optional<String> getTextFromElement(Element element) {
        if (Patterns.HTML_TEXT_TAG_NAMES
                .isMatches(element.nodeName())) {
            return Optional.of(
                    element.text()
            );
        }
        return Optional.empty();
    }
}