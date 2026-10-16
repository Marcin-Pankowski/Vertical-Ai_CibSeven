# Plan: KYC with rules, LLM and manual review

*Deutsche Fassung: [KYC_Demo_Planung.md](KYC_Demo_Planung.md)*

As of 3 October 2026, updated 5 October 2026 (rule R5, test cases K12–K15). Goal: a 30-minute talk with demo, followed by 15 minutes of questions.

Implementation has been approved; training, evaluation and demo scripts are in `laya_kyc/`, the results in [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md). The process runs on CIB seven 2.2.0 (Spring Boot) in `kyc-prozess/` (rules as DMN, Laya as a REST service, manual review as a user task). The BPMN file is a business process draft. The rules and case assessments below describe a fictional bank policy for the demo; they do not represent complete regulatory requirements.

In this document, "LLM" refers to the decision model in the process; in the implementation this is Laya Multilingual, an encoder-based decision model rather than a text generator.

## 1. Scope and process

The starting point is synthetic private customers onboarding at a German bank after a completed, successful identity verification. Only data from such customers is used for demo, training and evaluation. Identity verification takes place before the BPMN process starts and is not a decision rule of this process. Corporate customers and ownership structures remain a possible extension.

All cases are assessed directly after successful identity verification and before the account is opened. The basis is customer information from the application, screening results and any documents already submitted. "Approval" means approval of the requested account opening. K11 shows a contradiction between a requested private account and a customer free text about planned business use.

1. The process starts with KYC data of a customer who has already been identified successfully; the rule check follows.
2. A clear rule-based rejection leads to the end event "Rejection".
3. A green rule result or a rule result requiring review leads to the LLM check. Green cases are checked by the LLM as well.
4. The LLM assesses, based on the information and documents, whether a manual review is required.
5. Automatic approval requires a green rule result and a clear LLM result "no manual review required".
6. A need for manual clarification or an unclear LLM result leads to a manual review. A review need found by the rules also stays with manual review.
7. The manual review ends with approval or rejection after clarification.

Technical errors and invalid model output lead to manual review. The BPMN draft in the project root does not model these error paths explicitly; the process service implements them (`decision = fehler` → manual review, see [kyc-prozess/README.en.md](kyc-prozess/README.en.md)).

## 2. Proposed demo rules

| ID | Check | Result when triggered |
| --- | --- | --- |
| R1 | Does the customer state that they are not the beneficial owner (answer "No")? | Review required; manual specialist review |
| R2 | Is the PEP status (politically exposed person) confirmed? | Review required; manual specialist review |
| R3 | Is there a possible, still unresolved screening hit? | Review required; check the possible hit manually |
| R4 | Is there a US nexus (US tax liability or FATCA US person according to self-certification, US citizenship, US tax residence, or a home or foreign address in the US)? | Review required; manual review of the US nexus |
| R5 | Is there a confirmed sanctions hit? | Rejection; no LLM check |

The field "customer is the beneficial owner" holds the customer's answer "Ja" (yes) or "Nein" (no). A third value is not intended. Under our fictional demo policy, "No" triggers R1; it is not an automatic rejection.

"Green" means: no demo rule requires a manual review. "Review required" means: at least one demo rule requires a manual review. "Rejection" means: a confirmed exclusion reason exists.

R4 was added after training and is only evaluated in the process (DMN). From training, Laya knows only R1–R3 and keeps receiving only these in the rule result; the training data still treats a consistently declared US status as unremarkable. Since every rule trigger forces a manual review, the model's verdict does not change that.

R1–R4 only mark a need for manual review. The only exclusion reason is R5, a confirmed sanctions hit: the application ends directly in rejection and Laya is not called. Laya does not know this screening status from training; that is not a problem, because such a case never reaches the model. A confirmed PEP status is a trigger for specialist review here, not an automatic rejection. The additional test case K15 shows the rejection path (see below).

We do not set income limits or fixed amount thresholds for now. The interesting part for the LLM is the consistency between the customer's explanation, economic profile and documents, and between the stated account purpose and the declared planned use.

## 3. The LLM's task

