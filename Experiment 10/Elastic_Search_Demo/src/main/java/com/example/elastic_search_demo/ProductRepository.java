package com.example.elastic_search_demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query(value = "SELECT * FROM products p WHERE p.name ILIKE %:query% OR p.description ILIKE %:query% OR p.category ILIKE %:query% OR p.brand ILIKE %:query% ORDER BY p.timestamp DESC LIMIT 50", nativeQuery = true)
    List<Product> searchTop50(@Param("query") String query);

}
