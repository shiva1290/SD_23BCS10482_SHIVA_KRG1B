package com.example.elastic_search_demo;

public class ComparisonResult {
    private SearchResult normalSearch;
    private SearchResult elasticSearch;
    private String query;
    private long totalDataPoints;
    private double speedupFactor;

    public ComparisonResult() {}

    public ComparisonResult(SearchResult normalSearch, SearchResult elasticSearch, String query, long totalDataPoints, double speedupFactor) {
        this.normalSearch = normalSearch;
        this.elasticSearch = elasticSearch;
        this.query = query;
        this.totalDataPoints = totalDataPoints;
        this.speedupFactor = speedupFactor;
    }
    public void calculateSpeedup() {
        if (normalSearch != null && elasticSearch != null && elasticSearch.getTimeMs() > 0) {
            this.speedupFactor = normalSearch.getTimeMs() / elasticSearch.getTimeMs();
        }
    }

    public SearchResult getNormalSearch() {
        return normalSearch;
    }

    public void setNormalSearch(SearchResult normalSearch) {
        this.normalSearch = normalSearch;
    }

    public SearchResult getElasticSearch() {
        return elasticSearch;
    }

    public void setElasticSearch(SearchResult elasticSearch) {
        this.elasticSearch = elasticSearch;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getTotalDataPoints() {
        return totalDataPoints;
    }

    public void setTotalDataPoints(long totalDataPoints) {
        this.totalDataPoints = totalDataPoints;
    }

    public double getSpeedupFactor() {
        return speedupFactor;
    }

    public void setSpeedupFactor(double speedupFactor) {
        this.speedupFactor = speedupFactor;
    }
}

