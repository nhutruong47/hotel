package com.hsf.hotel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Amenities")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String iconCode; // e.g. "fa-wifi", "fa-swimming-pool", "📺", "📶"

    public Amenity() {
    }

    public Amenity(String name, String iconCode) {
        this.name = name;
        this.iconCode = iconCode;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconCode() {
        return iconCode;
    }

    public void setIconCode(String iconCode) {
        this.iconCode = iconCode;
    }
}
