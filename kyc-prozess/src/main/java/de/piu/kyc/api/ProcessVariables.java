package de.piu.kyc.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.bpm.engine.HistoryService;
import org.cibseven.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Component;

/** Liest die fachlichen Prozessvariablen aus der Historie; funktioniert für laufende und beendete Instanzen. */
@Component
public class ProcessVariables {

    /** Variablen auf Prozessebene, wie sie die Weboberfläche und run_demo_cases.py erwarten. */
    public static final List<String> NAMES = List.of(
            "application", "rule_result", "triggered_rules", "llm_result", "manual_decision", "manual_comment");

    private final HistoryService historyService;

    ProcessVariables(HistoryService historyService) {
        this.historyService = historyService;
    }

    public Map<String, Object> of(String processInstanceId) {
        return byInstance(List.of(processInstanceId)).getOrDefault(processInstanceId, emptyVariables());
    }

    /** Variablen mehrerer Instanzen mit einer Abfrage. Fehlende Variablen sind {@code null}. */
    public Map<String, Map<String, Object>> byInstance(List<String> processInstanceIds) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (processInstanceIds.isEmpty()) {
            return result;
        }
        processInstanceIds.forEach(id -> result.put(id, emptyVariables()));
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceIdIn(processInstanceIds.toArray(String[]::new))
                .variableNameIn(NAMES.toArray(String[]::new))
                .list();
        for (HistoricVariableInstance variable : variables) {
            // Nur Variablen auf Prozessebene, nicht lokale Kopien einzelner Aktivitäten
            if (variable.getActivityInstanceId() == null
                    || variable.getActivityInstanceId().equals(variable.getProcessInstanceId())) {
                result.get(variable.getProcessInstanceId()).put(variable.getName(), variable.getValue());
            }
        }
        return result;
    }

    private static Map<String, Object> emptyVariables() {
        Map<String, Object> variables = new LinkedHashMap<>();
        NAMES.forEach(name -> variables.put(name, null));
        return variables;
    }
}
