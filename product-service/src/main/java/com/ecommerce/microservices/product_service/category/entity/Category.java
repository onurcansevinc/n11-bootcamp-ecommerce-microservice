package com.ecommerce.microservices.product_service.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_slug", columnList = "slug")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
        }
)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank
    @Size(max = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    @NotBlank
    @Size(max = 120)
    private String slug;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Category() {
    }

    public Category(String name, String slug, boolean active) {
        updateDetails(name, slug, active);
    }

    public void updateDetails(String name, String slug, boolean active) {
        this.name = name;
        this.slug = slug;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
