package com.barbershop.service;

import com.barbershop.domain.BarberService;
import com.barbershop.exception.BusinessException;
import com.barbershop.repository.BarberServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final BarberServiceRepository repository;

    public List<BarberService> listActive() {
        return repository.findByActiveTrueOrderByNameAsc();
    }

    public BarberService findActiveById(Long id) {
        return repository.findById(id)
                .filter(BarberService::isActive)
                .orElseThrow(() -> new BusinessException("Serviço não encontrado ou inativo."));
    }
}
