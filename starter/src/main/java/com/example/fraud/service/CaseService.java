package com.example.fraud.service;

import com.example.fraud.model.Case;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import org.springframework.stereotype.Service;

/**
 * TODO (student) — THE CASE WORKFLOW (state machine).   PROJECT_BRIEF.html §4.4
 *
 * Legal moves (anything else must be rejected):
 *   pickup:      NEW       -> REVIEWING      (ANALYST)
 *   escalate:    REVIEWING -> ESCALATED      (ANALYST)
 *   send-back:   ESCALATED -> REVIEWING      (INVESTIGATOR)
 *   close-false: REVIEWING -> CLOSED_FALSE   (ANALYST)
 *   close-false: ESCALATED -> CLOSED_FALSE   (INVESTIGATOR)
 *   close-fraud: ESCALATED -> CLOSED_FRAUD   (INVESTIGATOR)
 *
 * The fields and the method signature below are GIVEN — you fill in the body.
 */
@Service
public class CaseService {

    private final CaseRepository caseRepo;
    private final AuditLogRepository auditLogRepo;

    public CaseService(CaseRepository caseRepo, AuditLogRepository auditLogRepo) {
        this.caseRepo = caseRepo;
        this.auditLogRepo = auditLogRepo;
    }

    /**
     * Apply an action to a case, enforcing the state machine and the role rules. Throw so your
     * controller can map it to the right HTTP status:
     *   case not found -> 404,  illegal move from the current state -> 409,  wrong role -> 403.
     *
     * @param caseId        the case to act on
     * @param action        one of: pickup, escalate, send-back, close-false, close-fraud
     * @param actorUsername who is doing it (use for assignedTo on pickup, and for the audit log)
     * @param actorRole     ANALYST | INVESTIGATOR | ADMIN
     * @return the updated case
     */
    public Case apply(Long caseId, String action, String actorUsername, String actorRole) {
        // TODO (student):
        //   1. load the case from caseRepo (not found -> throw 404)
        //   2. is `action` legal from the case's current status? if not -> throw 409
        //   3. is `actorRole` allowed to make that move? if not -> throw 403
        //   4. set the new status (and assignedTo on pickup), then save the case
        //   5. write one audit_log row with auditLogRepo, then return the updated case

        Case case = caseRepo.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException("Case with id " + caseId + " missing.");

        String currentStatus = case.getStatus();
        String status = null;

        switch (action) {
            case "pickup":
                if ("NEW".equals(currentStatus)) {
                    status = "REVIEWING";
                } else {
                    throw new IllegalStateException("Illegal status " + currentStatus + " with action " + action);
                }
                break;

            case "escalate":
                if ("REVIEWING".equals(currentStatus)) {
                    status = "ESCALATED";
                } else {
                    throw new IllegalStateException("Illegal status " + currentStatus + " with action " + action);
                }
                break;

            case "send-back":
                if ("ESCALATED".equals(currentStatus)) {
                    status = "REVIEWING";
                } else {
                    throw new IllegalStateException("Illegal status " + currentStatus + " with action " + action);
                }
                break;

            case "close-false":
                if ("REVIEWING".equals(currentStatus || "ESCALATED".equals(currentStatus))) {
                    status = "CLOSED_FALSE";
                } else {
                    throw new IllegalStateException("Illegal status " + currentStatus + " with action " + action);
                }
                break;

            case "close-fraud":
                if ("ESCALATED".equals(currentStatus)) {
                    status = "CLOSED_FRAUD";
                } else {
                    throw new IllegalStateException("Illegal status " + currentStatus + " with action " + action);
                }
                break;

            default:
                throw new IllegalStateException("Illegal status " + currentStatus + " with action " + action);
        }

        String role = actorRole.trim();
        boolean correctRole;
        switch (action) {
            case "pickup":
                correctRole = "ANALYST".equals(role) || "ADMIN".equals(role);
                break;
            case "escalate":
                correctRole = "ANALYST".equals(role) || "ADMIN".equals(role);
                break;
            case "send-back":
                correctRole = "INVESTIGATOR".equals(role) || "ADMIN".equals(role);
                break;
            case "close-fraud":
                if ("REVIEWING".equals(currentStatus)) {
                    correctRole = "ANALYST".equals(role) || "ADMIN".equals(role);
                } else {
                    correctRole = "INVESTIGATOR".equals(role) || "ADMIN".equals(role);
                }
                break;
            case "close-fraud":
                correctRole = "INVESTIGATOR".equals(role) || "ADMIN".equals(role);
                break;
            default:
                correctRole = false;
        }

        case.setStatus(status);
        if ("pickup".equals(action)) {
            case.setAssignedTo(actorUsername);
        }
        Case caseSave = caseRepo.save(case);

        AuditLog audLog = new AuditLog(actorUsername, action, "case", caseSave.getId(), "Status: " + status, LocalDateTime.now());
        auditLogRepo.save(audLog);

        return caseSave;
    }
}