The LLM receives the structured KYC case, the rule result including open findings, and text excerpts from synthetic documents. For K11 it receives the requested account type, the selected private account purpose and a free text about planned use. It should answer:

> Are the information and documents sufficiently consistent and plausible, or does a person need to review the case?

The answer contains:

- Decision: manual review required / not required / unclear.
- A short justification based on the specific case.
- References to the data fields and document passages used.
- Open questions or missing documents.

The model does not decide on a rejection itself. It must not treat open rule findings as resolved because of a plausible explanation. We do not use a self-reported numeric model confidence as a basis for decisions for now.

## 4. Synthetic test cases

All amounts and facts are fictional examples. The target values follow the policy defined for the demo. For all eleven customers, identity verification has already been completed successfully. The customer IDs match the table in [KYC_Beispieldaten.en.md](KYC_Beispieldaten.en.md).

| Case | Facts | Rule result | Expected path |
| --- | --- | --- | --- |
| K01 | Salary account; information, salary proof and expected incoming payments match | Green | LLM → approval |
| K02 | Planned initial deposit of EUR 18,000 from a vehicle sale; explanation, contract and amount fit together | Green | LLM → approval |
| K03 | Customer declares income from self-employment; the submitted document describes a loan from a third party | Green | LLM → manual review |
| K04 | Identity verified; customer states they are not the beneficial owner (answer "No") | Review required (R1) | LLM → manual review; no automatic approval |
| K05 | An inheritance explains a high planned initial deposit; synthetic proof matches person and amount | Green | LLM → approval |
| K06 | Salary and savings; information and documents fit; German and US citizenship, US status declared | Review required (R4) | LLM → manual review |
| K07 | PEP status confirmed; identity verified | Review required (R2) | LLM → manual review |
| K08 | Salary account of a nurse with consistent information | Green | LLM → approval |
| K09 | Screening returns a possible name match whose assignment is still open | Review required (R3) | LLM → manual review |
| K10 | Salary and gift; rules green, but the economic document excerpts are not sufficient for an assessment | Green | LLM unclear → manual review |
| K11 | Private account requested; according to the free text, the customer plans to handle client fees and costs of client projects through this account | Green | LLM → manual review |

The rules filter out exactly K04, K06, K07 and K09 for manual review. The seven green customers are checked by the LLM as well. K03, K10 and K11 demonstrate additional clarification needs that the rules do not cover.

K11 leads to a review of the discrepancy between the stated private account purpose and the planned business use. The decision on the requested account opening and a suitable account variant is made by the manual review after clarification.

Additional test cases in `laya_kyc/demo_cases.json` (details in [KYC_Beispieldaten.en.md](KYC_Beispieldaten.en.md)):

| Case | Facts | Rule result | Expected path |
| --- | --- | --- | --- |
| K12 | Variant of K01: home address on the Reeperbahn; self-employed, but salary savings and an employer confirmation | Green | LLM → manual review |
| K13 | Variant of K01: home address on the Reeperbahn, otherwise consistent | Green | LLM → manual review |
| K14 | Variant of K01: only the employer is located on the Reeperbahn | Green | LLM → approval |
| K15 | Employed engineer with consistent information, but a confirmed sanctions hit | Rejection (R5) | Rejection by rules, without LLM |

K12–K14 were entered during the workshop and exposed the address shortcut of the first model. K15 is the only case rejected by rules.

Forwarding a case to manual review is not a money-laundering suspicion and not a rejection. The final manual decision depends on the subsequent clarification.

## 5. Data for training and evaluation

A case consists of customer profile, account type, stated account purpose, expected account use, source of funds, documents and rule result. The planned use is collected as customer free text before the account is opened. A case is only included if identity verification has been completed successfully. Its status is documented as provenance metadata; it is constant in this dataset and not a learning feature for the decision on manual review. The separate target assessment contains the need for manual review, a justification and evidence references. This target assessment is not part of the model input.

### Additional data fields

The following are added for all eleven cases:

