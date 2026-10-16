package de.piu.kyc.api;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.bpm.engine.TaskService;
import org.cibseven.bpm.engine.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manuelle Prüfung über die Aufgabenschnittstelle der Kogito-Fassung (claim, complete, skip),
 * umgesetzt mit dem TaskService der Engine. Die Ergebnisse {@code decision} und {@code comment}
 * landen in den Prozessvariablen {@code manual_decision} und {@code manual_comment}, wie bei einer
 * Bearbeitung in der Tasklist von CIB seven.
 *
 * <p>Keine Authentifizierung: Benutzer und Gruppe kommen als Query-Parameter.
 */
@RestController
public class UserTaskController {

    private final TaskService taskService;
    private final ProcessVariables processVariables;

    UserTaskController(TaskService taskService, ProcessVariables processVariables) {
        this.taskService = taskService;
        this.processVariables = processVariables;
    }

    /** Offene Aufgaben der Gruppe: noch nicht übernommen (Ready) oder vom Benutzer übernommen (Reserved). */
    @GetMapping("/usertasks/instance")
    public List<Map<String, Object>> tasks(@RequestParam String user, @RequestParam String group) {
        List<Task> tasks = taskService.createTaskQuery()
                .processDefinitionKey(KycProcessController.PROCESS_KEY)
                .or().taskCandidateGroup(group).taskAssignee(user).endOr()
                .orderByTaskCreateTime().asc()
                .list();
        Map<String, Map<String, Object>> variables = processVariables.byInstance(
                tasks.stream().map(Task::getProcessInstanceId).distinct().toList());
        return tasks.stream().map(task -> view(task, variables.get(task.getProcessInstanceId()))).toList();
    }

    @PostMapping("/usertasks/instance/{id}/transition")
    public Map<String, Object> transition(@PathVariable String id, @RequestParam String user, @RequestParam String group,
                                          @RequestBody Map<String, Object> request) {
        Task task = taskService.createTaskQuery().taskId(id)
                .or().taskCandidateGroup(group).taskAssignee(user).endOr()
                .singleResult();
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Keine Aufgabe " + id + " für " + user + "/" + group);
        }
        String transition = String.valueOf(request.get("transitionId"));
        switch (transition) {
            case "claim" -> taskService.claim(id, user);
            case "complete" -> taskService.complete(id, result(request.get("data")));
            // Ohne Ergebnis abschließen: Das Gateway "Entscheidung" erzeugt die Aufgabe neu.
            case "skip" -> taskService.complete(id);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Transition " + transition);
        }
        return Map.of("id", id, "transitionId", transition);
    }

    private static Map<String, Object> result(Object data) {
        Map<String, Object> variables = new HashMap<>();
        if (data instanceof Map<?, ?> map) {
            variables.put("manual_decision", map.get("decision"));
            variables.put("manual_comment", map.get("comment"));
        }
        return variables;
    }

    /** Aufbau wie in Kogito 10: id, status.name, actualOwner, processInfo, inputs. */
    private static Map<String, Object> view(Task task, Map<String, Object> variables) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", task.getId());
        view.put("taskName", task.getName());
        view.put("status", Map.of("name", task.getAssignee() == null ? "Ready" : "Reserved"));
        view.put("actualOwner", task.getAssignee());
        view.put("created", task.getCreateTime());
        view.put("processInfo", Map.of("processInstanceId", task.getProcessInstanceId(),
                "processId", KycProcessController.PROCESS_KEY));
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("application", variables.get("application"));
        inputs.put("triggered_rules", variables.get("triggered_rules"));
        inputs.put("llm_result", variables.get("llm_result"));
        view.put("inputs", inputs);
        return view;
    }
}
