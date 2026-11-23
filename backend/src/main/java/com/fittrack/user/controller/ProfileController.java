package com.fittrack.user.controller;

import com.fittrack.common.api.ApiResponse;
import com.fittrack.user.dto.ProfileDtos.ProfileResponse;
import com.fittrack.user.dto.ProfileDtos.ThemeRequest;
import com.fittrack.user.dto.ProfileDtos.UpdateProfileRequest;
import com.fittrack.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Profile")
@RestController
@RequestMapping("/api/v1/me/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Get my profile")
    @GetMapping
    public ApiResponse<ProfileResponse> get() {
        return ApiResponse.ok(profileService.getMyProfile());
    }

    @Operation(summary = "Update my profile")
    @PutMapping
    public ApiResponse<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(profileService.update(request), "Profile updated");
    }

    @Operation(summary = "Change theme preference")
    @PatchMapping("/theme")
    public ApiResponse<ProfileResponse> theme(@Valid @RequestBody ThemeRequest request) {
        return ApiResponse.ok(profileService.updateTheme(request.theme()));
    }

    @Operation(summary = "Upload profile photo (JPEG/PNG, max 5 MB). Re-encoded server-side; metadata stripped.")
    @PostMapping(path = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileResponse> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(profileService.uploadPhoto(file), "Photo updated");
    }

    @Operation(summary = "Download my profile photo")
    @GetMapping(path = "/photo", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> photo() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().cachePrivate())
                .contentType(MediaType.IMAGE_JPEG)
                .body(profileService.getPhoto());
    }

    @Operation(summary = "Remove my profile photo")
    @DeleteMapping("/photo")
    public ApiResponse<Void> deletePhoto() {
        profileService.deletePhoto();
        return ApiResponse.message("Photo removed");
    }
}
