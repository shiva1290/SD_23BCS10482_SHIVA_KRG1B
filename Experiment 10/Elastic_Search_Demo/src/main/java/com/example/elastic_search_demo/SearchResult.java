package com.example.elastic_search_demo;

import java.util.List;

public class SearchResult {
    private List<Product> products;
    private double timeMs;
    private int resultCount;
    private String searchType;
    public SearchResult() {}

    public SearchResult(List<Product> products, double timeMs, int resultCount, String searchType) {
        this.products = products;
        this.timeMs = timeMs;
        this.resultCount = resultCount;
        this.searchType = searchType;
    }

    public SearchResult(List<Product> products, double timeMs) {
        this.products = products;
        this.timeMs = timeMs;
        this.resultCount = products != null ? products.size() : 0;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        this.resultCount = products != null ? products.size() : 0;
    }

    public double getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(double timeMs) {
        this.timeMs = timeMs;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
}

