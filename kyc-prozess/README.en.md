# KYC review process on CIB seven

*Deutsche Fassung: [README.md](README.md)*

Executable version of `../KYC_Pruefprozess.bpmn` on the CIB seven 2.2.0 process engine with Spring Boot 3.5.14 (Java 21). It comes with a web UI for manual testing, a dashboard showing the flow of individual instances and the CIB seven tools (web client, Cockpit, Tasklist, REST API). The engine database is in-memory H2. The web UI is in German.

The web UI, the Laya integration, the DMN rules and `run_demo_cases.py` match the Kogito version in the sibling project `Vertical AI`. The service offers the same REST paths for this.

## Architecture

```
Browser (http://localhost:8080)
  Check application · Manual review · Dashboard (bpmn-js)     CIB seven: /webapp, /camunda/app, /engine-rest
        │                                                                │
        ▼                                                                ▼
┌────────────────────── CIB seven + Spring Boot (port 8080) ────────────────────────┐
│ POST /kyc_review {application}                                                     │
│ Rule check        business rule task → DMN RuleCheck (R1–R5, COLLECT)              │
│        │  rule_result = green | review | reject (R5 → rejection)                   │
│ LLM check         ${layaAssessmentService.assess(...)} ──REST──► Laya (8000)        │
│        │  llm_result.decision                                                      │
│ Gateway: green AND keine_manuelle_pruefung → approval                              │
│          otherwise → user task "Manuelle Prüfung" (candidate group kyc-reviewers)  │
│ Decision: approve → approval, reject → rejection,                                  │
│           invalid/missing → task is created again                                  │
│ Engine history (H2) → /api/dashboard/...                                           │
└────────────────────────────────────────────────────────────────────────────────────┘
```

| File | Content |
| --- | --- |
| `src/main/resources/KYC_Pruefprozess.bpmn` | Process `kyc_review` with `camunda:` extensions; diagram and element IDs as in the Kogito version |
| `src/main/resources/KYC_RuleCheck.dmn` | Decision table R1–R5 (`TriggeredRules`), `RuleResult` and `RuleCheck` (both results for the process); R4 = US nexus, R5 = confirmed sanctions hit (rejection) |
| `src/main/java/de/piu/kyc/LayaAssessmentService.java` | Service task: adds `regelpruefung` (rule check), calls Laya, errors → manual review |
| `src/main/java/de/piu/kyc/LayaClient.java`, `LayaClientConfig.java` | REST client (`laya.url`, environment variable `LAYA_URL`) |
| `src/main/java/de/piu/kyc/api/` | Process start, DMN evaluation and task API with the paths of the Kogito version |
| `src/main/java/de/piu/kyc/dashboard/` | REST API for the dashboard, read from the engine history |
| `src/main/java/de/piu/kyc/DemoUsers.java` | Group `kyc-reviewers` with the reviewer `anna` for the Tasklist |
| `src/main/java/de/piu/kyc/DmnValues.java` | Turns FEEL lists into plain Java lists before they become process variables |
| `src/main/resources/application.properties` | Port, H2, history level, JSON serialization, admin `demo`, Laya URL |
| `src/main/resources/META-INF/resources/` | Web UI (`index.html`, `app.js`, `app.css`, bpmn-js 18.31.0 bundled under `vendor/`) |
| `src/test/java/de/piu/kyc/KycProcessTest.java` | 9 tests covering all paths with a mocked Laya service, including node log, R4 and rejection by R5 |
| `run_demo_cases.py` | End-to-end run of demo cases K01–K15 via the REST API |
| `../laya_kyc/laya_service.py` | Laya service (FastAPI, loads `../modelle/laya-multilingual-kyc-v2`; another model via `LAYA_MODEL`) |

Identifiers in the code are English. The field names in the application (`kunde` = customer, `screening`, …) and the Laya decisions (`keine_manuelle_pruefung` = no manual review, …) stay German: they are part of the dataset schema Laya was trained on.

Deviations from the planning draft (as in the Kogito version):

- Merging gateways before "Approval", "Rejection" and "Manual review". CIB seven would not need them, but they stay so that the diagram and node log are the same in both versions.
- A loop back "no valid decision" from the decision gateway to the manual review (on `skip` or typos).
- Technical errors in the LLM check lead to `decision = fehler` (error) and therefore to manual review, as planned.
- Rule R4 (US nexus) was added after training. `LayaAssessmentService` passes only the trained rules R1–R3 to Laya; R4 forces manual review through the rule result alone.
- Rule R5 (sanctions screening `bestaetigter_treffer`, confirmed hit) is the only exclusion reason: `rule_result = reject`, rejection without LLM check, `llm_result` stays empty. Demo case K15.

## Differences from the Kogito version

