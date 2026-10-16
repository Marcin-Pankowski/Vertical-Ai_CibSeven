# Using the synthetic KYC training data

*Deutsche Fassung: [KYC_Trainingsdaten_README.md](KYC_Trainingsdaten_README.md)*

The file `KYC_Trainingsdaten_5000.json` contains exactly 5,000 distinct synthetic customer profiles for the review after successful identification and before account opening. It serves the KYC model example. It is based on application data, self-disclosures, synthetic screening results and, where applicable, excerpts from submitted documents. Laya Multilingual was chosen as the decision model for local training on a Mac M1 with 64 GB RAM. Training method and limits are described in [KYC_Modellauswahl.en.md](KYC_Modellauswahl.en.md). Training was run on 3 October 2026; results are in [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md).

All field names and values in the dataset are German, for example `kunden` (customers), `eingabe` (input), `sollbewertung` (target assessment). They are kept as they are because the model was trained on them.

## Files

- `KYC_Trainingsdaten_6000.json`: version 1.1 with 1,000 additional derived variants against spurious correlations (address vs. document type, employer address, self-employed/salary contradiction); created with `laya_kyc/augment_dataset.py`, validation result in `KYC_Trainingsdaten_6000_Validierung.json`. Basis for model v2, see [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md).
- `KYC_Trainingsdaten_5000.json`: complete dataset as UTF-8 JSON with the array `kunden`.
- `KYC_Trainingsdaten.schema.json`: JSON Schema for the dataset and its input and target fields.
- `KYC_Trainingsdaten_Validierung.json`: validation results, distributions, file size and SHA-256 checksum.

## Structure of a customer case

Each element in `kunden` contains:

| Field | Purpose | Use in training |
| --- | --- | --- |
| `kunden_id` | Unique synthetic ID | For mapping and evaluation |
| `metadaten` | Split, source family, variant and scenario | For selection and evaluation |
| `eingabe` | Customer information, application, documents and rule check | As model input |
| `sollbewertung` | Need for manual review, decision, justification, evidence and questions | As training target |

**Only `eingabe` is given to the model as input.** The scenario name and the target assessment reveal the target decision and, like split and family ID, do not belong in the model input.

The target assessment has two decision levels: `manuelle_pruefung_erforderlich` (manual review required) is a boolean. `entscheidung` (decision) is `manuelle_pruefung`, `keine_manuelle_pruefung` or `unklar` (manual review, no manual review, unclear). With `unklar`, forwarding to manual review is required, so the boolean is `true`. Evidence entries point to the concrete input with field paths and their values. The model is trained on the need for manual review; a final rejection is not a target class.

## Customer fields included

- Successfully completed identity verification and the current process stage before account opening.
- Name, date of birth and citizenships.
- Customer address: postcode, street, house number, city and country.
- Customer is the beneficial owner: only `Ja` (yes) or `Nein` (no).
- Occupation, employment status and monthly net income as stated by the customer.
- Optional employer: name, street, postcode, house number, city and country. For self-employed and retired customers this is `null`.
- Origin of total wealth as free text, plus separate information on the source of the planned initial deposit.
- US tax liability, FATCA US person status and tax residence according to self-certification.
- Foreign residence as `Ja`/`Nein` and a list of additional addresses.
- Requested account type, account purpose and free text about planned use.
- Synthetic PEP/screening results, submitted document excerpts where applicable, and the computed result of the three demo rules.

Postcodes and house numbers are strings. Leading zeros, international postcodes and house number suffixes are preserved. Contradiction cases may intentionally contain differences between structured application fields and an explanation or document; these differences are part of the facts to be assessed.

## Splits

| Split | Customers | Manual review required | No manual review needed | Flagged by rules |
| --- | ---: | ---: | ---: | ---: |
| train | 4000 | 2000 | 2000 | 860 |
| validation | 500 | 250 | 250 | 65 |
| test | 500 | 250 | 250 | 75 |

The 1,000 source families each contain five individually designed customer cases. All variants of a family are in the same split. Every subset intentionally has the same number of cases with and without a need for manual review. This 50/50 distribution describes the demo, not how often such cases occur at a bank.

There are 1,000 customers with a rule trigger. Among the 4,000 rule-green customers, 1,500 need a manual review under the demo policy; 2,500 do not. For the actual added value of the model, the quality on the rule-green customers of `test` matters most. Passing the three rules alone would not be a sufficient classifier for these cases.

## Case types

Three rule triggers are kept: R1 customer is beneficial owner `Nein`; R2 PEP `Ja`; R3 possible screening hit unresolved. Each of these triggers means manual review under the fictional demo policy. The rules added later in the process, R4 (US nexus, manual review) and R5 (confirmed sanctions hit, rejection), are not part of the dataset; the screening status `bestaetigter_treffer` (confirmed hit) does not occur in the schema.

