package com.fittrack.assessment.mapper;

import com.fittrack.assessment.dto.GoalDtos.GoalResponse;
import com.fittrack.assessment.entity.FitnessGoal;
import org.mapstruct.Mapper;

@Mapper
public interface GoalMapper {

    GoalResponse toResponse(FitnessGoal goal);
}
