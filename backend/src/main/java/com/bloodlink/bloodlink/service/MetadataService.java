package com.bloodlink.bloodlink.service;

import com.bloodlink.bloodlink.dto.HospitalOptionDto;
import com.bloodlink.bloodlink.repository.HospitalProfileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetadataService {

    private final HospitalProfileRepository hospitalProfileRepository;

    public MetadataService(HospitalProfileRepository hospitalProfileRepository) {
        this.hospitalProfileRepository = hospitalProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<HospitalOptionDto> listHospitals() {
        return hospitalProfileRepository.findAll()
            .stream()
            .map(h -> new HospitalOptionDto(
                h.getHospitalId(),
                h.getHospitalName(),
                h.getCity(),
                h.getState()
            ))
            .toList();
    }
}
