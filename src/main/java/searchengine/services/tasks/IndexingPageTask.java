package searchengine.services.tasks;

import com.github.demidko.aot.WordformMeaning;
import lombok.AllArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.entities.Index;
import searchengine.entities.Lemma;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Constants;
import searchengine.enums.Patterns;
import searchengine.enums.Statuses;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingManager;
import searchengine.services.utils.LemmaProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@AllArgsConstructor
public class IndexingPageTask implements IndexingTask {

    private final IndexingManager indexingManager;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private Page page;

    @Override
    public IndexingTask setPage(Page page) {
        this.page = page;
        return this;
    }

    @Override
    public void run() {

        assert page != null;
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
            pageRepository.insertPage(page);
            page = pageRepository
                    .findBySiteAndPath(site, path).orElse(page);
            findSubpages(document);
            updateSiteStatus(Statuses.INDEXING, null);
            indexingPage();
        } catch (Exception e) {
            updateSiteStatus(Statuses.FAILED, e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateSiteStatus(Statuses status, String lastError) {

        assert page != null;
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

        assert page != null;
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
                path.contains("?") ||
                path.contains(":");
    }

    private void indexingPage() {

        assert page != null;
        String text = Jsoup.parse(page.getContent()).body().text();
        Map<Lemma, Integer> lemmaMap =
                LemmaProcessor
                        .getRussianLemmas(text)
                        .stream()
                        .map(WordformMeaning::toString)
                        .map(lemmaStr -> lemmaRepository
                                .findByLemma(lemmaStr)
                                .orElseGet(() -> {
                                    Lemma lemma = new Lemma(lemmaStr);
                                    lemmaRepository
                                        .insertLemma(lemma);
                                    return lemmaRepository
                                            .findByLemma(lemmaStr)
                                            .orElse(lemma);
                                })
                        )
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
        indexingManager.putIndexes(page, getIndexes(lemmaMap));
        updateSiteStatus(Statuses.INDEXING, null);
    }

    private Set<Index> getIndexes(Map<Lemma, Integer> lemmas) {

        assert page != null;
        return lemmas
                .entrySet()
                .stream()
                .map(entry ->
                        new Index(
                                page,
                                entry.getKey(),
                                (float) entry.getValue() / lemmas.size()
                        )
                )
                .collect(Collectors.toSet());
    }
}
