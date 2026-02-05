package com.bank.reconciliation.controller;

import com.bank.reconciliation.dto.OverrideRequest;
import com.bank.reconciliation.service.OverrideService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/override")
public class OverrideController {

    private final OverrideService overrideService;

    public OverrideController(OverrideService overrideService) {
        this.overrideService = overrideService;
    }

    @PostMapping("/link")
    public ResponseEntity<Void> link(@Valid @RequestBody OverrideRequest request, Authentication auth) {
        String email = auth != null ? auth.getName() : "system";
        overrideService.linkManually(email, request.getInvoiceId(), request.getTransactionId(), request.getAmount(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unlink")
    public ResponseEntity<Void> unlink(@Valid @RequestBody OverrideRequest request, Authentication auth) {
        String email = auth != null ? auth.getName() : "system";
        overrideService.unlink(email, request.getInvoiceId(), request.getTransactionId(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/invoices/{id}/notes")
    public ResponseEntity<Void> updateNotes(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body, Authentication auth) {
        String email = auth != null ? auth.getName() : "system";
        String notes = body != null && body.containsKey("notes") ? body.get("notes") : "";
        overrideService.addInvoiceNotes(email, id, notes != null ? notes : "", "Manual notes update");
        return ResponseEntity.ok().build();
    }
}