| Field | Data type | Content |
| --- | --- | --- |
| Origin of total wealth | Free text | Synthetic customer statement on how wealth was built over time; separate from the source of funds |
| Customer address | Object | Postcode, street and house number each as text; plus city and country |
| Employment status | Selection | Employed, self-employed, retired or public office |
| Employer | Optional object | Name, street, postcode and house number each as text; plus city and country |
| US tax liability per self-certification | Yes/No | Customer statement; separate from the bank's assessment |
| FATCA US person per self-certification | Yes/No | Customer statement on US person status |
| Tax residence | List of country codes | Customer statement; separate from home addresses |
| Foreign residence | Yes/No | Customer statement about one or more residences outside Germany |
| Foreign residences | Optional list of address objects | Country, city, postcode, street and house number |
| Planned account use | Free text | Customer statement from the application, e.g. private expenses or client fees |

For self-employed and retired customers, the employer object does not apply. Postcodes are text values so that leading zeros are kept. House numbers can contain letters. These details are added for every customer ID in [KYC_Beispieldaten.en.md](KYC_Beispieldaten.en.md). US tax liability and FATCA status are recorded separately as self-certification; foreign residence and tax residence are separate fields as well. K06 is a synthetic customer with German and US citizenship, declared US status and an additional US residence. K02 has an additional Swiss residence without declared US status. These fields do not change the training rules R1–R3; the US nexus has only been evaluated in the process since R4. Background on self-certification: [BZSt FATCA](https://online.portal.bzst.de/SharedDocs/Leistungsbeschreibung/DE/meldung_nach_FATCA-Abkommen.html) (German).

The eleven example customers serve business planning. The file `KYC_Trainingsdaten_5000.json` (later extended to `KYC_Trainingsdaten_6000.json`) adds 5,000 synthetic customer cases with separate inputs and target assessments: 4,000 training, 500 validation and 500 test. Under the demo policy, 2,500 cases each require or do not require manual review. Of the 4,000 rule-green cases, 1,500 have an additional need for manual review. The 1,000 source families stay separated between the splits. The texts are template-based; the same motifs appear in all splits. Use and limits are described in [KYC_Trainingsdaten_README.en.md](KYC_Trainingsdaten_README.en.md). The target assessments are constructed and have not yet been validated against real customer files.

For the talk, we first compare the same model without fine-tuning against a fine-tuned variant. This shows whether training actually helps with our task. As decision model we chose Laya Multilingual under Apache-2.0. Local training ran on a Mac M1 Max with 64 GB RAM using PyTorch MPS. Without fine-tuning, the model sends 246 of 250 unremarkable test cases to manual review unnecessarily; after fine-tuning, the synthetic test split is error-free, but among the demo cases K10 is approved by mistake. The active model v2 fixes the address shortcut from K12/K13; K10 remains wrong (see [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md)). Laya returns decisions and probabilities; free-text justifications and field references require an additional component. Model selection and evaluation limits are described in [KYC_Modellauswahl.en.md](KYC_Modellauswahl.en.md).

Key evaluation metrics:

- How many cases that need review are approved automatically by mistake?
- How many unremarkable cases are sent to manual review unnecessarily?
- Are the justifications supported by the stated data and document passages?
- How often does the model abstain or return an invalid answer?

Synthetic data demonstrates the method. A statement about quality on real bank customers requires separate, expert-validated data.

## 6. Proposed talk schedule

| Time | Content |
| --- | --- |
| 0–4 minutes | Problem and KYC example |
| 4–9 minutes | BPMN: rules, LLM and manual review |
| 9–14 minutes | Data, documents and case assessments |
| 14–19 minutes | Fine-tuning and comparison without fine-tuning |
| 19–27 minutes | Live demo: K01 automatic approval, K11 business use, K06 US nexus (R4), K15 rejection by rule (R5), K13/K14 home address vs. employer, task inbox and dashboard |
| 27–30 minutes | Results and limits |
| Then 15 minutes | Questions |

Next steps: have samples from the 5,000 synthetic cases checked by domain experts, create an independently worded test dataset, vary the training data so that wording and document situation are no longer coupled (finding K10), and add a component for justifications and field references.
