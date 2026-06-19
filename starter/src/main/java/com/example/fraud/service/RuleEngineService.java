package com.example.fraud.service;

import com.example.fraud.repo.AlertRepository;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import com.example.fraud.repo.RuleRepository;
import com.example.fraud.repo.TransactionRepository;
import com.example.fraud.repo.WatchlistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * TODO (student) — THE RULE ENGINE.   PROJECT_BRIEF.html §4.3
 *
 * Scan every transaction and OPEN A NEW CASE for each rule hit:
 *   R1  large single amount
 *   R2  velocity (too many transactions too fast on one account)
 *   R3  counterparty on the watchlist                              [R1–R3 required]
 *   R4  structuring (several transfers just under the limit)       [bonus]
 *
 * The fields and method signatures below are GIVEN — you fill in the bodies.
 *   - Read thresholds from the `rules` table via ruleRepo (do NOT hard-code them).
 *   - Opening a case = save an Alert, then a Case with status "NEW", then an audit_log row.
 *   - On the seeded data this must open EXACTLY 16 cases (5 R1 + 3 R2 + 5 R3 + 3 R4).
 *   - The given repositories return everything (findAll, etc.); you may also ADD query methods
 *     to them (e.g. findByAmountGreaterThanEqual) if you prefer to let the database filter.
 */
@Service
public class RuleEngineService {

    private final TransactionRepository transactionRepo;
    private final RuleRepository ruleRepo;
    private final WatchlistRepository watchlistRepo;
    private final AlertRepository alertRepo;
    private final CaseRepository caseRepo;
    private final AuditLogRepository auditLogRepo;

    public RuleEngineService(TransactionRepository transactionRepo, RuleRepository ruleRepo,
                             WatchlistRepository watchlistRepo, AlertRepository alertRepo,
                             CaseRepository caseRepo, AuditLogRepository auditLogRepo) {
        this.transactionRepo = transactionRepo;
        this.ruleRepo = ruleRepo;
        this.watchlistRepo = watchlistRepo;
        this.alertRepo = alertRepo;
        this.caseRepo = caseRepo;
        this.auditLogRepo = auditLogRepo;
    }

    /**
     * Scan all transactions and open one NEW case per rule hit.
     * @return the number of cases opened (should be 16 on the seeded data)
     */
    public int scanAndOpenCases() {
        // TODO (student):
        //   1. load the rules (thresholds) from ruleRepo into something you can look up by code
        //   2. R1 — open a case for each transaction whose amount >= the R1 threshold
        //   3. R3 — open a case for each transaction whose counterparty is on the watchlist
        //   4. R2 — for one account, open a case if >= R2.minCount transactions fall within
        //           R2.windowMinutes of each other
        //   5. R4 (bonus) — for one account, open a case if >= R4.minCount transfers of
        //           $9,000–$9,999 fall within R4.windowMinutes
        //   call openCase(...) for every hit, and count them.
        return 0;
    }

    /**
     * Open one case for a transaction that tripped a rule.
     * @param transactionId the offending transaction's id
     * @param ruleCode      "R1".."R4"
     * @param detail        a short, human-readable reason
     * @param when          the timestamp to stamp the alert and case with
     */
    private void openCase(long transactionId, String ruleCode, String detail, LocalDateTime when) {
        // TODO (student): save a new Alert(transactionId, ruleCode, detail, when) with alertRepo,
        //   then a new Case(alertId, "NEW", null, when) with caseRepo,
        //   then an AuditLog row ("system", "OPEN_CASE", "case", caseId, ...) with auditLogRepo.
    }
}
