package com.fittrack.video.controller;

import com.fittrack.common.api.ApiResponse;
import com.fittrack.exercise.entity.Exercise.Difficulty;
import com.fittrack.video.dto.VideoDtos.VideoDto;
import com.fittrack.video.entity.ExerciseVideo.Category;
import com.fittrack.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Exercise videos")
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @Operation(summary = "Video library with filters (category, muscle group, difficulty, search)")
    @GetMapping
    public ApiResponse<List<VideoDto>> library(@RequestParam(required = false) Category category,
                                               @RequestParam(required = false) String muscle,
                                               @RequestParam(required = false) Difficulty difficulty,
                                               @RequestParam(required = false) String q) {
        return ApiResponse.ok(videoService.library(category, muscle, difficulty, q));
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoDto> get(@PathVariable Long id) {
        return ApiResponse.ok(videoService.get(id));
    }
}
