package com.barbershop.service;

import com.barbershop.domain.BarberService;
import com.barbershop.exception.BusinessException;
import com.barbershop.repository.BarberServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ServicePhotoService {

    private final BarberServiceRepository serviceRepository;
    private final ServicePhotoStorage photoStorage;

    @Transactional
    public BarberService upload(Long serviceId, MultipartFile file) {
        BarberService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("Serviço não encontrado."));
        ServicePhotoStorage.StoredPhoto photo = photoStorage.store(file);
        String previousPhotoUrl = service.getPhotoUrl();

        try {
            service.setPhotoUrl(photo.url());
            BarberService saved = serviceRepository.save(service);
            if (previousPhotoUrl != null && !previousPhotoUrl.equals(photo.url())) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        photoStorage.delete(previousPhotoUrl);
                    }
                });
            }
            return saved;
        } catch (RuntimeException exception) {
            photoStorage.delete(photo.url());
            throw exception;
        }
    }
}
