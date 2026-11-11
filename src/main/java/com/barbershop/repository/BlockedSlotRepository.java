package com.barbershop.repository;

import com.barbershop.domain.BlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositório de bloqueios pontuais de agenda.
 */
@Repository
public interface BlockedSlotRepository extends JpaRepository<BlockedSlot, Long> {

    /**
     * Retorna todos os bloqueios de uma data específica.
     * O serviço de cálculo de slots livres usa este método para
     * suprimir slots sobrepostos com bloqueios.
     */
    List<BlockedSlot> findByDate(LocalDate date);

    /**
     * Retorna bloqueios em um intervalo de datas.
     * Útil para exibir bloqueios futuros na agenda semanal do barbeiro.
     */
    List<BlockedSlot> findByDateBetweenOrderByDateAscStartTimeAsc(
            LocalDate from, LocalDate to);
}
