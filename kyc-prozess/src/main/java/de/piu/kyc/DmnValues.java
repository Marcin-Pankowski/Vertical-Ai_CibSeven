package de.piu.kyc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Hilfsfunktionen für Output-Mappings nach DMN-Entscheidungen, z. B.
 * {@code ${dmnValues.list(rule_check.TriggeredRules)}}.
 *
 * <p>Die FEEL-Engine liefert Listen und Kontexte als Scala-Wrapper. Als Prozessvariable gespeichert,
 * lassen sie sich nicht wieder deserialisieren; deshalb werden sie in normale Java-Collections kopiert.
 */
@Component
public class DmnValues {

    public List<Object> list(Object value) {
        return value == null ? new ArrayList<>() : new ArrayList<>((Collection<?>) value);
    }
}
