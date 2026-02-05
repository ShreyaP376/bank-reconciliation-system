package com.bank.reconciliation.controller;

import com.bank.reconciliation.entity.AuditLog;
import com.bank.reconciliation.dto.AuditLogDto;
import com.bank.reconciliation.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLogDto>> logs() {
        List<AuditLog> list = auditService.getAllLogs();
        List<AuditLogDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private AuditLogDto toDto(AuditLog log) {
        AuditLogDto d = new AuditLogDto();
        d.setId(log.getId());
        d.setUserEmail(log.getUser() != null ? log.getUser().getEmail() : null);
        d.setTimestamp(log.getTimestamp());
        d.setAction(log.getAction());
        d.setEntityType(log.getEntityType());
        d.setEntityId(log.getEntityId());
        d.setReason(log.getReason());
        d.setBeforeState(log.getBeforeState());
        d.setAfterState(log.getAfterState());
        return d;
    }
}
