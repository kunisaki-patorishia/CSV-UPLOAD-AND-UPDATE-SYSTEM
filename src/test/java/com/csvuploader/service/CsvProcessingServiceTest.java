package com.csvuploader.service;

import com.csvuploader.model.UploadedFile;
import com.csvuploader.repository.ProductRepository;
import com.csvuploader.repository.UploadedFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CsvProcessingServiceTest {

    @Autowired
    private CSVProcessingService csvProcessingService;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void testUpsertLogic() throws Exception {
        // Create test CSV content
        String initialCsv = "UNIQUE_KEY,PRODUCT_TITLE,PIECE_PRICE\n" +
                "KEY1,Initial Product,10.00\n" +
                "KEY2,Another Product,20.00\n";

        String updateCsv = "UNIQUE_KEY,PRODUCT_TITLE,PIECE_PRICE\n" +
                "KEY1,Updated Product,15.00\n" + // Update existing
                "KEY3,New Product,25.00\n"; // Add new

        // Process initial file
        UploadedFile initialFile = new UploadedFile("initial.csv", "hash1");
        initialFile = uploadedFileRepository.save(initialFile);

        Path tempFile = Files.createTempFile("test", ".csv");
        Files.write(tempFile, initialCsv.getBytes());

        CSVProcessingService.processCsvFile(initialFile.getId(), tempFile.toString()); // ← This should work now

        // Process update file
        UploadedFile updateFile = new UploadedFile("update.csv", "hash2");
        updateFile = uploadedFileRepository.save(updateFile);

        Path tempFile2 = Files.createTempFile("test2", ".csv");
        Files.write(tempFile2, updateCsv.getBytes());

        CSVProcessingService.processCsvFile(updateFile.getId(), tempFile2.toString());

        // Verify results
        assertEquals(3, productRepository.count()); // KEY1, KEY2, KEY3

        var updatedProduct = productRepository.findByUniqueKeyAndUploadedFileId("KEY1", updateFile.getId());
        assertTrue(updatedProduct.isPresent());
        assertEquals("Updated Product", updatedProduct.get().getProductTitle());
        assertEquals(15.00, updatedProduct.get().getPiecePrice().doubleValue());

        var newProduct = productRepository.findByUniqueKeyAndUploadedFileId("KEY3", updateFile.getId());
        assertTrue(newProduct.isPresent());
    }
}