| Topic | Kogito 10.2 (Quarkus) | CIB seven 2.2 (Spring Boot) |
| --- | --- | --- |
| BPMN extensions | `drools:` attributes, Java conditions | `camunda:` attributes, JUEL conditions (`${rule_result == 'reject'}`) |
| DMN call | Business rule task with namespace and model, returns all decisions | Business rule task with `decisionRef="RuleCheck"`; an extra decision `RuleCheck` combines `RuleResult` and `TriggeredRules` |
| DMN table | Result is named after the decision | Dependent decisions see the result under the output column name; the column is therefore called `TriggeredRules` |
| Service task | Java interface in the BPMN | Expression on a Spring bean |
| User task | Kogito user task API | Engine `TaskService`; form fields `manual_decision`, `manual_comment` for the Tasklist |
| REST endpoints | Generated from the process | Own controllers with the same paths; additionally `/engine-rest` |
| Dashboard history | Own event listener, because Kogito forgets finished instances immediately | Engine history (history level `full`) |
| Variables | Java objects in memory | JSON (Spin) in H2, readable in Cockpit and the REST API |
| Start | `java -jar target/quarkus-app/quarkus-run.jar` | Run `target/kyc-prozess.jar` extracted, see below |
| Health | `/q/health/ready` | `/actuator/health/readiness` |

## Starting

All in one step: `../start_demo.sh` starts both services, runs `run_demo_cases.py` and keeps the services running until Ctrl+C (logs under `../logs/`; options in the script header). See [Quickstart.en.md](../Quickstart.en.md).

Separately:

```bash
# 1. Laya service (terminal 1)
cd laya_kyc && ../.venv/bin/uvicorn laya_service:app --port 8000

# 2. Process service (terminal 2)
cd kyc-prozess
mvn package                                   # build + tests
java -Djarmode=tools -jar target/kyc-prozess.jar extract --destination target/app
java -jar target/app/kyc-prozess.jar          # or: mvn spring-boot:run

# 3. Browser
open http://localhost:8080
```

The JAR is run extracted: if the project lives in a path containing spaces (`Vertical AI CIB seven`), the DMN engine cannot find its XML schema inside the fat JAR and startup fails.

Optional via script: `python3 run_demo_cases.py` starts K01–K15, `--decide approve` completes the open reviews.

## Web UI

| Area | Function |
| --- | --- |
| Antrag prüfen (check application) | Pick a template K01–K15, change fields in the form or as JSON, start the process. Result: DMN rule table with hits, Laya probabilities, path. |
| Manuelle Prüfung (manual review) | Open tasks of the group `kyc-reviewers` with application, rules and Laya result; approve or reject with a comment (claim + complete). |
| Dashboard | Key figures, instance list (refreshed every 2 s) and BPMN diagram with bpmn-js: completed nodes and edges green, waiting node orange, unreached ones faded; badges with DMN and Laya results on each task; timeline with duration per step. |

The dashboard reads from the engine history (up to 1,000 instances). „Leeren“ (clear) hides the instances so far; the history stays visible in Cockpit. After a restart everything is empty.

## CIB seven user interfaces

| Address | Content | Login |
| --- | --- | --- |
| `/webapp/` | CIB seven web client | `demo` / `demo` |
| `/camunda/app/cockpit/` | Cockpit: instances, variables, DMN evaluations with matched rows | `demo` / `demo` |
| `/camunda/app/tasklist/` | Tasklist: manual review with a form | `anna` / `anna` |
| `/camunda/app/admin/` | Users and groups | `demo` / `demo` |
| `/engine-rest/` | Engine REST API | – |

## REST calls

Start a process. `application` corresponds to `eingabe` (input) of the training data; the process computes `regelpruefung` (rule check) itself and overwrites a supplied value.

```bash
curl -X POST localhost:8080/kyc_review -H 'Content-Type: application/json' -d '{"application": {...}}'
```

The response contains `id`, `rule_result`, `triggered_rules` and `llm_result` (`null` on rejection by R5, because Laya is not called). After automatic approval and after rejection by rules, the instance is finished (`GET /kyc_review/{id}` → 404; the history remains under `/api/dashboard/instances/{id}`).

Manual review (same interface as the Kogito version, internally `TaskService`):

```bash
Q='user=anna&group=kyc-reviewers'
curl "localhost:8080/usertasks/instance?$Q"                                   # open tasks
curl -X POST "localhost:8080/usertasks/instance/$TASK/transition?$Q" \
     -H 'Content-Type: application/json' -d '{"transitionId":"claim"}'
curl -X POST "localhost:8080/usertasks/instance/$TASK/transition?$Q" \
     -H 'Content-Type: application/json' \
     -d '{"transitionId":"complete","data":{"decision":"approve","comment":"clarified"}}'
```

`skip` completes the task without a decision; the gateway then creates it again.

More endpoints:

| Endpoint | Content |
| --- | --- |
| `POST /KYC_RuleCheck` with `{"Application": {...}}` | DMN rules only |
| `GET /api/dashboard/instances` | Instance list |
| `GET /api/dashboard/instances/{id}` | History with steps, DMN and Laya results, manual decision |
| `GET /api/dashboard/status` | Reachability of the Laya service |
| `/engine-rest/...` | CIB seven REST API, e.g. `GET /engine-rest/task?candidateGroup=kyc-reviewers` |

## Limits

- Process instances, tasks and history live in in-memory H2; they are gone after a restart.
- No authentication on the custom UI and on `/engine-rest`: user and group are passed as query parameters. The CIB seven webapps require a login.
- Users, passwords and the JWT secret in `application.properties` are meant for the local demo only.
- Laya provides no justifications or field references (see [../KYC_Trainingsergebnisse.en.md](../KYC_Trainingsergebnisse.en.md)).
