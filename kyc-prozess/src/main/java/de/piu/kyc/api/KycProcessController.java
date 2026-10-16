package de.piu.kyc.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.cibseven.bpm.dmn.engine.DmnDecisionResult;
import org.cibseven.bpm.engine.DecisionService;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.cibseven.bpm.engine.variable.Variables;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prozessstart und DMN-Auswertung mit denselben Pfaden und Antworten wie die Kogito-Fassung,
 * damit Weboberfläche und run_demo_cases.py unverändert bleiben. Die vollständige REST-API der
 * Engine liegt zusätzlich unter /engine-rest.
 */
@RestController
public class KycProcessController {

    static final String PROCESS_KEY = "kyc_review";
    static final String DECISION_KEY = "RuleCheck";

    private final RuntimeService runtimeService;
    private final DecisionService decisionService;
    private final ProcessVariables processVariables;

    KycProcessController(RuntimeService runtimeService, DecisionService decisionService, ProcessVariables processVariables) {
        this.runtimeService = runtimeService;
        this.decisionService = decisionService;
        this.processVariables = processVariables;
    }

    /**
     * Startet eine Instanz mit {@code {"application": {...}}}. Die Engine läuft synchron bis zur
     * manuellen Prüfung oder zum Ende; die Antwort enthält Regelergebnis und Laya-Ergebnis.
     */
    @PostMapping("/" + PROCESS_KEY)
    public ResponseEntity<Map<String, Object>> start(@RequestBody Map<String, Object> request) {
        Object application = request.get("application");
        if (!(application instanceof Map<?, ?>)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Feld application (Objekt) fehlt"));
        }
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY,
                Variables.createVariables().putValue("application", application));
        return ResponseEntity.status(HttpStatus.CREATED).body(withId(instance.getId()));
    }

    /** 200 mit Variablen, solange die Instanz läuft; 404, sobald sie beendet ist. */
    @GetMapping("/" + PROCESS_KEY + "/{id}")
    public ResponseEntity<Map<String, Object>> instance(@PathVariable String id) {
        boolean active = runtimeService.createProcessInstanceQuery().processInstanceId(id).count() > 0;
        return active ? ResponseEntity.ok(withId(id)) : ResponseEntity.notFound().build();
    }

    /** Nur die DMN-Regeln: {@code {"Application": {...}}} → {@code RuleResult}, {@code TriggeredRules}. */
    @PostMapping("/KYC_RuleCheck")
    public Object ruleCheck(@RequestBody Map<String, Object> request) {
        DmnDecisionResult result = decisionService.evaluateDecisionByKey(DECISION_KEY)
                .variables(Variables.createVariables().putValue("Application", request.get("Application")))
                .evaluate();
        return result.getSingleEntry();
    }

    private Map<String, Object> withId(String id) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.putAll(processVariables.of(id));
        return body;
    }
}
