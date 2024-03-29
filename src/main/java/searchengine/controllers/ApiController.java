package searchengine.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.models.PageUrl;
import searchengine.models.statistics.StatisticsResponse;
import searchengine.enums.Messages;
import searchengine.services.IndexingService;

@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final IndexingService indexingService;

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        if (indexingService.startIndexing()) {
            return ResponseEntity
                    .ok(Messages.SUCCESS.getMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(Messages.FAILED_START.getMessage());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return ResponseEntity
                    .ok(Messages.SUCCESS.getMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(Messages.FAILED_STOP.getMessage());
    }

    @PostMapping(path = "/indexPage",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> indexPage(PageUrl page) {
        if (indexingService.indexPage(page.url())) {
            return ResponseEntity
                    .ok(Messages.SUCCESS.getMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(Messages.FAILED_PAGE_INDEX.getMessage());
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity
                .ok(indexingService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query,
                                    @RequestParam(required = false) String site,
                                    @RequestParam int offset,
                                    @RequestParam int limit) {
        if (query.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(Messages.FAILED_SEARCH.getMessage());
        }
        return ResponseEntity.ok(indexingService.getSearchResults(query, site, offset, limit));
    }
}