Rule-green cases with an additional review need include, among others, the contradiction between a requested private account and planned business use, contradictions in the source of funds or the build-up of wealth, insufficiently explained gifts, contradictory employer information and contradictory tax or foreign-residence information.

The 500 cases `privatkonto_geschaeftlich_geplant` (private account, business use planned) correspond to the motif of K11: the customer has been identified, requests a private account and declares that he will use it to handle client payments and costs for client projects. The basis is the application text.

Unremarkable counter-examples include explicitly private account use by self-employed customers, consistently declared US status and foreign residence, documented source of funds, explained changes of employer and plausibly stated private loans. These case types prevent the demo policy from treating such features as a review need across the board.

The 50 cases `adresse_reeperbahn_umfeld_pruefbedarf` (Reeperbahn area address, review needed) have synthetic home addresses on Hamburg's Reeperbahn or in its immediate surroundings. All other information and documents are consistent; R1–R3 stay green. Under the fictional demo policy, the location of the address alone leads to a manual address check. The house number assignments are invented; they are not information about real residents.

| Street | Postcode | Cases | train | validation | test |
| --- | --- | ---: | ---: | ---: | ---: |
| Reeperbahn | 20359 | 20 | 16 | 2 | 2 |
| Große Freiheit | 22767 | 10 | 8 | 1 | 1 |
| Davidstraße | 20359 | 10 | 8 | 1 | 1 |
| Spielbudenplatz | 20359 | 10 | 8 | 1 | 1 |

Each of the ten families contains two addresses directly on the Reeperbahn and one address on each of the three surrounding streets. The families stay entirely in their previous split. "Surroundings" describes the location of the streets; house coordinates or distances in metres were not computed. The postcode alone is not a reason for review.

The street locations were checked using [Hamburg.de: Große Freiheit](https://www.hamburg.de/tourismus/sehenswuerdigkeiten/grosse-freiheit-341258), [Hamburg.de: Davidwache](https://www.hamburg.de/tourismus/sehenswuerdigkeiten/davidwache-hamburg-341256) and [Hamburg.de: Spielbudenplatz](https://www.hamburg.de/oeffentliche-plaetze/4260704/spielbudenplatz.html) (German). The postcodes are based on [Große Freiheit 36](https://docksfreiheit36.de/grosse-freiheit/), [Davidstraße](https://www.hamburg.de/branchenbuch/hamburg/st-pauli/10247659/n0/) and [Spielbudenplatz](https://www.hamburg.de/freizeit/strassenfeste/spielbudenfestival-338520). These sources support the streets and postcodes, not the invented customer addresses or the need for manual review.

These cases replace 50 earlier variants with a contradictory source of funds. Their documents were reworded to match the stated source of funds. This keeps exactly 5,000 customers, the existing splits and the distribution of 2,500 cases with and 2,500 without a need for manual review. The earlier review reason was replaced by the address-only review need.

## Planned training procedure with Laya Multilingual

1. Load `kunden` and select the records via `metadaten.split`.
2. Use only `train` for training. The instruction reads roughly: "Using these KYC application data and the demo policy, check whether a manual review is required. Justify the result with evidence from the input."
3. Render `eingabe` compactly as customer content and check the actual token length including question and options. For Laya, `sollbewertung.entscheidung` is used as the target class over the three answer options. Justifications, evidence and open questions remain additional reference data; Laya itself does not generate free text. The implementation respects the context limit of the multilingual checkpoint and must not silently truncate relevant information.
4. Use `validation` to choose model and prompt settings. Use `test` only for the final comparison.
5. Compare the same model before and after fine-tuning. Evaluate cases approved automatically by mistake, unnecessary manual reviews and supported justifications separately; additionally evaluate the rule-green subset.

## Quality and limits

The target assessments were derived from the constructed facts and the fictional demo policy. They do not come from expert-reviewed real customer files. The schema does not represent a complete regulatory KYC or tax review process.

The free texts and documents are generated from wording templates. The profiles differ and the source families are separated, but the same motifs and partly the same sentence structures appear in all splits. A good test result therefore first shows how well the model learns these constructed cases. Before any statement about performance on real customer cases, independently worded, expert-reviewed test cases would be required.

Validated were the stored JSON file and all structural conditions used in the schema: exactly 5,000 customers, unique IDs and inputs, required fields and data types, successful identification status, addresses, employer null values, the three rule triggers, consistency of target class and boolean, the evidence paths and the complete separation of families between splits.

The JSON file contains no randomly invented confidence values. Uncertain facts are represented by the target decision `unklar` with forwarding to manual review.

SHA-256 of the data file: `1527a5ead2ec54970e16abaccddd2207791e36184b4be9a70208d384d489ac28`.
