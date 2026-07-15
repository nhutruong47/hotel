package com.hsf.hotel.service;

import com.hsf.hotel.model.Amenity;
import com.hsf.hotel.repository.AmenityRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AmenityService {

    private final AmenityRepository amenityRepository;

    public AmenityService(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "amenities", key = "'all'")
    public List<Amenity> getAllAmenities() {
        return amenityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Amenity> getAmenityById(Integer id) {
        return amenityRepository.findById(id);
    }

    @CacheEvict(cacheNames = "amenities", allEntries = true)
    public Amenity saveAmenity(Amenity amenity) {
        return amenityRepository.save(amenity);
    }

    @Transactional
    @CacheEvict(cacheNames = "amenities", allEntries = true)
    public void deleteAmenity(Integer id) {
        amenityRepository.deleteById(id);
    }
}
