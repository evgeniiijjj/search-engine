package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import searchengine.config.SiteList;
import searchengine.entities.Index;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Constants;
import searchengine.enums.Messages;
import searchengine.enums.Patterns;
import searchengine.enums.Statuses;
import searchengine.models.Meaning;
import searchengine.models.MeaningPositions;
import searchengine.models.SearchResult;
import searchengine.models.SearchResults;
import searchengine.models.Snippet;
import searchengine.models.statistics.DetailedStatisticsItem;
import searchengine.models.statistics.StatisticsData;
import searchengine.models.statistics.StatisticsResponse;
import searchengine.models.statistics.TotalStatistics;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.utils.LemmaProcessor;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SiteList list;
    private final IndexingManager indexingManager;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    @Nullable
    private String query;
    @Nullable
    private String siteUrl;
    @Nullable
    private SearchResults searchResults;

    @Override
    public boolean startIndexing() {
        if (indexingManager.isIndexing()) {
            return false;
        }
        indexingManager.setSites(
                list.getSites().stream()
                        .map(this::setStatusSite)
                        .collect(Collectors.toSet())
        );
        indexingManager.startIndexing();
        return true;
    }

    private Site setStatusSite(Site site) {
        return updateStatusSite(site, Statuses.INDEXING, null);
    }

    private Site updateStatusSite(Site site,
                                  Statuses status,
                                  String lastError) {
        siteRepository.findByUrl(site.getUrl())
                .ifPresent(this::deleteOldIndexes);
        site.setStatus(status);
        site.setStatusTime(
                Instant.now()
                        .truncatedTo(ChronoUnit.SECONDS)
        );
        site.setLastError(lastError);
        site = siteRepository.save(site);
        return site;
    }

    private void deleteOldIndexes(Site site) {
        indexRepository.deleteOldIndexesBySite(site);
        pageRepository.deleteAllBySite(site);
        siteRepository.delete(site);
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
        indexingManager.startSearchPagesTask(page);
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
        pageRepository.delete(page);
    }

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics totalStatistics = new TotalStatistics(
                (int) siteRepository.count(),
                (int) pageRepository.count(),
                (int) indexRepository.countDistinctLemma(),
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
                                (int) pageRepository.countBySite(site),
                                (int) indexRepository.lemmaCountBySite(site)
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
    public SearchResults getSearchResults(
            String query, String siteUrl, int offset, int limit
    ) {
        if (!Objects.equals(this.query, query) ||
                !Objects.equals(this.siteUrl, siteUrl) ||
                searchResults == null) {
            this.query = query;
            this.siteUrl = siteUrl;
            searchResults = new SearchResults(getSearchResults(query, siteUrl));
        }
        return searchResults.getPagedResults(offset, limit);
    }

    public List<SearchResult> getSearchResults(String query, String siteUrl) {
        List<Meaning> words = LemmaProcessor.getLemmas(query, true);
        return getPages(query, siteUrl)
                .map(page -> getSearchResult(page, words))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .toList();
    }

    private Stream<Page> getPages(String query, String siteUrl) {
        return LemmaProcessor
                .getLemmas(
                        Patterns.REMOVE_PUNCTUATION_MARKS
                                .getStringValue(query),
                        false
                )
                .stream()
                .flatMap(lemma -> siteRepository.findByUrl(siteUrl)
                        .map(site ->
                                indexRepository.findByLemmaAndSiteOrderByRankDescLimit(
                                        lemma.word(),
                                        site,
                                        Constants.MOST_RELEVANT_INDEXES_COUNT_LIMIT
                                                .getValue()
                                )
                        )
                        .orElse(
                                indexRepository.findByLemmaOrderByRankDescLimit(
                                        lemma.word(),
                                        Constants.MOST_RELEVANT_INDEXES_COUNT_LIMIT
                                                .getValue()
                                )
                        )
                        .stream()
                )
                .map(Index::getPage)
                .distinct();
    }

    private Optional<SearchResult> getSearchResult(Page page, List<Meaning> words) {
        Document document = Jsoup.parse(page.getContent());
        return document.body()
                .getAllElements()
                .stream()
                .map(this::getTextFromElement)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(text -> getSnippet(text, words))
                .filter(Snippet::isNotBlank)
                .max(Snippet::compareTo)
                .map(snippet ->
                        new SearchResult(
                                page.getSite().getUrl(),
                                page.getSite().getName(),
                                page.getPath(),
                                document.title(),
                                snippet.toString(),
                                (float) snippet.getWordMeaningsVarietyCount() / words.size() +
                                        (float) snippet.getWordMeaningCount() / words.size() / 100
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

    private Snippet getSnippet(String text, List<Meaning> words) {
        Snippet snippet = getPositions(text, words);
        int prevPos = 0;
        StringBuilder stringSnippetBuilder = new StringBuilder();
        List<MeaningPositions> positions = snippet.getPositions();
        for (int i = 0; i < positions.size(); i++) {
            stringSnippetBuilder.append(
                    modifyString(
                            text,
                            prevPos,
                            positions.get(i),
                            i == positions.size() - 1
                    )
            );
            prevPos = positions.get(i).end();
        }
        snippet.setSnippet(stringSnippetBuilder.toString());
        return snippet;
    }

    private Snippet getPositions(String text, List<Meaning> words) {
        Set<MeaningPositions> positions = new HashSet<>();
        words
                .stream()
                .flatMap(meaning -> Patterns.SAMPLE.getRedexPattern(meaning.word())
                        .matcher(text.toLowerCase())
                        .results()
                        .map(matchResult ->
                                new MeaningPositions(
                                        meaning.word(),
                                        matchResult.start(),
                                        matchResult.end(),
                                        meaning.stopWord()
                                )
                        )
                )
                .sorted(MeaningPositions::compareTo)
                .forEach(positions::add);
        List<MeaningPositions> list = positions.stream()
                .sorted(MeaningPositions::compareTo)
                .toList();
        Set<String> strings = new HashSet<>();
        List<MeaningPositions> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).stopWord() || i > 0  &&
                    !list.get(i - 1).stopWord() && list.get(i).start() - list.get(i - 1).end() == 1 ||
                    i < list.size() - 1 &&
                    !list.get(i + 1).stopWord() && list.get(i + 1).start() - list.get(i).end() == 1) {
                result.add(list.get(i));
                strings.add(list.get(i).word());
            }
        }
        return new Snippet(
                "",
                result,
                strings.size()
        );
    }

    private String modifyString(String text, int prevPos,
                                MeaningPositions meaningPositions, boolean isEndPart) {
        String highlightedPart = Patterns.HIGHLIGHTED_STRING_PART
                .getStringValue(text.substring(meaningPositions.start(), meaningPositions.end()));
        String beginPart = "";
        String endPart = "";
        if (prevPos == 0 && meaningPositions.start() > 0) {
            beginPart = Patterns.FIRST_STRING_PART
                    .getStringValue(text.substring(prevPos, meaningPositions.start()));
        }
        if (prevPos > 0) {
            beginPart = Patterns.MIDDLE_STRING_PART
                    .getStringValue(text.substring(prevPos, meaningPositions.start()));
        }
        if (isEndPart && meaningPositions.end() < text.length()) {
            endPart = Patterns.LAST_STRING_PART
                    .getStringValue(text.substring(meaningPositions.end()));
        }
        return beginPart.concat(highlightedPart).concat(endPart);
    }
}