package com.csvuploader.repository;

import com.csvuploader.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByUniqueKeyAndUploadedFileId(String uniqueKey, Long uploadedFileId);
    
    @Modifying
    @Query("DELETE FROM Product p WHERE p.uploadedFile.id = :fileId")
    void deleteByUploadedFileId(@Param("fileId") Long fileId);
    
    List<Product> findByUploadedFileId(Long uploadedFileId);
    
    @Query("SELECT p FROM Product p WHERE p.uniqueKey = :uniqueKey ORDER BY p.uploadedFile.createdAt")
    List<Product> findByUniqueKeyOrderByUploadedFileId(@Param("uniqueKey") String uniqueKey);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.uploadedFile.id = :fileId")
    Long countByUploadedFileId(@Param("fileId") Long fileId);
}