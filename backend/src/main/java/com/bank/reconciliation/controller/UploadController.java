package com.bank.reconciliation.controller;

import com.bank.reconciliation.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/ledger")
    public ResponseEntity<Map<String, Object>> uploadLedger(@RequestParam("file") MultipartFile file) {
        int count = 0;
        try {
            count = uploadService.uploadLedger(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(Map.of("uploaded", count, "message", "Ledger uploaded successfully"));
    }

    @PostMapping("/statement")
    public ResponseEntity<Map<String, Object>> uploadStatement(@RequestParam("file") MultipartFile file) {
        int count = 0;
        try {
            count = uploadService.uploadStatement(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(Map.of("uploaded", count, "message", "Bank statement uploaded successfully"));
    }
}
