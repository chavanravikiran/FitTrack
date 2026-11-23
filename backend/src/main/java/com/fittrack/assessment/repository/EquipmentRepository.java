package com.fittrack.assessment.repository;

import com.fittrack.assessment.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findAllByOrderBySortOrderAsc();

    List<Equipment> findByCodeIn(Collection<String> codes);
}
