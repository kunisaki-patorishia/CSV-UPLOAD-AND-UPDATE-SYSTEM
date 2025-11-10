package com.csvuploader.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"unique_key", "uploaded_file_id"})
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "unique_key", nullable = false)
    private String uniqueKey;
    
    @Column(name = "product_title")
    private String productTitle;
    
    @Column(name = "product_description", length = 2000)
    private String productDescription;
    
    @Column(name = "style_number")
    private String styleNumber;
    
    @Column(name = "sanmar_mainframe_color")
    private String sanmarMainframeColor;
    
    private String size;
    
    @Column(name = "color_name")
    private String colorName;
    
    @Column(name = "piece_price", precision = 10, scale = 2)
    private BigDecimal piecePrice;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_file_id")
    private UploadedFile uploadedFile;
    
    public Product() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUniqueKey() { return uniqueKey; }
    public void setUniqueKey(String uniqueKey) { this.uniqueKey = uniqueKey; }
    
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    
    public String getStyleNumber() { return styleNumber; }
    public void setStyleNumber(String styleNumber) { this.styleNumber = styleNumber; }
    
    public String getSanmarMainframeColor() { return sanmarMainframeColor; }
    public void setSanmarMainframeColor(String sanmarMainframeColor) { this.sanmarMainframeColor = sanmarMainframeColor; }
    
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    
    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }
    
    public BigDecimal getPiecePrice() { return piecePrice; }
    public void setPiecePrice(BigDecimal piecePrice) { this.piecePrice = piecePrice; }
    
    public UploadedFile getUploadedFile() { return uploadedFile; }
    public void setUploadedFile(UploadedFile uploadedFile) { this.uploadedFile = uploadedFile; }
}
