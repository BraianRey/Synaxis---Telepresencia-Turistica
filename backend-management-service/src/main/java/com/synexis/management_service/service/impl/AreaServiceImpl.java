package com.synexis.management_service.service.impl;

import com.synexis.management_service.entity.Area;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.AreaRepository;
import com.synexis.management_service.service.AreaService;
import org.springframework.stereotype.Service;

@Service
public class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;

    public AreaServiceImpl(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @Override
    public Area findById(Long areaId) {
        return areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Area not found with id: " + areaId));
    }
}
