# Model selection for the KYC demo

*Deutsche Fassung: [KYC_Modellauswahl.md](KYC_Modellauswahl.md)*

As of 3 October 2026.

We chose **convaiinnovations/laya-multilingual** for the decision on the need for manual review. The intended hardware is a **Mac M1 with 64 GB of unified memory**. Local training was run with **PyTorch MPS** (Apple M1 Max, 64 GB): 3 epochs in 97.6 minutes using about 9.5 GB of memory. Setup, results and limits are described in [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md).

## Model and training approach

Laya Multilingual is an open decision model with about **322 million parameters** under **Apache-2.0**. It uses a multilingual mmBERT encoder and a decision head. For the German KYC texts we explicitly load the multilingual checkpoint. [Official model card](https://huggingface.co/convaiinnovations/laya-multilingual).

The project publishes a [training script for Apple Silicon](https://github.com/NandhaKishorM/laya/blob/main/notebooks/laya_finetune_typed_decisions_mps.py) with MPS, CPU fallback, small batches, gradient accumulation and gradient checkpointing. By default it uses the English checkpoint and a different example dataset. Model path, data preparation and training targets have to be adapted for Laya Multilingual and our KYC schema. [Project and training documentation](https://github.com/NandhaKishorM/laya#fine-tuning).

64 GB of RAM made a first local training attempt with this compact model look feasible. That planning estimate does not replace a measurement: memory use and runtime depend on input lengths, batch size and the parameters being trained. We start with small batches. A first attempt can fine-tune only the decision head and freeze the encoder; if needed, we include the encoder with a small learning rate. The scripts have to be adapted for this. LoRA is not required for this first attempt.

By default Laya Multilingual has an input limit of **1,024 tokens**; according to the model card, the encoder supports up to **8,192 tokens**. Before training we measure the actual lengths including question and answer options. We use a compact, deterministic rendering of all relevant input fields. If necessary, we raise the limit after a memory and compatibility test. Relevant fields and documents must not be silently truncated. [Context limits and accuracy on long documents](https://huggingface.co/convaiinnovations/laya-multilingual).

## Decisions and justifications

The three answer options match the existing dataset:

- `keine_manuelle_pruefung` (no manual review)
- `manuelle_pruefung` (manual review)
- `unklar` (unclear)

`unklar` leads to manual review. Rule triggers R1–R4 force this forwarding regardless of the model's decision (R4 US nexus only in the process, not in training). R5 (confirmed sanctions hit) rejects in the process before the model is called; rejection is not one of the model's answer options. Only `eingabe` (input) is used as case content; metadata and target assessments stay outside the model input. The fictional demo policy is given as a shared task description.

For supervised fine-tuning, `sollbewertung.entscheidung` (target decision) is converted into a class index or a one-hot target distribution over these three options. The target distribution describes the label, not a measured certainty.

Laya does not generate free-text justifications or field references. This part of the original demo plan needs an additional component: further structured questions about review reasons and evidence candidates, or a separate text generation step with a language model. Both have to be checked against the actual input. The target justification must not be shown in production as if it were the model's justification. Model probabilities are logged for evaluation for now.

## Training and comparison

We first compare the unchanged base model with the fine-tuned variant: 4,000 training cases, 500 validation cases and 500 test cases. The existing families stay separated. We evaluate missed review needs, unnecessary forwarding and correct field references. The test split has only five examples of the 50 address cases; more independently worded address cases and counter-examples are needed for a reliable assessment.

Probabilities are calibrated only on held-out validation data. The final test is used neither for training nor for setting thresholds. The model card explicitly states that the multilingual checkpoint is overconfident and not calibrated out of the box. [Model limits](https://huggingface.co/convaiinnovations/laya-multilingual#limits).

## Alternative considered

**Qwen3.5-4B with LLM2Jev** remains a candidate for comparison in case the compact decision model is too weak on our data or we also need free-text generation. The preferred option for the first local KYC attempt is Laya Multilingual. [Qwen model card](https://huggingface.co/Qwen/Qwen3.5-4B), [LLM2Jev](https://github.com/Yinsongxu/LLM2Jev).
