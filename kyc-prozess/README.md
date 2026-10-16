# KYC-Prüfprozess auf CIB seven

*English version: [README.en.md](README.en.md)*

Ausführbare Fassung von `../KYC_Pruefprozess.bpmn` auf der Prozess-Engine CIB seven 2.2.0 mit Spring Boot 3.5.14 (Java 21). Dazu gehören eine Weboberfläche zum manuellen Testen, ein Dashboard für den Ablauf einzelner Instanzen und die Werkzeuge von CIB seven (Webclient, Cockpit, Tasklist, REST-API). Die Engine-Datenbank ist H2 im Arbeitsspeicher.

## Architektur

```
Browser (http://localhost:8080)
  Antrag prüfen · Manuelle Prüfung · Dashboard (bpmn-js)      CIB seven: /webapp, /camunda/app, /engine-rest
        │                                                                │
        ▼                                                                ▼
┌────────────────────── CIB seven + Spring Boot (Port 8080) ────────────────────────┐
│ POST /kyc_review {application}                                                     │
│ Prüfung nach Regeln   Business-Rule-Task → DMN RuleCheck (R1–R5, COLLECT)          │
│        │  rule_result = green | review | reject (R5 → Ablehnung)                   │
│ Prüfung mit LLM       ${layaAssessmentService.assess(...)} ──REST──► Laya (8000)    │
│        │  llm_result.decision                                                      │
│ Gateway: green UND keine_manuelle_pruefung → Freigabe                              │
│          sonst → User Task „Manuelle Prüfung“ (Kandidatengruppe kyc-reviewers)     │
│ Entscheidung: approve → Freigabe, reject → Ablehnung,                              │
│               ungültig/fehlend → Aufgabe wird erneut erstellt                      │
│ Historie der Engine (H2) → /api/dashboard/...                                      │
└────────────────────────────────────────────────────────────────────────────────────┘
```

| Datei | Inhalt |
| --- | --- |
| `src/main/resources/KYC_Pruefprozess.bpmn` | Prozess `kyc_review` mit `camunda:`-Erweiterungen |
| `src/main/resources/KYC_RuleCheck.dmn` | Entscheidungstabelle R1–R5 (`TriggeredRules`), `RuleResult` und `RuleCheck` (beide Ergebnisse für den Prozess); R4 = USA-Bezug, R5 = bestätigter Sanktionstreffer (Ablehnung) |
| `src/main/java/de/piu/kyc/LayaAssessmentService.java` | Service-Task: ergänzt `regelpruefung`, ruft Laya, Fehler → manuelle Prüfung |
| `src/main/java/de/piu/kyc/LayaClient.java`, `LayaClientConfig.java` | REST-Client (`laya.url`, Umgebungsvariable `LAYA_URL`) |
| `src/main/java/de/piu/kyc/api/` | Prozessstart, DMN-Auswertung und Aufgaben-API für Weboberfläche und `run_demo_cases.py` |
| `src/main/java/de/piu/kyc/dashboard/` | REST-API für das Dashboard, gelesen aus der Historie der Engine |
| `src/main/java/de/piu/kyc/DemoUsers.java` | Gruppe `kyc-reviewers` mit der Prüferin `anna` für die Tasklist |
| `src/main/java/de/piu/kyc/DmnValues.java` | Wandelt FEEL-Listen in normale Java-Listen, bevor sie Prozessvariable werden |
| `src/main/resources/application.properties` | Port, H2, History-Level, JSON-Serialisierung, Admin `demo`, Laya-URL |
| `src/main/resources/META-INF/resources/` | Weboberfläche (`index.html`, `app.js`, `app.css`, bpmn-js 18.31.0 lokal unter `vendor/`) |
| `src/test/java/de/piu/kyc/KycProcessTest.java` | 9 Tests aller Pfade mit gemocktem Laya-Dienst, inkl. Knotenprotokoll, R4 und Ablehnung durch R5 |
| `run_demo_cases.py` | Ende-zu-Ende-Lauf der Demo-Fälle K01–K15 über die REST-API |
| `../laya_kyc/laya_service.py` | Laya-Dienst (FastAPI, lädt `../modelle/laya-multilingual-kyc-v2`; anderes Modell per `LAYA_MODEL`) |

