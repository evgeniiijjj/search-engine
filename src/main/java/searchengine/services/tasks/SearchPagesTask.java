package searchengine.services.tasks;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Constants;
import searchengine.enums.Patterns;
import searchengine.enums.Statuses;
import searchengine.services.IndexingManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@AllArgsConstructor
@Slf4j
public class SearchPagesTask implements Runnable {

    private final IndexingManager indexingManager;
    private Page page;

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
            indexingManager.addPage(page);
            findSubpages(document);
            updateSiteStatus(Statuses.INDEXING, null);
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
            if (isAcceptablePage(path, newPage)) {
                indexingManager.startSearchPagesTask(newPage);
            }
        }
    }

    private boolean isAcceptablePage(String root, Page page) {
        String path = page.getPath();
        return !indexingManager.isPagePresent(page) &&
                path.startsWith(root) &&
                path.length() != root.length() &&
                !Patterns.NOT_RELEVANT_PAGE_PATH.isMatches(path);
    }
}
