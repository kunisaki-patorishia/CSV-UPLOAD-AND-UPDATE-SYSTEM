package com.csvuploader;

import com.csvuploader.model.Product;
import com.csvuploader.model.UploadedFile;
import com.csvuploader.repository.ProductRepository;
import com.csvuploader.repository.UploadedFileRepository;
import com.csvuploader.service.FileStorageService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class CsvProcessingService{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Async
    @Transactional
    public CompletableFuture<Void> processCsvFile(Long uploadedFileId, String filePath) {
        UploadedFile uploadedFile = uploadedFileRepository.findById(uploadedFileId)
                .orElseThrow(() -> new RuntimeException("Uploaded file not found"));

        try {
            uploadedFile.setStatus("processing");
            uploadedFileRepository.save(uploadedFile);

            // Clear existing products for this file (for idempotent retry)
            productRepository.deleteByUploadedFileId(uploadedFileId);

            int processedRows = processCsvRecords(filePath, uploadedFile);

            uploadedFile.setStatus("completed");
            uploadedFile.setProcessedRows(processedRows);
            uploadedFileRepository.save(uploadedFile);

            // Clean up file after processing
            fileStorageService.deleteFile(filePath);

        } catch (Exception e) {
            uploadedFile.setStatus("failed");
            uploadedFile.setErrorMessage(e.getMessage());
            uploadedFileRepository.save(uploadedFile);
        }

        return CompletableFuture.completedFuture(null);
    }

    private int processCsvRecords(String filePath, UploadedFile uploadedFile) throws IOException {
        int processedRows = 0;

        try (Reader reader = new FileReader(filePath, StandardCharsets.UTF_8);
                CSVParser csvParser = new CSVParser(reader,
                        CSVFormat.DEFAULT.builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreHeaderCase(true)
                                .setTrim(true)
                                .build())) {

            for (CSVRecord record : csvParser) {
                if (processCsvRecord(record, uploadedFile)) {
                    processedRows++;
                }
            }
        }

        return processedRows;
    }

    private boolean processCsvRecord(CSVRecord record, UploadedFile uploadedFile) {
        try {
            String uniqueKey = getCleanValue(record, "UNIQUE_KEY");
            if (uniqueKey == null || uniqueKey.trim().isEmpty()) {
                return false;
            }

            // UPSERT logic
            Product product = productRepository
                    .findByUniqueKeyAndUploadedFileId(uniqueKey, uploadedFile.getId())
                    .orElse(new Product());

            product.setUniqueKey(uniqueKey);
            product.setProductTitle(getCleanValue(record, "PRODUCT_TITLE"));
            product.setProductDescription(getCleanValue(record, "PRODUCT_DESCRIPTION"));
            product.setStyleNumber(getCleanValue(record, "STYLE#"));
            product.setSanmarMainframeColor(getCleanValue(record, "SANMAR_MAINFRAME_COLOR"));
            product.setSize(getCleanValue(record, "SIZE"));
            product.setColorName(getCleanValue(record, "COLOR_NAME"));
            product.setUploadedFile(uploadedFile);

            String priceStr = getCleanValue(record, "PIECE_PRICE");
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                try {
                    product.setPiecePrice(new BigDecimal(priceStr.trim().replace("$", "")));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid price format: " + priceStr);
                }
            }

            productRepository.save(product);
            return true;

        } catch (Exception e) {
            System.err.println("Error processing record: " + e.getMessage());
            return false;
        }
    }

    private String getCleanValue(CSVRecord record, String header) {
        try {
            String value = record.get(header);
            return cleanUtf8(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String cleanUtf8(String text) {
        if (text == null)
            return null;
        return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replaceAll("[^\\x00-\\x7F]", "")
                .trim();
    }
}
