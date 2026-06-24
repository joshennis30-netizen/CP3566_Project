package com.example.fraud.service;

import com.example.fraud.repo.AlertRepository;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import com.example.fraud.repo.RuleRepository;
import com.example.fraud.repo.TransactionRepository;
import com.example.fraud.repo.WatchlistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.math.*;
import com.example.fraud.model.Alert;
import com.example.fraud.model.AuditLog;
import com.example.fraud.model.Case;
import com.example.fraud.model.Rule;
import com.example.fraud.model.Transaction;
import com.example.fraud.model.Watchlist;

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

        int cases = 0;

        Map<String, Rule> ruleMap = new HashMap<>();
        for (Rule R : ruleRepo.findAll()) {
            if (R != null && R.getCode() != null) {
                ruleMap.put(R.getCode(), R);
            }
        }

        List<Transaction> transactionList = transactionRepo.findAll();


        Rule R1 = ruleMap.get("R1");
        if (R1 != null && R1.isEnabled() && R1.getThresholdAmount() != null) {
            BigDecimal thresholdAmount = R1.getThresholdAmount();
            for (Transaction trans : transactionList) {
                if (trans != null && trans.getAmount() != null && trans.getAmount().compareTo(thresholdAmount) >= 0) {
                    String amount = "Amount " + trans.getAmount().toPlainString() + " >= " + thresholdAmount.toPlainString();
                    LocalDateTime when = trans.getOccuredAt();
                    openCase(trans.getId(), "R1", amount, when);
                    cases++;
                }
            }
        }

        Rule R3 = ruleMap.get("R3");
        if (R3 != null && R3.isEnabled()) {
            Set<String> watchlists = new HashSet<>();
            for (Watchlist W : watchlistRepo.findAll()) {
                if (W != null && W.getName() != null) {
                    watchlists.add(W.getName().toLowerCase().trim());
                }
            }
            if (!watchlists.isEmpty()) {
                for (Transaction trans : transactionList) {
                    if (trans != null && trans.getCounterparty() != null &&
                            watchlists.contains(trans.getCounterparty())) {
                        String countParty = "Counterparty " + trans.getCounterparty() + " is on the watchlist";
                        LocalDateTime when = trans.getOccuredAt();
                        openCase(trans.getId(), "R3", countParty, when);
                        cases++;
                    }
                }
            }
        }

        Rule R2 = ruleMap.get("R2");
        if (R2 != null && R2.isEnabled() && R2.getMinCount() > 0) {
            int windowMinutes = R2.getWindowMinutes();
            int minCount = R2.getMinCount();

            Map<Long, List<Transaction>> accounts = new HashMap<>();
            for (Transaction trans : transactionList) {
                if (trans != null && trans.getAccountId() != null) {
                    accounts.computeIfAbsent(trans.getAccountId(), key -> new ArrayList<>()).add(trans);
                }
            }
            for (Map.Entry<Long, List<Transaction>> entry : accounts.entrySet()) {
                List<Transaction> transact = entry.getValue();
                transact.sort((time1, time2) -> time1.getOccuredAt().compareTo(time2.getOccuredAt()));
                for (int i = 0; i < transact.size(); i++) {
                    LocalDateTime startTime = transact.get(i).getOccuredAt();
                    LocalDateTime endTime = startTime.plusMinutes(windowMinutes);
                    int transCount = 0;
                    for (Transaction trans : transact) {
                        if (!trans.getOccuredAt().isBefore(startTime) && !trans.getOccuredAt().isAfter(endTime)) {
                            transCount++;
                        }
                    }
                    if (transCount >= minCount) {
                        String within = transCount + " transactions within " + windowMinutes + " minutes";
                        openCase(transact.get(i).getId(), "R2", within, startTime);
                        cases++;
                    }
                }
            }
        }

        Rule R4 = ruleMap.get("R4");
        if (R4 != null && R4.isEnabled() && R4.getMinCount() > 0 && R4.getThresholdAmount() != null) {
            int minCount = R4.getMinCount();
            int windowMinutes = R4.getWindowMinutes();
            BigDecimal thresholdAmount = R4.getThresholdAmount();
            Map<Long, List<Transaction>> accounts = new HashMap<>();
            for (Transaction trans : transactionList) {
                if (trans != null && trans.getOccuredAt() != null && trans.getAccountId() != null && trans.getAmount() != null && trans.getAmount().compareTo(thresholdAmount) < 0) {
                    accounts.computeIfAbsent(trans.getAccountId(), key -> new ArrayList<>()).add(trans);
                }
            }
            for (Map.Entry<Long, List<Transaction>> account : accounts.entrySet()) {
                List<Transaction> transact = account.getValue();
                transact.sort((time1, time2) -> time1.getOccuredAt().compareTo(time2.getOccuredAt()));

                for (int i = 0; i < transact.size(); i++) {
                    LocalDateTime startTime = transact.get(i).getOccuredAt();
                    LocalDateTime endTime = startTime.plusMinutes(windowMinutes);
                    int transCount = 0;
                    for (Transaction trans : transactionList) {
                        if (!trans.getOccuredAt().isBefore(startTime) && !trans.getOccuredAt().isAfter(endTime)) {
                            transCount++;
                        }
                    }
                    if (transCount >= minCount) {
                        String within = transCount + " transactions within " + windowMinutes + " minutes";
                        openCase(transact.get(i).getId(), "R4", within, startTime);
                        cases++;
                    }
                }
            }
        }
        return cases;
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
        Alert alert = new Alert(transactionId, ruleCode, detail, when);
        alert = alertRepo.save(alert);

        Case case1 = new Case(alert.getId(), "NEW", null, when);
        Case caseSave = caseRepo.save(case1);

        AuditLog audLog = new AuditLog("system", "OPEN_CASE", "case", caseSave.getId(), "Rule " + ruleCode, when);
            auditLogRepo.save(audLog);
    }
}
