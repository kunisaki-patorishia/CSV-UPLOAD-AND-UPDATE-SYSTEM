package com.csvuploader.service;

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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class CSVProcessingService {

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

            System.out.println("=== PROCESSING COMPLETED ===");
            System.out.println("File: " + uploadedFile.getFileName());
            System.out.println("Processed rows: " + processedRows);

        } catch (Exception e) {
            uploadedFile.setStatus("failed");
            uploadedFile.setErrorMessage(e.getMessage());
            uploadedFileRepository.save(uploadedFile);
            System.err.println("=== PROCESSING FAILED ===");
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(null);
    }

    private int processCsvRecords(String filePath, UploadedFile uploadedFile) throws IOException {
        int processedRows = 0;

        try (Reader reader = new FileReader(filePath, StandardCharsets.UTF_8);
                CSVParser csvParser = new CSVParser(reader,
                        CSVFormat.DEFAULT.builder()
                                .setDelimiter('\t') // ADDED: Handle TAB-delimited files
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreHeaderCase(true)
                                .setTrim(true)
                                .build())) {

            System.out.println("=== PROCESSING STARTED ===");
            System.out.println("File: " + uploadedFile.getFileName());
            System.out.println("File ID: " + uploadedFile.getId());
            System.out.println("Headers: " + csvParser.getHeaderMap().keySet());

            int recordCount = 0;
            for (CSVRecord record : csvParser) {
                recordCount++;
                if (processCsvRecord(record, uploadedFile, recordCount)) {
                    processedRows++;
                }

                // Log progress every 1000 records
                if (recordCount % 1000 == 0) {
                    System.out.println("Processed " + recordCount + " records...");
                }
            }

            System.out.println("Total records in file: " + recordCount);
            System.out.println("Successfully processed: " + processedRows);

        } catch (Exception e) {
            System.err.println("Error processing CSV records: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return processedRows;
    }

    private boolean processCsvRecord(CSVRecord record, UploadedFile uploadedFile, int recordNumber) {
        try {
            String uniqueKey = getCleanValue(record, "UNIQUE_KEY");
            if (uniqueKey == null || uniqueKey.trim().isEmpty()) {
                System.out.println("Record " + recordNumber + ": Skipped - empty UNIQUE_KEY");
                return false;
            }

            // BEST UPSERT LOGIC: Find by UNIQUE_KEY across ALL files
            Optional<Product> existingProduct = productRepository.findByUniqueKey(uniqueKey);

            Product product;
            String operation;

            if (existingProduct.isPresent()) {
                product = existingProduct.get();
                operation = "UPDATED";
                System.out.println("Record " + recordNumber + ": UPDATING existing product - " + uniqueKey);
            } else {
                product = new Product();
                operation = "INSERTED";
                System.out.println("Record " + recordNumber + ": INSERTING new product - " + uniqueKey);
            }

            // Update all fields
            product.setUniqueKey(uniqueKey);
            product.setProductTitle(getCleanValue(record, "PRODUCT_TITLE"));
            product.setProductDescription(getCleanValue(record, "PRODUCT_DESCRIPTION"));
            product.setStyleNumber(getCleanValue(record, "STYLE#"));
            product.setSanmarMainframeColor(getCleanValue(record, "SANMAR_MAINFRAME_COLOR"));
            product.setSize(getCleanValue(record, "SIZE"));
            product.setColorName(getCleanValue(record, "COLOR_NAME"));
            product.setUploadedFile(uploadedFile); // Link to current file

            // Handle price
            String priceStr = getCleanValue(record, "PIECE_PRICE");
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                try {
                    product.setPiecePrice(new BigDecimal(priceStr.trim().replace("$", "")));
                } catch (NumberFormatException e) {
                    System.err.println("Record " + recordNumber + ": Invalid price format - " + priceStr);
                }
            }

            productRepository.save(product);
            System.out.println("Record " + recordNumber + ": " + operation + " - " + uniqueKey);
            return true;

        } catch (Exception e) {
            System.err.println("Record " + recordNumber + ": Error - " + e.getMessage());
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