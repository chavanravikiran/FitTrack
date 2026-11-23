package com.fittrack.user.controller;

import com.fittrack.auth.RefreshCookieFactory;
import com.fittrack.auth.dto.AuthDtos.DeleteAccountRequest;
import com.fittrack.common.api.ApiResponse;
import com.fittrack.user.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account")
@RestController
@RequestMapping("/api/v1/me/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final RefreshCookieFactory cookieFactory;

    @Operation(summary = "Permanently delete my account and all my data")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteMyAccount(request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .body(ApiResponse.message("Your account and data have been deleted."));
    }
}
