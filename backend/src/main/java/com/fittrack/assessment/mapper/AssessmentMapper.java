package com.fittrack.assessment.mapper;

import com.fittrack.assessment.dto.AssessmentDtos.AssessmentResponse;
import com.fittrack.assessment.dto.AssessmentDtos.AssessmentSummary;
import com.fittrack.assessment.dto.AssessmentDtos.EquipmentDto;
import com.fittrack.assessment.dto.AssessmentDtos.LimitationDto;
import com.fittrack.assessment.entity.Equipment;
import com.fittrack.assessment.entity.FitnessAssessment;
import com.fittrack.assessment.entity.Limitation;
import com.fittrack.common.util.DayMask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper
public interface AssessmentMapper {

    @Mapping(target = "availableDays", expression = "java(toDays(entity.getAvailableDays()))")
    @Mapping(target = "equipment", expression = "java(toEquipment(entity.getEquipment()))")
    @Mapping(target = "limitations", expression = "java(toLimitations(entity.getLimitations()))")
    AssessmentResponse toResponse(FitnessAssessment entity);

    @Mapping(target = "daysPerWeek", expression = "java(Integer.bitCount(entity.getAvailableDays()))")
    AssessmentSummary toSummary(FitnessAssessment entity);

    EquipmentDto toDto(Equipment equipment);

    default List<DayOfWeek> toDays(int mask) {
        return DayMask.fromMask(mask).stream().sorted().toList();
    }

    default List<EquipmentDto> toEquipment(Set<Equipment> equipment) {
        return equipment.stream()
                .sorted(Comparator.comparingInt(Equipment::getSortOrder))
                .map(this::toDto)
                .toList();
    }

    default List<LimitationDto> toLimitations(Set<Limitation> limitations) {
        return limitations.stream()
                .sorted(Comparator.comparing(Limitation::getBodyArea))
                .map(l -> new LimitationDto(l.getBodyArea(), l.getSeverity()))
                .toList();
    }
}
