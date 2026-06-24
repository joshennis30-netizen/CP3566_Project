package com.example.fraud.web;

import com.example.fraud.model.Rule;
import com.example.fraud.repo.RuleRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.BigDecimal;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleRepository ruleRepo;

    public RuleController(RuleRepository ruleRepo) {
        this.ruleRepo = ruleRepo;
    }

    @GetMapping
    public List<Rule> getRules() {
        return ruleRepo.findAll();
    }

    @PutMapping("/{code}")
    public Rule updateRule(@PathVariable String code, @RequestBody Map<String, Object> update, Authentication auth) {
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(author -> author.getAuthority().replace("ROLE_", ""))
                .orElse("");

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("Only the admin can update the rules."));
        }

        Rule rule = ruleRepo.findById(code).orElseThrow(() -> new IllegalStateException("Rule " + code + " not found"));

        if (update.containsKey("thresholdAmount")) {
            Object amount = update.get("thresholdAmount");
            rule.setThresholdAmount(new BigDecimal(amount.toString()));
        }
        if (update.containsKey("windowMinutes")) {
            Object windowMinutes = update.get("windowMinutes");
            rule.setWindowMinutes(((Number) windowMinutes).intValue());
        }
        if (update.containsKey("minCount")) {
            Object minCount = update.get("minCount");
            rule.setMinCount(((Number) minCount).intValue());
        }
        if (update.containsKey("enabled")) {
            Object enabled = update.get("enabled");
            rule.setEnabled((Boolean) enabled);
        }

        return ruleRepo.save(rule);
    }
}