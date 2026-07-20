package com.bloodlink.bloodlink.controller;

import com.bloodlink.bloodlink.dto.HospitalOptionDto;
import com.bloodlink.bloodlink.service.MetadataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
public class MetadataController {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/hospitals")
    public List<HospitalOptionDto> listHospitals() {
        return metadataService.listHospitals();
    }
}
