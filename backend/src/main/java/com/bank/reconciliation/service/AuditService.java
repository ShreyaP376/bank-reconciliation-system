package com.bank.reconciliation.service;

import com.bank.reconciliation.entity.AuditLog;
import com.bank.reconciliation.entity.User;
import com.bank.reconciliation.repository.AuditLogRepository;
import com.bank.reconciliation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @Transactional
    public void log(String userEmail, String action, String entityType, Long entityId, String reason, String beforeState, String afterState) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setTimestamp(Instant.now());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setReason(reason);
        log.setBeforeState(beforeState);
        log.setAfterState(afterState);
        auditLogRepository.save(log);
    }
}
