package com.example.elastic_search_demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/compare")
    public ResponseEntity<ComparisonResult> compare(@RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(searchService.compareSearches(query));
    }

    @GetMapping("/api/stats")
    public ResponseEntity<SearchService.Stats> stats() {
        return ResponseEntity.ok(searchService.getStats());
    }
}

