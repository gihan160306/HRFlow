package com.hrflow.attendance;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** API của máy chấm công tại quầy – chỉ tài khoản thiết bị KIOSK (SecurityConfig, PM-21/PM-22). */
@RestController
public class KioskController {

    public record ScanRequest(@NotBlank String qrPayload) {
    }

    public record PunchRequest(@NotNull Long scanId, String photoBase64, String photoMime) {
    }

    private final KioskService service;

    public KioskController(KioskService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/kiosk/me")
    public Map<String, Object> me() {
        return service.me();
    }

    @PostMapping("/api/v1/kiosk/scan")
    public Map<String, Object> scan(@Valid @RequestBody ScanRequest r) {
        return service.scan(r.qrPayload());
    }

    @PostMapping("/api/v1/kiosk/punch")
    public Map<String, Object> punch(@Valid @RequestBody PunchRequest r) {
        return service.punch(r.scanId(), r.photoBase64(), r.photoMime());
    }
}
