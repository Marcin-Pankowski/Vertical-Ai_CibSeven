# Training and results: Laya Multilingual for the KYC demo

*Deutsche Fassung: [KYC_Trainingsergebnisse.md](KYC_Trainingsergebnisse.md)*

As of 3 October 2026. All data is synthetic; the target assessments follow the fictional demo policy.

The model's three answers are `keine_manuelle_pruefung` (no manual review), `manuelle_pruefung` (manual review) and `unklar` (unclear).

## Summary

| Model | Test split (500 cases): approved automatically by mistake | Test split: sent to manual review unnecessarily | Demo K01–K11 correct end path |
| --- | ---: | ---: | ---: |
| Laya Multilingual without fine-tuning | 5 | 246 of 250 | 5 of 11 |
| Laya Multilingual after fine-tuning | 0 | 0 | 10 of 11 |

Training changes the behaviour substantially: without fine-tuning, the model sends almost every case to manual review. After fine-tuning, the synthetic test split is error-free. This result mainly shows that the model has learned the dataset's wording templates. The differently worded demo cases show the limit: K10 is approved automatically with a displayed confidence of 1.00, although proof for the gift is missing.

## Setup

| Item | Implementation |
| --- | --- |
| Base model | `convaiinnovations/laya-multilingual` (mmBERT encoder, ~322M parameters, Apache-2.0), package `laya` 0.3.24 |
| Hardware | Apple M1 Max, 64 GB, PyTorch MPS |
| Data | `KYC_Trainingsdaten_5000.json`: 4,000 training, 500 validation, 500 test (SHA-256 `1527a5ea…ac28`) |
| Input | Only `eingabe` (input), as `path: value` lines; the paths match the evidence paths. The constant fields `prozessphase` and `identitaetspruefung` are left out. A compact version of the demo policy precedes them. |
| Question | Laya `choice` with `keine_manuelle_pruefung`, `manuelle_pruefung`, `unklar` |
| Length | Training data 804–1,017 tokens with a limit of 1,024; no case is truncated (otherwise the check aborts). K02 and K06 are longer at 1,035 and 1,039 tokens and are scored with a raised limit in the demo, not shortened. |
| Method | Full fine-tuning of encoder and decision head following the official MPS script: cross-entropy plus an RL term with `proper_reward`, AdamW (encoder 2.5e-5, head 1e-4), cosine schedule, gradient checkpointing, effective batch size 32 |
| Runtime | 3 epochs in 97.6 minutes, about 9.5 GB MPS memory |
| Selection | Best epoch by path errors on validation: epoch 2 |
| Calibration | Temperature fitted on validation; with error-free validation it stays at 1.0. The probabilities are therefore effectively **not calibrated**. |
| Test | Evaluated once after training, not used for selection |

Progress on validation:

| Epoch | Training CE | Accuracy, 3 classes | approved by mistake | unnecessarily manual |
| ---: | ---: | ---: | ---: | ---: |
| 0 (base) | – | 0.460 | 7 | 245 |
| 1 | 0.448 | 0.988 | 0 | 6 |
| 2 | 0.013 | 1.000 | 0 | 0 |
| 3 | 0.000 | 1.000 | 0 | 0 |

## Results on the test split

BPMN path: automatic approval only with a green rule result and the Laya decision `keine_manuelle_pruefung`. Among the 425 rule-green test cases:

| Model | correctly approved | correctly forwarded | approved by mistake | unnecessarily manual |
| --- | ---: | ---: | ---: | ---: |
| without fine-tuning | 4 | 170 | 5 | 246 |
| after fine-tuning | 250 | 175 | 0 | 0 |

Without fine-tuning, the model answers `manuelle_pruefung` in 472 of 500 test cases. The fine-tuned variant matches all 500 target decisions, including the 15 `unklar` cases.

## Demo cases K01–K11 (model v1)

The eleven cases are in `laya_kyc/demo_cases.py`. Master data and free texts come from [KYC_Beispieldaten.en.md](KYC_Beispieldaten.en.md); missing amounts, account purposes and document excerpts were added synthetically. Their wording partly differs from the training templates.

