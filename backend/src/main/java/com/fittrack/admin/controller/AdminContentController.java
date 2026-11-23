package com.fittrack.admin.controller;

import com.fittrack.common.api.ApiResponse;
import com.fittrack.common.api.PageResponse;
import com.fittrack.nutrition.dto.NutritionDtos.FoodDto;
import com.fittrack.nutrition.dto.NutritionDtos.FoodRequest;
import com.fittrack.nutrition.service.NutritionService;
import com.fittrack.video.dto.VideoDtos.ConfirmUploadRequest;
import com.fittrack.video.dto.VideoDtos.UploadRequest;
import com.fittrack.video.dto.VideoDtos.UploadTargetDto;
import com.fittrack.video.dto.VideoDtos.VideoDto;
import com.fittrack.video.dto.VideoDtos.VideoRequest;
import com.fittrack.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Content management: exercise video library (with S3 uploads) and the system food database. */
@Tag(name = "Admin content")
@Validated
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminContentController {

    private final VideoService videoService;
    private final NutritionService nutritionService;

    // ---- videos ----

    @GetMapping("/videos")
    public ApiResponse<List<VideoDto>> videos() {
        return ApiResponse.ok(videoService.adminList());
    }

    @PostMapping("/videos")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VideoDto> createVideo(@Valid @RequestBody VideoRequest req) {
        return ApiResponse.ok(videoService.create(req), "Video entry created");
    }

    @PutMapping("/videos/{id}")
    public ApiResponse<VideoDto> updateVideo(@PathVariable Long id, @Valid @RequestBody VideoRequest req) {
        return ApiResponse.ok(videoService.update(id, req), "Video entry saved");
    }

    @DeleteMapping("/videos/{id}")
    public ApiResponse<Void> deleteVideo(@PathVariable Long id) {
        videoService.delete(id);
        return ApiResponse.message("Video entry deleted");
    }

    @Operation(summary = "Upload a thumbnail image (stored in S3 or local storage)")
    @PostMapping(path = "/videos/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VideoDto> thumbnail(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(videoService.uploadThumbnail(id, file), "Thumbnail uploaded");
    }

    @Operation(summary = "Get a presigned S3 URL to upload a video file directly from the browser")
    @PostMapping("/videos/{id}/upload-url")
    public ApiResponse<UploadTargetDto> uploadUrl(@PathVariable Long id, @Valid @RequestBody UploadRequest req) {
        return ApiResponse.ok(videoService.videoUploadTarget(id, req.contentType(), req.sizeBytes()));
    }

    @Operation(summary = "Attach a finished S3 upload to the video entry")
    @PostMapping("/videos/{id}/confirm-upload")
    public ApiResponse<VideoDto> confirmUpload(@PathVariable Long id, @Valid @RequestBody ConfirmUploadRequest req) {
        return ApiResponse.ok(videoService.confirmVideoUpload(id, req.key()), "Video attached");
    }

    // ---- foods ----

    @GetMapping("/foods")
    public ApiResponse<PageResponse<FoodDto>> foods(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return ApiResponse.ok(nutritionService.systemFoods(page, size));
    }

    @PostMapping("/foods")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FoodDto> createFood(@Valid @RequestBody FoodRequest req) {
        return ApiResponse.ok(nutritionService.createSystemFood(req), "Food created");
    }

    @PutMapping("/foods/{id}")
    public ApiResponse<FoodDto> updateFood(@PathVariable Long id, @Valid @RequestBody FoodRequest req,
                                           @RequestParam(defaultValue = "true") boolean active) {
        return ApiResponse.ok(nutritionService.updateSystemFood(id, req, active), "Food saved");
    }
}
