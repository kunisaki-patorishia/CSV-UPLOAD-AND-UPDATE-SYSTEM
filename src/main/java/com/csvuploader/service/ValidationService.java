// src/main/java/com/csvuploader/service/ValidationService.java
package main.java.com.csvuploader.service;

import com.csvuploader.model.Product;
import com.csvuploader.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ValidationService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public Map<String, Object> validateUpload(Long uploadedFileId, List<String> expectedUniqueKeys) {
        Map<String, Object> result = new HashMap<>();
        
        // Get all products from this upload
        List<Product> uploadedProducts = productRepository.findByUploadedFileId(uploadedFileId);
        
        result.put("totalUploaded", uploadedProducts.size());
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
        
        // Sample some records to show changes
        if (!uploadedProducts.isEmpty()) {
            List<Map<String, Object>> sampleRecords = uploadedProducts.stream()
                    .limit(5)
                    .map(product -> Map.of(
                            "uniqueKey", product.getUniqueKey(),
                            "productTitle", product.getProductTitle(),
                            "piecePrice", product.getPiecePrice()
                    ))
                    .toList();
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
            result.put("latest", Map.of(
                    "productTitle", latest.getProductTitle(),
                    "piecePrice", latest.getPiecePrice(),
                    "uploadFile", latest.getUploadedFile().getFileName()
            ));
            result.put("previous", Map.of(
                    "productTitle", previous.getProductTitle(),
                    "piecePrice", previous.getPiecePrice(),
                    "uploadFile", previous.getUploadedFile().getFileName()
            ));
            result.put("changed", !latest.getPiecePrice().equals(previous.getPiecePrice()) ||
                                 !latest.getProductTitle().equals(previous.getProductTitle()));
        }
        
        return result;
    }
}