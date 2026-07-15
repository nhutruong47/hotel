package com.hsf.hotel.service;

import com.hsf.hotel.model.Amenity;
import com.hsf.hotel.repository.AmenityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AmenityService {

    @Autowired
    private AmenityRepository amenityRepository;

    public List<Amenity> getAllAmenities() {
        return amenityRepository.findAll();
    }

    public Optional<Amenity> getAmenityById(Integer id) {
        return amenityRepository.findById(id);
    }

    public Amenity saveAmenity(Amenity amenity) {
        return amenityRepository.save(amenity);
    }

    public void deleteAmenity(Integer id) {
        amenityRepository.deleteById(id);
    }
}
