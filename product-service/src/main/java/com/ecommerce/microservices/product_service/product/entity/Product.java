package com.ecommerce.microservices.product_service.product.entity;

import com.ecommerce.microservices.product_service.category.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_sku", columnList = "sku")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "campaign_label", length = 60)
    private String campaignLabel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_category")
    )
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {
    }

    public Product(
            String name,
            String description,
            BigDecimal price,
            String sku,
            boolean active,
            String campaignLabel,
            Category category
    ) {
        updateDetails(name, description, price, sku, active, campaignLabel, category);
    }

    public void updateDetails(
            String name,
            String description,
            BigDecimal price,
            String sku,
            boolean active,
            String campaignLabel,
            Category category
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.sku = sku;
        this.active = active;
        this.campaignLabel = normalizeCampaignLabel(campaignLabel);
        this.category = category;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getSku() {
        return sku;
    }

    public boolean getActive() {
        return active;
    }

    public Category getCategory() {
        return category;
    }

    public String getCampaignLabel() {
        return campaignLabel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private String normalizeCampaignLabel(String campaignLabel) {
        if (campaignLabel == null) {
            return null;
        }

        String normalizedCampaignLabel = campaignLabel.trim();
        return normalizedCampaignLabel.isEmpty() ? null : normalizedCampaignLabel;
    }
}