| Case | Expected | without fine-tuning | after fine-tuning |
| --- | --- | --- | --- |
| K01, K02, K05, K06, K08 | Approval | manual (unnecessary) | Approval |
| K03, K11 | manual | manual | manual |
| K04, K07, K09 | Rule → manual | manual | manual |
| K10 | unclear → manual | **Approval** | **Approval (1.00)** |

K10 is the most important finding. In the training data, all 200 documented gifts have a document and the wording „Schenkung meiner Eltern“ ("gift from my parents"); all 175 open gifts have no document and a template with „möchte ich nicht weiter erläutern“ ("I would rather not explain further"). K10 mentions „eine Schenkung meiner Eltern“ without a document. The model approves. Replacing just one free text in K10 with the training wording flips the decision:

| K10 variant | none | manual | unclear |
| --- | ---: | ---: | ---: |
| Original | 1.00 | 0.00 | 0.00 |
| Source-of-funds text as in the training template | 0.00 | 1.00 | 0.00 |
| Wealth text as in the training template | 0.00 | 0.00 | 1.00 |

Here the model has learned the wording, not the missing proof. The displayed confidence is not a reliable statement about correctness.

## Sensitivity to field order

When connecting the model to Kogito, we noticed that it reacts strongly to the order of the input lines. With the same information in randomly shuffled JSON field order, the decision changed for 8 of the 11 demo cases in at least one of three variants; K03 would then have been approved automatically. All training cases have the same field order, so the model has also learned the position of the lines.

This is fixed in the input rendering (`laya_kyc/kyc_data.py`, `FIELD_ORDER`): the lines are always output in the order of the training data, regardless of the order of the incoming JSON. For all 5,000 training cases and K01–K11 the rendering is unchanged; the figures above remain valid. After the fix, 55 shuffled variants of the demo cases show no deviation. A more robust model would be trained with varying field order.

## Version 2: decoupled data (active model)

As of 4 October 2026. Cases entered during the workshop revealed a second shortcut: Karl Jones and Tim Uphaus live on the Reeperbahn and were still approved automatically. Counterfactual probes show the cause: all 50 Reeperbahn cases in the dataset were self-employed customers with a savings confirmation („Rücklagenbestätigung“), and no other case had this document. Model v1 learned the document type, not the home address.

| Counterfactual probe on test cases (one feature changed each) | v1 | v2 |
| --- | ---: | ---: |
| Home address moved into the Reeperbahn area (target: manual) | 0/250 | 250/250 |
| Only the employer moved into the Reeperbahn area (target: none) | 185/185 | 185/185 |
| Address case moved to an unremarkable home address (target: none) | 0/15 | 15/15 |
| Unchanged cases (target as in the dataset) | 255/255 | 255/255 |

The unchanged test cases were error-free with v1. That is why the shortcut did not show up in the normal test evaluation; only the counterfactual probes reveal it.

**Data:** `KYC_Trainingsdaten_6000.json` adds 1,000 derived variants to the original dataset (`laya_kyc/augment_dataset.py`). Each variant stays in the family and split of its template; the original stays unchanged.

| New variant | Count | Target |
| --- | ---: | --- |
| Home address in the Reeperbahn area for otherwise unremarkable cases of all kinds | 400 | manual |
| Previous address cases at an unremarkable home address (including postcode 20359 without a listed street) | 150 | none |
| Self-employed with a matching savings confirmation | 150 | none |
| Only the employer in the Reeperbahn area | 100 | none |
| "Self-employed", but salary savings and an employer confirmation | 200 | manual |

**Training:** a first run with the v1 method became unstable in epoch 2 (training loss rose from 0.70 to above 1.2). Training continued from its state after epoch 1 (about 36 minutes), with pure cross-entropy without the RL term (`--ce-only`) and a lower learning rate (encoder 1e-5, head 5e-5), 2 epochs in 66 minutes. Model: `modelle/laya-multilingual-kyc-v2`.

