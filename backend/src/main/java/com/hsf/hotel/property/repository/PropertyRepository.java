package com.hsf.hotel.property.repository;

import com.hsf.hotel.property.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Integer> {
    Optional<Property> findByCode(String code);
}
