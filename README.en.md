# Vertical AI: KYC review with BPMN, rules and a locally trained decision model

*Deutsche Fassung: [README.md](README.md)*

Talk and demo: a KYC review process at bank account opening combines DMN rules, the locally trained decision model Laya Multilingual and a manual review as a user task. The process runs on the CIB seven 2.2 process engine (Spring Boot). All data is fully synthetic and the bank policy is fictional.

Field names and values in the dataset (`kunde`, `eingabe`, `keine_manuelle_pruefung`, …) are German, because they are part of the schema the model was trained on. The English documents explain them where they appear.

## Contents

| Path | Content |
| --- | --- |
| [Quickstart.en.md](Quickstart.en.md) | Start and present the demo (`start_demo.sh`) |
| [KYC_Demo_Planung.en.md](KYC_Demo_Planung.en.md) | Process, rules R1–R5, demo cases, talk schedule |
| [KYC_Beispieldaten.en.md](KYC_Beispieldaten.en.md) | Demo customers K01–K15 |
| [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md) | Training, results, shortcuts the model learned |
| [KYC_Modellauswahl.en.md](KYC_Modellauswahl.en.md) | Why Laya Multilingual |
| [KYC_Trainingsdaten_README.en.md](KYC_Trainingsdaten_README.en.md) | Synthetic training data (5,000 and 6,000 cases) |
| `KYC_Pruefprozess.bpmn` | Business process draft |
| `laya_kyc/` | Training, evaluation, counterfactual probes, Laya REST service |
| `kyc-prozess/` | CIB seven process service with web UI, see [kyc-prozess/README.en.md](kyc-prozess/README.en.md) |
| `praesentation/` | Slide deck `Vertical_AI_KYC.pptx` (German) with images and logos |

## Clone and start

```bash
git clone https://github.com/Marcin-Pankowski/Vertical-Ai_CibSeven.git
cd Vertical-Ai_CibSeven
./start_demo.sh
```

On first start, `start_demo.sh` downloads the demo model v2 as a ZIP from the release [modelle](https://github.com/Marcin-Pankowski/Vertical-Ai_CibSeven/releases/tag/modelle) (about 600 MB), creates the Python environment `.venv/` from `requirements.txt` (downloads PyTorch, a few minutes) and builds the process service with Maven (Java 21). Details: [Quickstart.en.md](Quickstart.en.md).

## Models

The models are too large for Git and are published as ZIP files in the release [modelle](https://github.com/Marcin-Pankowski/Vertical-Ai_CibSeven/releases/tag/modelle). Unzip them in the project folder (`unzip <file>.zip`); they end up under `modelle/`.

| ZIP | Content | Needed for |
| --- | --- | --- |
| `laya-multilingual-kyc-v2.zip` | Model v2, active in the Laya service | Demo (`start_demo.sh` downloads it automatically) |
| `laya-multilingual-kyc.zip` | Model v1 | Comparisons and counterfactual probes |
| `laya-multilingual-base.zip` | Base model `convaiinnovations/laya-multilingual` (Apache-2.0) | Training, comparison without fine-tuning |

The training logs are in the repository under `modelle/*_arbeit/`. The release does not contain the epoch-1 checkpoint from which v2 was trained further.

`.venv/` and `kyc-prozess/target/` are not in the repository either; `start_demo.sh` creates both.
