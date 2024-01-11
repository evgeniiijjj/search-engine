package searchengine.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.PageDto;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.enums.Messages;
import searchengine.services.Service;


@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final Service service;

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {

        if (service.startIndexing()) {
            return ResponseEntity
                    .ok(Messages.SUCCESS.getMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(Messages.FAILED_START.getMessage());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {

        if (service.stopIndexing()) {
            return ResponseEntity
                    .ok(Messages.SUCCESS.getMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(Messages.FAILED_STOP.getMessage());
    }

    @PostMapping(path = "/indexPage",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> indexPage(PageDto page) {

        if (service.indexPage(page.url())) {

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
                .ok(service.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query,
                                    @RequestParam(required = false) String site,
                                    @RequestParam int offset,
                                    @RequestParam int limit) {

        return service.getSearchResults(query, site, offset, limit)
                .map(results -> ResponseEntity
                .ok(Messages.SUCCESS_SEARCH
                        .getMessage(results)))
                .orElseGet(() -> ResponseEntity
                        .badRequest()
                        .body(Messages.FAILED_SEARCH.getMessage()));
    }
}
