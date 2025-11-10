package main.java.com.csvuploader.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String status = "pending";
    
    @Column(nullable = false, unique = true)
    private String checksum;
    
    private String errorMessage;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private Integer processedRows = 0;
    
    public UploadedFile() {}
    
    public UploadedFile(String fileName, String checksum) {
        this.fileName = fileName;
        this.checksum = checksum;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Integer getProcessedRows() { return processedRows; }
    public void setProcessedRows(Integer processedRows) { this.processedRows = processedRows; }
}
