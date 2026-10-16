# Quickstart: running the KYC demo

*Deutsche Fassung: [Quickstart.md](Quickstart.md)*

`start_demo.sh` starts the Laya service and the CIB seven process service, runs the demo cases K01–K15 and opens the dashboard. The services keep running until you stop them with Ctrl+C.

## Prerequisites

- Python 3.12 or newer; the script creates the environment `.venv/` from `requirements.txt` on first start
- Java 21 and Maven (the process service is built on first start)
- Model `modelle/laya-multilingual-kyc-v2/`; if it is missing, the script downloads it on first start as a ZIP from the GitHub release `modelle` (about 600 MB)
- Ports 8000 (Laya) and 8080 (CIB seven) free

## Starting

```bash
cd Vertical-Ai_CibSeven    # or the project folder
./start_demo.sh
```

What happens:

1. If the model or `.venv/` is missing, it is downloaded or created (once, a few minutes).
2. If the process service has not been built yet, the script builds it first (`mvn package` with tests, about 1 minute).
3. Laya loads the model. This takes about 40 seconds; the script prints dots meanwhile.
4. CIB seven is ready after about 10 seconds.
5. Cases K01–K15 are started, one output line per case.
6. The browser opens the dashboard at http://localhost:8080/#dashboard.
7. The terminal waits. **Keep it open** while the demo runs.

Variants:

| Command | Effect |
| --- | --- |
| `./start_demo.sh` | Start services, run demo cases, open reviews stay open |
| `NO_CASES=1 ./start_demo.sh` | Start services only, no cases; start cases live in the web UI |
| `./start_demo.sh --decide approve` | After running the cases, approve all open manual reviews (`--decide reject` rejects them) |
| `BUILD=1 ./start_demo.sh` | Rebuild first, e.g. after changes to DMN, BPMN or the web UI |

For a talk, `NO_CASES=1` is usually better: the dashboard starts empty and each case appears when you present it.

## Reading the demo output

```
K06  Regeln review R4     Laya keine_manuelle_pruefung  p(keine)=1.00  ->  manuelle Prüfung (offen)   ✓
K15  Regeln reject R5     Laya -                        p(keine)=0.00  ->  Ablehnung (Regeln)         ✓
14/15 Fälle mit erwartetem Endpfad
```

The script output is German:

- **Regeln** (rules): `green` (no rule), `review` (R1–R4, manual review) or `reject` (R5, rejection)
- **Laya**: the model's decision (`keine_manuelle_pruefung` = no manual review, `manuelle_pruefung` = manual review, `unklar` = unclear); `-` for K15, because R5 rejects before Laya is called (the `p(keine)` value is meaningless there)
- **Path**: `Freigabe (automatisch)` = automatic approval, `manuelle Prüfung (offen)` = manual review (open), `Ablehnung (Regeln)` = rejection by rules
- **✓/✗**: comparison with the expected path. 14 of 15 is expected: K10 is a known false approval (see [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md)).

## Working with the web UI

http://localhost:8080 has three areas (the UI is in German):

**Antrag prüfen** (check application): pick a template K01–K15, change fields in the form or as JSON, click „Prüfung starten“. The result shows the DMN rule table with hits, the Laya probabilities and the path. Good for presenting: change one value and check again, e.g. set the sanctions screening of K01 to `bestaetigter_treffer` (confirmed hit).

**Manuelle Prüfung** (manual review): open tasks of the group `kyc-reviewers` with application, rules and Laya result. Enter a comment, then „Freigeben“ (approve) or „Ablehnen“ (reject).

**Dashboard**: key figures, instance list (refreshed every 2 seconds) and the BPMN diagram of the selected instance: completed steps green, waiting step orange, badges with DMN and Laya results, timeline. „Leeren“ hides the instances so far; they remain in the engine history.

## CIB seven user interfaces

The same instances and tasks are also visible in the engine's own tools:

| Address | Content | Login |
| --- | --- | --- |
| http://localhost:8080/webapp/ | CIB seven web client: processes, tasks, decisions | `demo` / `demo` |
| http://localhost:8080/camunda/app/cockpit/ | Cockpit: running and finished instances, DMN evaluations with matched rows | `demo` / `demo` |
| http://localhost:8080/camunda/app/tasklist/ | Tasklist: manual review with a form (decision, comment) | `anna` / `anna` (group `kyc-reviewers`) |
| http://localhost:8080/engine-rest/ | Engine REST API | – |

A review decided in the Tasklist shows up in the dashboard of the custom UI right away, and vice versa.

## Suggested live demo

Same order as on the "Live-Demo" slide:

| Case | What you see |
| --- | --- |
| K01 | Rules green, Laya OK → automatic approval |
| K11 | Private account with planned business use; Laya sends it to manual review |
| K06 | Laya says OK, rule R4 (US nexus) still forces a manual review |
| K15 | Confirmed sanctions hit; R5 rejects immediately, Laya is not asked |
| K13 / K14 | Home address on the Reeperbahn → manual; only the employer there → approval |
| – | Decide open cases under "Manuelle Prüfung", show the flow in the dashboard |

## Stopping

Ctrl+C in the terminal running `start_demo.sh`. The script stops both services. The browser tab stays open but no longer loads anything. All instances live in memory only and are gone after a restart.

## Troubleshooting

| Message | Fix |
| --- | --- |
| `Port 8000 ist schon belegt` (or 8080) | An old instance is still running: `lsof -ti tcp:8000 -sTCP:LISTEN \| xargs kill` (or 8080) |
| Model download fails | Download the ZIP from the release page `modelle` and unzip it in the project folder (`unzip laya-multilingual-kyc-v2.zip`) |
| `Warte auf Laya … ✗ abgebrochen` | Check `logs/laya.log` |
| `Warte auf CIB seven … ✗` | Check `logs/cibseven.log`; check the Java version (`java -version`, 21 is required) |
| Laya result `fehler` (error) in the UI | Laya service not reachable; the case correctly goes to manual review |
| Changes to DMN/BPMN/UI not visible | `BUILD=1 ./start_demo.sh` |

More details: [kyc-prozess/README.en.md](kyc-prozess/README.en.md) (process service, REST API), [KYC_Demo_Planung.en.md](KYC_Demo_Planung.en.md) (cases and rules), [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md) (model).
