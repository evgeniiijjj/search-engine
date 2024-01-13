package searchengine.services.tasks;

import lombok.AllArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.PatternsAndConstants;
import searchengine.enums.Statuses;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


@AllArgsConstructor
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FindPagesTask implements IndexingTask {

    private final IndexingManager indexingManager;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Nullable
    private Page page;
    @Nullable
    private Boolean findSubpages;

    @Override
    public IndexingTask setPage(Page page) {

        this.page = page;
        indexingManager.addIndexedPage(page);
        findSubpages = true;
        return this;
    }

    @Override
    public IndexingTask withoutFindSubpages() {

        findSubpages = false;
        return this;
    }

    @Override
    public void run() {

        assert page != null;
        Site site = page.getSite();
        String url = site.getUrl();
        String path = page.getPath();

        try {
            Thread.sleep(150);
            Connection.Response response = Jsoup
                    .connect(url + path)
                    .execute();

            page.setCode(response.statusCode());

            Document document = response.parse();

            page.setContent(document.html());

            pageRepository.insertOrUpdate(page);

            pageRepository
                    .findBySiteAndPath(site, path)
                    .ifPresent(value -> page = value);

            updateSiteStatus(Statuses.INDEXING, null);

            if (Boolean.TRUE.equals(findSubpages)) {

                findSubpages(document);
            }

            indexingManager.startIndexingPageTask(page);

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
                PatternsAndConstants.HTML_TAG_A.getStringValue()
        );

        for (Element element : elements) {

            var newPath = element.attr(
                    PatternsAndConstants.HTML_TAG_ATTRIBUTE_HREF.getStringValue()
            );

            Page newPage = new Page(site, newPath);

            if (isNotAcceptablePage(path, newPage)) {

                continue;
            }

            indexingManager.startFindPageTask(newPage);
        }
    }

    private boolean isNotAcceptablePage(String root, Page page) {

        String path = page.getPath();

        return indexingManager.isPageIndexed(page) ||
                !path.startsWith(root) ||
                path.length() == root.length() ||
                path.indexOf('#') >= 0 ||
                path.indexOf('?') >= 0;
    }
}
