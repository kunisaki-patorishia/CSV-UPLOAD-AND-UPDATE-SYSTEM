// src/main/java/com/csvuploader/service/ValidationService.java
package com.csvuploader.service;

import com.csvuploader.model.Product;
import com.csvuploader.model.UploadedFile;
import com.csvuploader.repository.ProductRepository;
import com.csvuploader.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;

@Service
public class ValidationService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    public Map<String, Object> validateUpload(Long uploadedFileId, List<String> expectedUniqueKeys) {
        Map<String, Object> result = new HashMap<>();

        // Get all products from this upload
        List<Product> uploadedProducts = productRepository.findByUploadedFileId(uploadedFileId);

        result.put("totalUploaded", uploadedProducts.size());

        if (expectedUniqueKeys != null) {
            result.put("expectedKeys", expectedUniqueKeys.size());

            // Check which expected keys were found
            List<String> foundKeys = uploadedProducts.stream()
                    .map(Product::getUniqueKey)
                    .toList();

            List<String> missingKeys = expectedUniqueKeys.stream()
                    .filter(key -> !foundKeys.contains(key))
                    .toList();

            result.put("foundKeys", foundKeys);
            result.put("missingKeys", missingKeys);
        }

        // Sample some records to show changes
        if (!uploadedProducts.isEmpty()) {
            List<Map<String, Object>> sampleRecords = new ArrayList<>();
            uploadedProducts.stream()
                    .limit(5)
                    .forEach(product -> {
                        Map<String, Object> record = new HashMap<>();
                        record.put("uniqueKey", product.getUniqueKey());
                        record.put("productTitle", product.getProductTitle());
                        record.put("piecePrice", product.getPiecePrice());
                        sampleRecords.add(record);
                    });
            result.put("sampleRecords", sampleRecords);
        }

        return result;
    }

    public Map<String, Object> compareRecords(String uniqueKey) {
        Map<String, Object> result = new HashMap<>();

        // FIXED: Use findByUniqueKey and sort manually since the specific method
        // doesn't exist
        List<Product> allProducts = productRepository.findByUniqueKey(uniqueKey)
                .stream()
                .toList();

        if (allProducts.isEmpty()) {
            result.put("message", "No records found for key: " + uniqueKey);
            return result;
        }

        // Sort by uploaded file creation date manually
        List<Product> sortedProducts = allProducts.stream()
                .sorted(Comparator.comparing(p -> p.getUploadedFile().getCreatedAt()))
                .toList();

        if (sortedProducts.size() > 1) {
            Product latest = sortedProducts.get(sortedProducts.size() - 1);
            Product previous = sortedProducts.get(sortedProducts.size() - 2);

            result.put("uniqueKey", uniqueKey);

            Map<String, Object> latestMap = new HashMap<>();
            latestMap.put("productTitle", latest.getProductTitle());
            latestMap.put("piecePrice", latest.getPiecePrice());
            latestMap.put("uploadFile", latest.getUploadedFile().getFileName());
            latestMap.put("fileId", latest.getUploadedFile().getId());
            result.put("latest", latestMap);

            Map<String, Object> previousMap = new HashMap<>();
            previousMap.put("productTitle", previous.getProductTitle());
            previousMap.put("piecePrice", previous.getPiecePrice());
            previousMap.put("uploadFile", previous.getUploadedFile().getFileName());
            previousMap.put("fileId", previous.getUploadedFile().getId());
            result.put("previous", previousMap);

            result.put("changed", !latest.getPiecePrice().equals(previous.getPiecePrice()) ||
                    !latest.getProductTitle().equals(previous.getProductTitle()));

            // Show all versions
            List<Map<String, Object>> allVersions = new ArrayList<>();
            for (Product product : sortedProducts) {
                Map<String, Object> version = new HashMap<>();
                version.put("productTitle", product.getProductTitle());
                version.put("piecePrice", product.getPiecePrice());
                version.put("uploadFile", product.getUploadedFile().getFileName());
                version.put("fileId", product.getUploadedFile().getId());
                version.put("uploadedAt", product.getUploadedFile().getCreatedAt());
                allVersions.add(version);
            }
            result.put("allVersions", allVersions);

        } else {
            result.put("message", "Only one version found for this key");
            Map<String, Object> onlyVersion = new HashMap<>();
            Product product = sortedProducts.get(0);
            onlyVersion.put("productTitle", product.getProductTitle());
            onlyVersion.put("piecePrice", product.getPiecePrice());
            onlyVersion.put("uploadFile", product.getUploadedFile().getFileName());
            onlyVersion.put("fileId", product.getUploadedFile().getId());
            result.put("onlyVersion", onlyVersion);
        }

        return result;
    }
}