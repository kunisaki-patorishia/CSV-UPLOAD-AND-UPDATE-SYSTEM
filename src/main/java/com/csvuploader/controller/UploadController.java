package com.csvuploader.controller;

import com.csvuploader.model.UploadedFile;
import com.csvuploader.repository.UploadedFileRepository;
import com.csvuploader.service.CsvProcessingService;
import com.csvuploader.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UploadController {
    
    @Autowired
    private UploadedFileRepository uploadedFileRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private CsvProcessingService csvProcessingService;
    
    @GetMapping("/")
    public String index(Model model) {
        List<UploadedFile> uploads = uploadedFileRepository.findRecentUploads();
        model.addAttribute("uploads", uploads);
        return "index";
    }
    
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("error", "Please select a file");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                response.put("error", "Please upload a CSV file");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check for idempotency
            String checksum = fileStorageService.calculateChecksum(file);
            if (uploadedFileRepository.findByChecksum(checksum).isPresent()) {
                response.put("error", "This file has already been uploaded");
                return ResponseEntity.status(409).body(response);
            }
            
            // Store file
            String storedFilePath = fileStorageService.storeFile(file);
            
            // Create upload record
            UploadedFile uploadedFile = new UploadedFile(file.getOriginalFilename(), checksum);
            uploadedFile = uploadedFileRepository.save(uploadedFile);
            
            // Process in background
            csvProcessingService.processCsvFile(uploadedFile.getId(), storedFilePath);
            
            response.put("success", true);
            response.put("message", "File uploaded successfully. Processing in background.");
            response.put("upload", Map.of(
                "id", uploadedFile.getId(),
                "fileName", uploadedFile.getFileName(),
                "status", uploadedFile.getStatus()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/api/uploads")
    @ResponseBody
    public List<UploadedFile> getUploadsApi() {
        return uploadedFileRepository.findRecentUploads();
    }
    
    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> healthCheck() {
        return Map.of("status", "OK", "service", "CSV Uploader");
    }
}