Bezeichner im Code sind englisch. Die Feldnamen im Antrag (`kunde`, `screening`, …) und die Laya-Entscheidungen (`keine_manuelle_pruefung`, …) bleiben deutsch: Sie sind Teil des Datensatzschemas, mit dem Laya trainiert wurde.

Abweichungen vom Planungsentwurf:

- Zusammenführende Gateways vor „Freigabe“, „Ablehnung“ und „Manuelle Prüfung“. Sie machen im Diagramm und im Knotenprotokoll sichtbar, wo die Pfade zusammenlaufen.
- Rückweg „keine gültige Entscheidung“ vom Entscheidungs-Gateway zur manuellen Prüfung (bei `skip` oder Tippfehlern).
- Technische Fehler der LLM-Prüfung führen zu `decision = fehler` und damit zur manuellen Prüfung, wie in der Planung vorgesehen.
- Regel R4 (USA-Bezug) wurde nach dem Training ergänzt. `LayaAssessmentService` gibt nur die trainierten Regeln R1–R3 an Laya weiter; R4 erzwingt die manuelle Prüfung allein über das Regelergebnis.
- Regel R5 (Sanktionsscreening `bestaetigter_treffer`) ist der einzige Ausschlussgrund: `rule_result = reject`, Ablehnung ohne LLM-Prüfung, `llm_result` bleibt leer. Demo-Fall K15.

## Technische Hinweise zu CIB seven

- BPMN: `camunda:`-Attribute, Bedingungen in JUEL (`${rule_result == 'reject'}`).
- DMN-Aufruf: Business-Rule-Task mit `decisionRef="RuleCheck"`; die zusätzliche Entscheidung `RuleCheck` fasst `RuleResult` und `TriggeredRules` zusammen. Abhängige Entscheidungen sehen ein Ergebnis unter dem Namen der Ausgabespalte, deshalb heißt die Spalte der Regeltabelle `TriggeredRules`.
- Service-Task: Ausdruck auf eine Spring-Bean (`${layaAssessmentService.assess(...)}`).
- User Task: `TaskService` der Engine; Formularfelder `manual_decision`, `manual_comment` für die Tasklist.
- REST: eigene Controller für Weboberfläche und `run_demo_cases.py`, zusätzlich `/engine-rest`.
- Dashboard: liest die Historie der Engine (History-Level `full`).
- Variablen: JSON (Spin) in H2, lesbar in Cockpit und REST-API.
- Health: `/actuator/health/readiness`.

## Starten

Alles in einem Schritt: `../start_demo.sh` startet beide Dienste, führt `run_demo_cases.py` aus und lässt die Dienste bis Strg+C laufen (Logs unter `../logs/`; Optionen im Skriptkopf).

Einzeln:

```bash
# 1. Laya-Dienst (Terminal 1)
cd laya_kyc && ../.venv/bin/uvicorn laya_service:app --port 8000

# 2. Prozessdienst (Terminal 2)
cd kyc-prozess
mvn package                                   # Build + Tests
java -Djarmode=tools -jar target/kyc-prozess.jar extract --destination target/app
java -jar target/app/kyc-prozess.jar          # oder: mvn spring-boot:run

# 3. Browser
open http://localhost:8080
```

Das JAR wird entpackt gestartet: Liegt das Projekt in einem Pfad mit Leerzeichen (`Vertical AI CIB seven`), findet die DMN-Engine ihr XML-Schema im Fat-JAR nicht und der Start bricht ab.

Optional per Skript: `python3 run_demo_cases.py` startet K01–K15, `--decide approve` schließt die offenen Prüfungen ab.

## Weboberfläche

