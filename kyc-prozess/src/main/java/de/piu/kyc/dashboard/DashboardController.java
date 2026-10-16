package de.piu.kyc.dashboard;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cibseven.bpm.engine.HistoryService;
import org.cibseven.bpm.engine.history.HistoricActivityInstance;
import org.cibseven.bpm.engine.history.HistoricProcessInstance;
import org.cibseven.bpm.engine.history.HistoricProcessInstanceQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.piu.kyc.LayaClient;
import de.piu.kyc.LlmResult;
import de.piu.kyc.api.ProcessVariables;

/**
 * Daten für Webinterface und Dashboard. Quelle ist die Historie der Engine (History-Level full):
 * Sie behält auch beendete Instanzen, ein eigenes Protokoll wie in der Kogito-Fassung ist nicht nötig.
 */
@RestController
@RequestMapping(path = "/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardController {

    static final int MAX_ENTRIES = 1000;
    private static final String PROCESS_KEY = "kyc_review";

    private final HistoryService historyService;
    private final ProcessVariables processVariables;
    private final LayaClient laya;

    /** „Leeren“ blendet ältere Instanzen aus; die Historie der Engine bleibt vollständig (Cockpit). */
    private volatile Date clearedAt;

    DashboardController(HistoryService historyService, ProcessVariables processVariables, LayaClient laya) {
        this.historyService = historyService;
        this.processVariables = processVariables;
        this.laya = laya;
    }

    /** Erreichbarkeit des Laya-Dienstes für die Statusanzeige. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        try {
            return Map.of("laya", laya.health());
        } catch (RuntimeException e) {
            return Map.of("laya", Map.of("status", "unreachable", "error", String.valueOf(e.getMessage())));
        }
    }

    /** Neueste zuerst. */
    @GetMapping("/instances")
    public List<Map<String, Object>> instances() {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY);
        if (clearedAt != null) {
            query.startedAfter(clearedAt);
        }
        List<HistoricProcessInstance> instances = query.orderByProcessInstanceStartTime().desc().listPage(0, MAX_ENTRIES);
        Map<String, Map<String, Object>> variables = processVariables.byInstance(
                instances.stream().map(HistoricProcessInstance::getId).toList());
        // Laufende Instanzen warten nur an der manuellen Prüfung
        Map<String, String> waitingAt = historyService.createHistoricActivityInstanceQuery()
                .unfinished().activityType("userTask").list().stream()
                .collect(Collectors.toMap(HistoricActivityInstance::getProcessInstanceId,
                        HistoricActivityInstance::getActivityName, (a, b) -> a));
        return instances.stream().map(instance -> {
            InstanceHistory history = history(instance, variables.get(instance.getId()));
            history.waitingAt = waitingAt.get(instance.getId());
            return history.summary();
        }).toList();
    }

    @GetMapping("/instances/{id}")
    public InstanceHistory instance(@PathVariable String id) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(id).singleResult();
        if (instance == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unbekannte Instanz " + id);
        }
        InstanceHistory history = history(instance, processVariables.of(id));
        historyService.createHistoricActivityInstanceQuery().processInstanceId(id)
                .orderPartiallyByOccurrence().asc().list()
                .forEach(a -> history.steps.add(new InstanceHistory.Step(a.getActivityId(), a.getActivityName(),
                        instant(a.getStartTime()), instant(a.getEndTime()))));
        history.waitingAt = history.steps.stream().filter(s -> s.left() == null).map(InstanceHistory.Step::name)
                .findFirst().orElse(null);
        return history;
    }

    @DeleteMapping("/instances")
    public void clear() {
        clearedAt = new Date();
    }

    /** Das ausgeführte BPMN, für die Darstellung mit bpmn-js. */
    @GetMapping(path = "/process.bpmn", produces = MediaType.APPLICATION_XML_VALUE)
    public String processDefinition() throws IOException {
        return classpathResource("/KYC_Pruefprozess.bpmn");
    }

    @SuppressWarnings("unchecked")
    private static InstanceHistory history(HistoricProcessInstance instance, Map<String, Object> variables) {
        InstanceHistory history = new InstanceHistory();
        history.id = instance.getId();
        history.status = switch (instance.getState()) {
            case HistoricProcessInstance.STATE_ACTIVE -> InstanceHistory.STATUS_ACTIVE;
            case HistoricProcessInstance.STATE_COMPLETED -> "completed";
            case HistoricProcessInstance.STATE_SUSPENDED -> "suspended";
            default -> "aborted";
        };
        history.start = instant(instance.getStartTime());
        history.end = instant(instance.getEndTime());
        String endActivity = instance.getEndActivityId();
        if (endActivity != null && endActivity.startsWith("End_")) {
            history.outcome = "End_Approve".equals(endActivity) ? "approved" : "rejected";
        }
        history.application = (Map<String, Object>) variables.get("application");
        history.customer = customerName(history.application);
        history.ruleResult = (String) variables.get("rule_result");
        history.triggeredRules = (List<String>) variables.get("triggered_rules");
        history.llmResult = (LlmResult) variables.get("llm_result");
        history.manualDecision = (String) variables.get("manual_decision");
        history.manualComment = (String) variables.get("manual_comment");
        return history;
    }

    private static Instant instant(Date date) {
        return date == null ? null : date.toInstant();
    }

    /** Die Antragsfelder folgen dem Datensatzschema (deutsch). */
    @SuppressWarnings("unchecked")
    private static String customerName(Map<String, Object> application) {
        if (application == null || !(application.get("kunde") instanceof Map<?, ?> raw)) {
            return null;
        }
        Map<String, Object> person = (Map<String, Object>) raw;
        return (person.getOrDefault("vorname", "") + " " + person.getOrDefault("nachname", "")).trim();
    }

    private static String classpathResource(String path) throws IOException {
        try (InputStream in = DashboardController.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
