package de.piu.kyc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementierung des BPMN-Service-Tasks "Prüfung mit LLM"
 * ({@code ${layaAssessmentService.assess(application, triggered_rules)}}).
 *
 * <p>Ergänzt den Antrag um das im Prozess berechnete Regelergebnis, so wie das Modell es im
 * Training gesehen hat, und ruft den Laya-Dienst auf. Ein vom Aufrufer mitgeliefertes
 * {@code regelpruefung}-Feld wird überschrieben. Technische Fehler führen nicht zum Abbruch,
 * sondern zur Empfehlung einer manuellen Prüfung.
 */
@Service
public class LayaAssessmentService {

    private static final Logger LOG = LoggerFactory.getLogger(LayaAssessmentService.class);

    private final LayaClient laya;

    LayaAssessmentService(LayaClient laya) {
        this.laya = laya;
    }

    /**
     * Regeln, die Laya aus dem Training kennt. Spätere Regeln (z. B. R4 USA-Bezug) setzt allein der
     * Prozess durch; sie erzwingen die manuelle Prüfung unabhängig vom Modell und würden das Modell
     * nur mit einer nie gesehenen Eingabe konfrontieren.
     */
    static final Set<String> MODEL_RULES = Set.of("R1", "R2", "R3");

    public LlmResult assess(Map<String, Object> application, List<String> triggeredRules) {
        List<String> rules = triggeredRules == null ? List.of()
                : triggeredRules.stream().filter(MODEL_RULES::contains).toList();
        // Feldnamen gehören zum Datensatzschema, mit dem Laya trainiert wurde.
        Map<String, Object> input = new LinkedHashMap<>(application);
        input.put("regelpruefung", Map.of(
                "ergebnis", rules.isEmpty() ? "Gruen" : "Pruefbedarf",
                "ausgeloeste_regeln", rules));
        try {
            LlmResult result = laya.assess(Map.of("application", input));
            if (result == null || result.getDecision() == null) {
                return LlmResult.error("Leere Antwort des Laya-Dienstes");
            }
            LOG.info("Laya: {} {}", result.getDecision(), result.getProbabilities());
            return result;
        } catch (RuntimeException e) {
            LOG.warn("Laya-Dienst nicht nutzbar, Fall geht zur manuellen Prüfung: {}", e.getMessage());
            return LlmResult.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
