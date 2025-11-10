package main.java.com.csvuploader.repository;

import com.csvuploader.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    Optional<UploadedFile> findByChecksum(String checksum);
    
    @Query("SELECT u FROM UploadedFile u ORDER BY u.createdAt DESC")
    List<UploadedFile> findRecentUploads();
    
    List<UploadedFile> findByStatusOrderByCreatedAtDesc(String status);
}
