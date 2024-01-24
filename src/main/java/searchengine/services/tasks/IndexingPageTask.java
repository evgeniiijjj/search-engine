package searchengine.services.tasks;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.entities.Index;
import searchengine.entities.Lemma;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Constants;
import searchengine.enums.Patterns;
import searchengine.enums.Statuses;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingManager;
import searchengine.services.utils.LemmaProcessor;
import searchengine.services.utils.WordFormMeanings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class IndexingPageTask implements IndexingTask {

    private final IndexingManager indexingManager;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private Page page;

    @Override
    public IndexingTask setPage(Page page) {
        this.page = page;
        return this;
    }

    @Override
    public void run() {
        Site site = page.getSite();
        String url = site.getUrl();
        String path = page.getPath();
        try {
            Thread.sleep(Constants.TIMEOUT_150_MS.getValue());
            Connection.Response response = Jsoup
                    .connect(url + path)
                    .execute();
            page.setCode(response.statusCode());
            Document document = response.parse();
            page.setContent(document.html());
            try {
                page = pageRepository.save(page);
            } catch (DataIntegrityViolationException e) {
                log.info(e.getMessage());
                return;
            }
            findSubpages(document);
            updateSiteStatus(Statuses.INDEXING, null);
            indexingPage();
        } catch (Exception e) {
            String errorMessage = url + path + " - " + e.getMessage();
            updateSiteStatus(Statuses.FAILED, errorMessage);
            log.warn(errorMessage);
        }
    }

    private void updateSiteStatus(Statuses status, String lastError) {
        Site site = page.getSite();
        site.setStatus(status);
        site.setStatusTime(
                Instant.now()
                        .truncatedTo(ChronoUnit.SECONDS)
        );
        site.setLastError(lastError);
        siteRepository.save(site);
    }

    private void findSubpages(Document document) {
        Site site = page.getSite();
        String path = page.getPath();
        Elements elements = document.getElementsByTag(
                Patterns.HTML_TAG_A.getStringValue()
        );
        for (Element element : elements) {
            var newPath = element.attr(
                    Patterns.HTML_TAG_ATTRIBUTE_HREF.getStringValue()
            );
            Page newPage = new Page(site, newPath);
            if (isNotAcceptablePage(path, newPage)) {
                continue;
            }
            indexingManager.startIndexingPageTask(newPage);
        }
    }

    private boolean isNotAcceptablePage(String root, Page page) {
        String path = page.getPath();
        return pageRepository.existsBySiteAndPath(page.getSite(), path) ||
                !path.startsWith(root) ||
                path.length() == root.length() ||
                Patterns.NOT_RELEVANT_PAGE_PATH.isMatches(path);
    }

    private void indexingPage() {
        String text = Jsoup.parse(page.getContent()).body().text();
        Map<Lemma, Integer> lemmas =
                LemmaProcessor
                        .getLemmas(text)
                        .stream()
                        .map(WordFormMeanings::toString)
                        .map(this::saveLemma)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(
                                Collectors.toMap(
                                        lemma -> lemma,
                                        lemma -> 1,
                                        Integer::sum
                                )
                        )
                        .entrySet()
                        .stream()
                        .collect(Collectors
                                .toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue
                                )
                        );
        saveIndexes(lemmas);
    }

    private Optional<Lemma> saveLemma(String lemmaStr) {
        Lemma lemma = new Lemma(page.getSite(), lemmaStr);
        lemmaRepository.insertOrUpdateLemma(lemma);
        return lemmaRepository
                .findBySiteAndLemma(lemma.getSite(), lemma.getLemma());
    }

    private void saveIndexes(Map<Lemma, Integer> lemmas) {
        indexRepository.saveAllAndFlush(
                lemmas
                        .entrySet()
                        .stream()
                        .map(entry ->
                                new Index(
                                        page,
                                        entry.getKey(),
                                        (float) entry.getValue() / lemmas.size()
                                )
                        )
                        .toList()
        );
        updateSiteStatus(Statuses.INDEXED, null);
    }
}
