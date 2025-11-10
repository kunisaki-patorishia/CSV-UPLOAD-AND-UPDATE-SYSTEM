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
        
        // Sample some records to show changes - FIXED: Use explicit typing
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
        
        // Get all versions of this record
        List<Product> allVersions = productRepository.findByUniqueKeyOrderByUploadedFileId(uniqueKey);
        
        if (allVersions.size() > 1) {
            Product latest = allVersions.get(allVersions.size() - 1);
            Product previous = allVersions.get(allVersions.size() - 2);
            
            result.put("uniqueKey", uniqueKey);
            
            // FIXED: Use explicit HashMap instead of Map.of()
            Map<String, Object> latestMap = new HashMap<>();
            latestMap.put("productTitle", latest.getProductTitle());
            latestMap.put("piecePrice", latest.getPiecePrice());
            latestMap.put("uploadFile", latest.getUploadedFile().getFileName());
            result.put("latest", latestMap);
            
            Map<String, Object> previousMap = new HashMap<>();
            previousMap.put("productTitle", previous.getProductTitle());
            previousMap.put("piecePrice", previous.getPiecePrice());
            previousMap.put("uploadFile", previous.getUploadedFile().getFileName());
            result.put("previous", previousMap);
            
            result.put("changed", !latest.getPiecePrice().equals(previous.getPiecePrice()) ||
                                 !latest.getProductTitle().equals(previous.getProductTitle()));
        } else {
            result.put("message", "Only one version found for this key");
        }
        
        return result;
    }
}