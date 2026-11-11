package com.barbershop.repository;

import com.barbershop.domain.BarberService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório dos serviços da barbearia.
 */
@Repository
public interface BarberServiceRepository extends JpaRepository<BarberService, Long> {

    /**
     * Retorna apenas os serviços ativos — usados na listagem pública
     * que o cliente vê antes de agendar.
     */
    List<BarberService> findByActiveTrueOrderByNameAsc();

    /**
     * Verifica se já existe um serviço com o mesmo nome (case-sensitive).
     * Usado na criação e edição para evitar duplicatas.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Verifica duplicata de nome excluindo o próprio serviço (na edição).
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