| Bereich | Funktion |
| --- | --- |
| Antrag prüfen | Vorlage K01–K15 wählen, Felder im Formular oder als JSON ändern, Prozess starten. Ergebnis: DMN-Regeltabelle mit Treffern, Laya-Wahrscheinlichkeiten, Pfad. |
| Manuelle Prüfung | Offene Aufgaben der Gruppe `kyc-reviewers` mit Antrag, Regeln und Laya-Ergebnis; Freigeben oder Ablehnen mit Kommentar (claim + complete). |
| Dashboard | Kennzahlen, Instanzliste (aktualisiert alle 2 s) und BPMN-Diagramm mit bpmn-js: durchlaufene Knoten und Kanten grün, wartender Knoten orange, nicht erreichte blass; Badges mit DMN- und Laya-Ergebnis am jeweiligen Task; Zeitleiste mit Dauer je Schritt. |

Das Dashboard liest aus der Historie der Engine (bis zu 1.000 Instanzen). „Leeren“ blendet die bisherigen Instanzen aus, die Historie bleibt in Cockpit sichtbar. Nach einem Neustart ist alles leer.

## Oberflächen von CIB seven

| Adresse | Inhalt | Login |
| --- | --- | --- |
| `/webapp/` | CIB seven Webclient | `demo` / `demo` |
| `/camunda/app/cockpit/` | Cockpit: Instanzen, Variablen, DMN-Auswertungen mit Trefferzeilen | `demo` / `demo` |
| `/camunda/app/tasklist/` | Tasklist: manuelle Prüfung mit Formular | `anna` / `anna` |
| `/camunda/app/admin/` | Benutzer und Gruppen | `demo` / `demo` |
| `/engine-rest/` | REST-API der Engine | – |

## REST-Aufrufe

Prozess starten. `application` entspricht `eingabe` der Trainingsdaten; `regelpruefung` berechnet der Prozess selbst und überschreibt einen mitgelieferten Wert.

```bash
curl -X POST localhost:8080/kyc_review -H 'Content-Type: application/json' -d '{"application": {...}}'
```

Die Antwort enthält `id`, `rule_result`, `triggered_rules` und `llm_result` (bei Ablehnung durch R5 `null`, weil Laya nicht aufgerufen wird). Bei automatischer Freigabe und bei Ablehnung durch Regeln ist die Instanz danach beendet (`GET /kyc_review/{id}` → 404; Verlauf weiter unter `/api/dashboard/instances/{id}`).

Manuelle Prüfung (intern `TaskService`):

```bash
Q='user=anna&group=kyc-reviewers'
curl "localhost:8080/usertasks/instance?$Q"                                   # offene Aufgaben
curl -X POST "localhost:8080/usertasks/instance/$TASK/transition?$Q" \
     -H 'Content-Type: application/json' -d '{"transitionId":"claim"}'
curl -X POST "localhost:8080/usertasks/instance/$TASK/transition?$Q" \
     -H 'Content-Type: application/json' \
     -d '{"transitionId":"complete","data":{"decision":"approve","comment":"geklärt"}}'
```

`skip` schließt die Aufgabe ohne Entscheidung ab; das Gateway erzeugt sie dann neu.

Weitere Endpunkte:

| Endpunkt | Inhalt |
| --- | --- |
| `POST /KYC_RuleCheck` mit `{"Application": {...}}` | nur die DMN-Regeln |
| `GET /api/dashboard/instances` | Instanzliste |
| `GET /api/dashboard/instances/{id}` | Verlauf mit Schritten, DMN- und Laya-Ergebnis, manueller Entscheidung |
| `GET /api/dashboard/status` | Erreichbarkeit des Laya-Dienstes |
| `/engine-rest/...` | REST-API von CIB seven, z. B. `GET /engine-rest/task?candidateGroup=kyc-reviewers` |

## Grenzen

- Prozessinstanzen, Aufgaben und Historie liegen in H2 im Arbeitsspeicher; nach einem Neustart sind sie weg.
- Keine Authentifizierung an der eigenen Oberfläche und an `/engine-rest`: Benutzer und Gruppe kommen als Query-Parameter. Die Webapps von CIB seven verlangen einen Login.
- Benutzer, Passwörter und das JWT-Secret in `application.properties` sind nur für die lokale Demo gedacht.
- Laya liefert keine Begründungen oder Feldbelege (siehe `../KYC_Trainingsergebnisse.md`).
