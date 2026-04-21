package com.example.elastic_search_demo;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductRepository productRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public SearchService(ElasticsearchOperations elasticsearchOperations, ProductRepository productRepository, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.productRepository = productRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final int PRODUCT_COUNT = 2_000_000;
    private static final String[] CATEGORIES = {"Electronics", "Cellphones", "Computers", "Televisions", "Audio", "Cameras", "Wearables", "Gaming"};
    private static final String[] BRANDS = {"Apple", "Sony", "Samsung", "Nike", "Microsoft", "LG", "Adidas", "Lenovo", "Nvidia", "Panasonic"};
    private static final String[] KEYWORDS = {"Premium", "Wireless", "Smart", "4K", "Pro", "Portable", "Digital", "Gaming", "HD", "Ultimate"};

    private final AtomicInteger esIndexedCount = new AtomicInteger(0);
    private final AtomicBoolean esIndexing = new AtomicBoolean(false);
    private final AtomicBoolean esReady = new AtomicBoolean(false);

    public record Stats(long totalProducts, boolean elasticsearchIndexing, int elasticsearchIndexed, boolean elasticsearchReady) {}

    @EventListener(ApplicationReadyEvent.class)
    public void initAsync() {
        Thread worker = new Thread(() -> {
            try {
                if (productRepository.count() < PRODUCT_COUNT) {
                    System.out.println("PostgreSQL database is empty. Generating and pushing " + PRODUCT_COUNT + " products...");
                    rebuildDataSources();
                } else {
                    System.out.println("PostgreSQL database already seeded with 2M products.");
                    esIndexedCount.set(PRODUCT_COUNT);
                    esReady.set(true);
                }
            } catch (Exception e) {
                System.err.println("Initialization error: " + e.getMessage());
            }
        }, "db-seeder");
        worker.setDaemon(true);
        worker.start();
    }

    private void rebuildDataSources() {
        esIndexing.set(true);
        esReady.set(false);
        esIndexedCount.set(0);

        try {
            var ops = elasticsearchOperations.indexOps(Product.class);
            try { ops.delete(); } catch (Exception ignored) {}
            ops.createWithMapping();

            Random random = new Random(42);
            int batchSize = 10_000;
            String sql = "INSERT INTO products (id, name, description, category, brand, price, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            for (int i = 0; i < PRODUCT_COUNT; i += batchSize) {
                List<Product> batch = new ArrayList<>(batchSize);
                List<Object[]> jdbcBatch = new ArrayList<>(batchSize);
                
                int end = Math.min(i + batchSize, PRODUCT_COUNT);
                for (int j = i; j < end; j++) {
                    String keyword = KEYWORDS[random.nextInt(KEYWORDS.length)];
                    String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
                    String brand = BRANDS[random.nextInt(BRANDS.length)];
                    String id = "prod-" + j;
                    String name = brand + " " + keyword + " " + category + " " + j;
                    String description = "High quality " + keyword.toLowerCase() + " " + category.toLowerCase() + " from " + brand + ". Features advanced design and much more. ID: " + j;
                    double price = Math.round(random.nextDouble() * 1000 * 100.0) / 100.0;
                    long timestamp = System.currentTimeMillis() - (Math.abs(random.nextLong()) % (365L * 24 * 60 * 60 * 1000));

                    Product product = new Product(id, name, description, category, brand, price, timestamp);
                    batch.add(product);
                    
                    jdbcBatch.add(new Object[] {id, name, description, category, brand, price, timestamp});
                }
                
                jdbcTemplate.batchUpdate(sql, jdbcBatch);
                elasticsearchOperations.save(batch);
                
                esIndexedCount.set(end);
                if (end % 100_000 == 0) {
                    System.out.println("Indexed " + end + " out of " + PRODUCT_COUNT);
                }
            }

            elasticsearchOperations.indexOps(IndexCoordinates.of("products")).refresh();
            esReady.set(true);
            System.out.println("Database and Elasticsearch fully synced with " + PRODUCT_COUNT + " products.");
        } catch (Exception e) {
            System.err.println("Synchronization failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            esIndexing.set(false);
        }
    }

    public SearchResult normalSearch(String query) {
        if (query == null || query.isBlank()) {
            SearchResult empty = new SearchResult(List.of(), 0.0);
            empty.setSearchType("PostgreSQL DB");
            return empty;
        }

        long start = System.nanoTime();
        List<Product> results = productRepository.searchTop50(query);
        long end = System.nanoTime();

        SearchResult result = new SearchResult(results, (end - start) / 1_000_000.0);
        result.setSearchType("PostgreSQL DB");
        return result;
    }

    public SearchResult elasticSearch(String query) {
        if (query == null || query.isBlank()) {
            SearchResult empty = new SearchResult(List.of(), 0.0);
            empty.setSearchType("Elasticsearch");
            return empty;
        }

        if (!esReady.get()) {
            SearchResult notReady = new SearchResult(new ArrayList<>(), 0.0);
            notReady.setSearchType(esIndexing.get() ? "Elasticsearch (indexing…)" : "Elasticsearch (not ready)");
            notReady.setResultCount(0);
            return notReady;
        }

        long start = System.nanoTime();
        try {
            String escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"");
            String[] words = escapedQuery.split("\\s+");
            StringBuilder wildcardQuery = new StringBuilder();
            for (String w : words) {
                wildcardQuery.append("*").append(w).append("* ");
            }
            
            String queryJson = String.format(
                "{\"query_string\": {\"query\": \"%s\", \"fields\": [\"name\", \"description\", \"category\", \"brand\"]}}",
                wildcardQuery.toString().trim()
            );
            Query esQuery = new StringQuery(queryJson);
            ((org.springframework.data.elasticsearch.core.query.BaseQuery) esQuery).setMaxResults(50);

            SearchHits<Product> hits = elasticsearchOperations.search(esQuery, Product.class);
            long end = System.nanoTime();

            List<Product> results = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .toList();

            SearchResult result = new SearchResult(results, (end - start) / 1_000_000.0);
            result.setSearchType("Elasticsearch");
            return result;
        } catch (Exception e) {
            long end = System.nanoTime();
            System.err.println("Elasticsearch query failed: " + e.getMessage());
            SearchResult errResult = new SearchResult(new ArrayList<>(), (end - start) / 1_000_000.0);
            errResult.setSearchType("Elasticsearch (error)");
            errResult.setResultCount(0);
            return errResult;
        }
    }

    public ComparisonResult compareSearches(String query) {
        SearchResult normal = normalSearch(query);
        SearchResult elastic = elasticSearch(query);
        ComparisonResult comparison = new ComparisonResult();
        comparison.setNormalSearch(normal);
        comparison.setElasticSearch(elastic);
        comparison.setQuery(query);
        comparison.setTotalDataPoints(PRODUCT_COUNT);
        comparison.calculateSpeedup();
        return comparison;
    }

    public Stats getStats() {
        return new Stats(PRODUCT_COUNT, esIndexing.get(), esIndexedCount.get(), esReady.get());
    }
}