| Test split | v1: approved by mistake | v1: unnecessarily manual | v2: approved by mistake | v2: unnecessarily manual |
| --- | ---: | ---: | ---: | ---: |
| new dataset (621 cases) | 75 | 29 | 0 | 0 |
| original dataset (500 cases) | 0 | 0 | 0 | 0 |

Demo cases K01–K14 (process with rules R1–R4): v1 11 of 14, v2 13 of 14. v2 fixes K12 (Tim Uphaus) and K13 (Karl Jones) and still approves K14 (Marcin Pankowski, only the employer on the Reeperbahn). **K10 stays wrong**: the "gift from my parents" shortcut was not decoupled.

Addendum 5 October 2026: with K15 (confirmed sanctions hit, rule R5) there are 15 demo cases. K15 is rejected by R5 in the process before Laya is called; the model plays no role. End to end with v2 (first via Kogito, now via CIB seven): 14 of 15, only K10 is still wrong.

The lesson for the talk: an error-free test on template-based data proves little. Targeted counterfactual probes that change exactly one feature show what the model actually reacts to. Decoupled training data fixes a shortcut, but only the one you have found.

## Assessment and limits

- Training clearly helps with this task: the base model does not know the fictional policy and is practically useless for automatic approval.
- 100 % on the test split does not prove quality on real customer cases. The splits separate families, but the same templates appear in all splits.
- An independently worded, expert-reviewed test dataset is required before any further claim. K01–K11 are a first, very small step in that direction.
- More variation in the training data would be the next step, especially documented and undocumented gifts with the same wording that differ only in the proof.
- Meaningful calibration needs validation data on which the model makes mistakes.
- Laya does not provide justifications or field references. An additional component is still required for that.
- Rule triggers R1–R4 force a manual review regardless of the model; the model cannot override them. R4 (US nexus) was introduced after training; the model itself still assesses US cases according to the original policy.
- R5 (confirmed sanctions hit) rejects before the model is asked. The model does not know the screening status "confirmed hit" from training; in the process, it never receives such cases.

## Files and commands

| Path | Content |
| --- | --- |
| `modelle/laya-multilingual-base/` | Base model without fine-tuning |
| `modelle/laya-multilingual-kyc/` | Model v1; training details in `rl_agent_config.json` under `kyc_training` |
| `modelle/laya-multilingual-kyc-v2/` | Model v2 (active in the Laya service), trained on `KYC_Trainingsdaten_6000.json` |
| `laya_kyc/augment_dataset.py` | Creates `KYC_Trainingsdaten_6000.json` and its validation report |
| `laya_kyc/kyc_counterfactuals.py` | Counterfactual probes: one feature changed each |
| `modelle/laya-multilingual-kyc_arbeit/training.log` | Full training log |
| `laya_kyc/kyc_data.py` | Input rendering, policy text, question, truncation check |
| `laya_kyc/kyc_train.py` | Training on MPS |
| `laya_kyc/kyc_evaluate.py` | Evaluation per split, cross-checked via the Laya API |
| `laya_kyc/kyc_demo.py`, `laya_kyc/demo_cases.py` | Demo without a process engine, cases K01–K15 (K15: rejection by R5) |
| `laya_kyc/laya_service.py` | REST service for the process (`POST /assessment`) |
| `laya_kyc/ergebnisse/` | Metrics and individual predictions per model and split |

The models are not in the repository; download them from the release `modelle` (see [README.en.md](README.en.md)).

```bash
cd laya_kyc
../.venv/bin/python kyc_demo.py --compare               # demo: base and fine-tuned
../.venv/bin/python kyc_demo.py --case K11 --show-input
../.venv/bin/python kyc_evaluate.py --model ../modelle/laya-multilingual-kyc-v2 --split test --name v2 --data ../KYC_Trainingsdaten_6000.json
../.venv/bin/python kyc_counterfactuals.py --model ../modelle/laya-multilingual-kyc-v2 --name v2
../.venv/bin/python kyc_train.py --epochs 3               # repeat training (about 1.5–2 hours)
```
