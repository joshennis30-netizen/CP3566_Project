package com.example.fraud.web;

import com.example.fraud.model.Case;
import com.example.fraud.model.AuditLog;
import com.example.fraud.repo.CaseRepository;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.service.CaseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseRepository caseRepo;
    private final CaseService caseService;
    private final AuditLogRepository auditLogRepo;
    private final AuthController auth;

    public CaseController(CaseRepository caseRepo, CaseService caseService, AuditLogRepository auditLogRepo) {
        this.caseRepo = caseRepo;
        this.caseService = caseService;
        this.auditLogRepo = auditLogRepo;
    }

    @GetMapping
    public List<Case> listCase(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return caseRepo.findAll();
        }
    }

    @GetMapping("/{id}")
    public Case getCase(@PathVariable Long id) {
        return caseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ID not found."));
    }

    @PostMapping("/{id}/pickup")
    public Case pickup(@PathVariable Long id, Authentication auth) {
        return caseService.apply(id, "pickup", auth.getName(), getRoleFromAuth(auth));
    }

    @PostMapping("/{id}/escalate")
    public Case escalate(@PathVariable Long id, Authentication auth) {
        return caseService.apply(id, "escalate", auth.getName(), getRoleFromAuth(auth));
    }

    @PostMapping("/{id}/send-back")
    public Case sendBack(@PathVariable Long id, Authentication auth) {
        return caseService.apply(id, "send-back", auth.getName(), getRoleFromAuth(auth));
    }

    @PostMapping("/{id}/close-false")
    public Case closeFalse(@PathVariable Long id, Authentication auth) {
        return caseService.apply(id, "close-false", auth.getName(), getRoleFromAuth(auth));
    }

    @PostMapping("/{id}/close-fraud")
    public Case closeFraud(@PathVariable Long id, Authentication auth) {
        return caseService.apply(id, "close-fraud", auth.getName(), getRoleFromAuth(auth));
    }

    @PostMapping("/{id}/notes")
    public Case addNote(@PathVariable Long id, Map<String, String> body, Authentication auth) {
        caseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ID not found."));

        String note = body.get("text");
        AuditLog audLog = new AuditLog(auth.getName(), "NOTE", "case", id, note, LocalDateTime.now());
        auditLogRepo.save(audLog);
    }
}