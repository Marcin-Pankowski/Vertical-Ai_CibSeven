# Synthetic KYC sample data after successful identity verification

*Deutsche Fassung: [KYC_Beispieldaten.md](KYC_Beispieldaten.md)*

Prerequisite for all records: the customer's identity verification has been completed successfully. It takes place before the BPMN process considered here. All names, data and facts are fully synthetic. All cases are reviewed before the account is opened. Only customer information, screening results and, where applicable, submitted documents are available. K11 shows a contradiction between a requested private account and planned business use. The core data are the eleven customers K01–K11; the test cases K12–K15 were added later (see [Additional test cases K12–K15](#additional-test-cases-k12k15)).

Names of companies, streets and places are fictional German examples and are left untranslated. The customers' free texts are translated; the German originals are in the German version and in `laya_kyc/demo_cases.py`.

## Demo rules

- R1: The customer states that they are not the beneficial owner (answer "No").
- R2: PEP status (politically exposed person) confirmed.
- R3: Possible screening hit still unresolved.
- R4: US nexus (US tax liability or FATCA US person according to self-certification, US citizenship, US tax residence or an address in the US). Added later; evaluated only in the process.
- R5: Confirmed sanctions hit. **Rejection rule**

R1–R4 mark the case for manual clarification. The rule result is then "review required". According to the BPMN process, these cases also go through the LLM check; the LLM cannot cancel the need for manual review set by the rules. All green cases are checked by the LLM as well.

R5 is the only exclusion reason. The rule result is then "rejection": the application ends directly at the end event "Rejection", without LLM check and without manual review. None of the customers K01–K11 triggers R5; K15 shows the rejection path.

## Eleven records

For all customers: identity verification = completed successfully; citizenship = DE, for K06 additionally US. The field "customer is the beneficial owner" contains only the customer's answer "Yes" or "No". A third value is not intended. Under our fictional demo policy, "No" triggers a manual review, not an automatic rejection. The source of funds is considered separately from this customer statement. Income and planned payments are recorded as customer statements from the application.

| ID | Name | Date of birth | Residence | Occupation | Net per month | Source of funds | Customer is beneficial owner | PEP | Screening | Rule result |
| --- | --- | --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| K01 | Erika Muster | 1988-04-12 | Hannover, DE | Engineer | EUR 3,200 | Salary | Yes | No | No hit | Green |
| K02 | Thomas Beispiel | 1976-09-23 | Köln, DE | Teacher | EUR 3,400 | Salary, vehicle sale | Yes | No | No hit | Green |
| K03 | Laura Muster | 1993-02-07 | Hamburg, DE | Designer | EUR 2,800 | Self-employment | Yes | No | No hit | Green |
| K04 | Jan Beispiel | 1990-11-18 | Leipzig, DE | Technician | EUR 2,700 | Salary | No | No | No hit | Review required: R1; manual |
| K05 | Claudia Muster | 1965-06-30 | Bremen, DE | Retiree | EUR 2,100 | Pension, inheritance | Yes | No | No hit | Green |
| K06 | Felix Beispiel | 1985-08-09 | Stuttgart, DE | Software developer | EUR 4,600 | Salary, savings | Yes | No | No hit | Review required: R4; manual |
| K07 | Lea Muster | 1972-03-14 | Berlin, DE | Senior public office | EUR 7,800 | Official pay | Yes | Yes | No hit | Review required: R2; manual |
| K08 | Paul Beispiel | 1998-01-25 | Dresden, DE | Nurse | EUR 2,600 | Salary | Yes | No | No hit | Green |
| K09 | Nora Muster | 1981-12-02 | Frankfurt, DE | Project manager | EUR 4,100 | Salary | Yes | No | Possible hit, unresolved | Review required: R3; manual |
| K10 | David Beispiel | 1995-05-16 | München, DE | Electrician | EUR 2,900 | Salary, gift | Yes | No | No hit | Green |
| K11 | Simon Muster | 1987-07-21 | Düsseldorf, DE | Self-employed IT consultant | EUR 3,100 | Self-employment | Yes | No | No hit | Green |

Exactly four of the eleven customers are filtered out by these rules: K04 (R1), K06 (R4), K07 (R2) and K09 (R3). None of them is rejected. The seven green customers also go to the LLM check. Rule-green alone is not an approval.

## Extended customer fields

The following tables extend the same eleven customers. The ID links the details to the main table. All addresses and employer names are fictional. The customer texts on the origin of total wealth are synthetic self-disclosures; the target assessments are kept separately.

### Home address

Postcode and house number are stored as text. This keeps leading zeros as in "04109" and suffixes as in "23a". City and country complete the address.

| ID | Postcode | Street | House no. | City | Country |
| --- | --- | --- | --- | --- | --- |
| K01 | 30159 | Musterweg | 12 | Hannover | DE |
| K02 | 50667 | Beispielstraße | 8 | Köln | DE |
| K03 | 20095 | Testallee | 23a | Hamburg | DE |
| K04 | 04109 | Musterstraße | 4 | Leipzig | DE |
| K05 | 28195 | Beispielweg | 9 | Bremen | DE |
| K06 | 70173 | Teststraße | 18 | Stuttgart | DE |
| K07 | 10115 | Musterallee | 15 | Berlin | DE |
| K08 | 01067 | Beispielgasse | 5 | Dresden | DE |
| K09 | 60311 | Testweg | 20 | Frankfurt | DE |
| K10 | 80331 | Musterplatz | 6 | München | DE |
| K11 | 40213 | Beispielstraße | 12 | Düsseldorf | DE |

### Employer

Self-employed and retired customers have no current employer. The employer and its address fields do not apply; "—" in the table stands for these empty values, not for an invented address. A customer's own company can be added later as a separate data object.

| ID | Employment status | Employer name | Street | House no. | Postcode | City | Country |
| --- | --- | --- | --- | --- | --- | --- | --- |
| K01 | Employed | Beispielmaschinenbau GmbH | Werkstraße | 50 | 30179 | Hannover | DE |
| K02 | Employed | Beispielschule West gGmbH | Schulstraße | 3 | 50931 | Köln | DE |
| K03 | Self-employed | No employer | — | — | — | — | — |
| K04 | Employed | Beispieltechnik Mitte GmbH | Werkstattstraße | 10 | 04129 | Leipzig | DE |
| K05 | Retired | No current employer | — | — | — | — | — |
| K06 | Employed | Beispielsoftware Süd GmbH | Technologieallee | 7 | 70563 | Stuttgart | DE |
| K07 | Public office | Beispielverwaltung Bund | Verwaltungsstraße | 1 | 10117 | Berlin | DE |
| K08 | Employed | Beispielpflege Elbe gGmbH | Klinikstraße | 22 | 01307 | Dresden | DE |
| K09 | Employed | Beispielprojekt Main GmbH | Projektstraße | 14 | 60327 | Frankfurt | DE |
| K10 | Employed | Beispielelektro Isar GmbH | Werkstattweg | 19 | 81331 | München | DE |
| K11 | Self-employed | No employer | — | — | — | — | — |

### Origin of total wealth as free text

The origin of total wealth describes how the customer built their wealth over time. The source of funds in the main table, by contrast, describes where the money intended for the account comes from. Both are recorded separately.

| ID | Origin of total wealth – customer free text (translated) |
| --- | --- |
| K01 | My wealth comes from regular savings from my salary as an engineer. Since 2015 I have invested part of it in funds. |
| K02 | Over many years I saved part of my teacher's salary. The proceeds from selling my private vehicle are now added. |
| K03 | My savings come from design commissions since 2018. I put aside part of my income after taxes and expenses. |
| K04 | I built my wealth from savings from my technician's salary. I have not received any major inheritances or gifts. |
| K05 | My wealth comes from earlier salary savings and an inheritance from my parents. Today I also receive a pension. |
| K06 | Since starting work as a software developer I have saved part of my salary. My wealth consists mostly of these savings and fund investments. |
| K07 | My wealth comes from earlier employment, current official pay and an inherited flat. It was built up over several years. |
| K08 | My savings come from my salary as a nurse. I saved small amounts every month. |
| K09 | I built my wealth from my salary as a project manager and returns on long-term investments. |
| K10 | My wealth consists of savings from my electrician's salary and a gift from my parents. I have not yet submitted any documents for the gift. |
| K11 | My wealth was built from the surpluses of my self-employed IT consulting. After costs and taxes I regularly put money aside. |

### US tax information, FATCA and foreign residence

"US dependency" is recorded, as defined for this demo, as US tax information or FATCA self-certification. US tax liability and FATCA US person status remain separate fields; in these synthetic examples the answers match. The bank's assessment has to be distinguished from the self-certification. Tax residence is determined by a self-certification; see [BZSt FATCA](https://online.portal.bzst.de/SharedDocs/Leistungsbeschreibung/DE/meldung_nach_FATCA-Abkommen.html) (German).

K06 states US citizenship in addition to German; his US status is not derived from a foreign residence alone. K02 shows a foreign residence in Switzerland without a declared US status. The table contains additional residences next to the German address. A foreign residence is modelled as a yes/no field with an optional list of addresses; the examples have at most one additional residence. Tax residence is recorded separately as a customer statement.

| ID | US tax liable per self-certification | FATCA US person per self-certification | Tax residence per self-certification | Foreign residence | Country | Postcode | Street | House no. | City |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| K01 | No | No | DE | No | — | — | — | — | — |
| K02 | No | No | DE, CH | Yes | CH | 8001 | Musterstrasse | 14 | Zürich |
| K03 | No | No | DE | No | — | — | — | — | — |
| K04 | No | No | DE | No | — | — | — | — | — |
| K05 | No | No | DE | No | — | — | — | — | — |
| K06 | Yes | Yes | DE, US | Yes | US | 98101 | Example Street | 100 | Seattle |
| K07 | No | No | DE | No | — | — | — | — | — |
| K08 | No | No | DE | No | — | — | — | — | — |
| K09 | No | No | DE | No | — | — | — | — | — |
| K10 | No | No | DE | No | — | — | — | — | — |
| K11 | No | No | DE | No | — | — | — | — | — |

Since R4 was introduced, a US nexus triggers a manual review; K06 is affected. A foreign residence outside the US (K02, Switzerland) still does not trigger a manual KYC review on its own. The additional data is used to assess the consistency of the customer profile; a complete tax review process is not part of the demo rules.

## K11: requested private account with planned business use

Identity verification has been completed successfully. The account has not been opened yet. The customer requests a private account and enters "private living expenses, rent and shopping" in the structured field "account purpose". He states that he is the beneficial owner. PEP and screening checks return no trigger.

In a free text about the planned account use, the customer explains: "My clients are to transfer the fees for IT projects to this account. I also want to pay the hosting costs for my client projects from it. I chose a private account because I work as a sole trader under my own name."

This explanation is a synthetic customer free text from the account application. It describes the intended future use. For this case, the information in the application is enough to demonstrate the contradiction; additional documents are not needed.

The model input contains the requested account type "private account", the stated private account purpose, the occupation and the free text about planned use. A pre-judged assessment "business use" or the target decision is not given to the model as an input field.

**Target assessment:** manual review required. The selected private account purpose contradicts the planned handling of client fees and costs of client projects. Rules R1–R5 stay green; the LLM should recognise and justify the discrepancy from the application fields and the customer free text.

**Expected process path:** rules green → LLM check → manual review → approval or rejection of the requested account opening after clarification. The manual review decides which account variant and customer profile are appropriate. Self-employment alone is not a trigger in this example; what matters is the contradiction between the selected account type and the declared planned use.

## Economic documents for the LLM demo

For K03, a synthetic document excerpt should contradict the stated source of funds: the customer names income from self-employment, but the document describes a loan from a third party. For K10, the documents for the stated gift should not be sufficient for a clear assessment. This lets the LLM detect additional need for manual clarification in cases that are initially green. Implemented in `laya_kyc/demo_cases.py`: K03 has a financing agreement about a repayable loan from a third party as its document; K10 has no document for the gift. For K11, the synthetic customer free text from the application is worked out above.

## Additional test cases K12–K15

K12–K14 were entered during the workshop as variants of K01 and exposed weaknesses of the first model (see [KYC_Trainingsergebnisse.en.md](KYC_Trainingsergebnisse.en.md), version 2). K15 shows the rejection by rule R5. The full application data is in `laya_kyc/demo_cases.py`; identity verification successful, citizenship DE, customer is beneficial owner "Yes", PEP "No", no US nexus.

| ID | Name | Date of birth | Home address | Employment | Net per month | Source of funds | Screening | Rule result | Expected path |
| --- | --- | --- | --- | --- | ---: | --- | --- | --- | --- |
| K12 | Tim Uphaus | 1988-04-12 | Reeperbahn 12, 20359 Hamburg | Self-employed, occupation "engineer", no employer | EUR 3,200 | Salary savings | No hit | Green | LLM → manual review |
| K13 | Karl Jones | 1988-04-12 | Reeperbahn 12, 20359 Hamburg | Employed, Beispielmaschinenbau GmbH, Reeperbahn 50 | EUR 3,200 | Salary savings | No hit | Green | LLM → manual review |
| K14 | Marcin Pankowski | 1988-04-12 | Musterweg 12, 30159 Hannover | Employed, Beispielmaschinenbau GmbH, Reeperbahn 50 | EUR 3,200 | Salary savings | No hit | Green | LLM → approval |
| K15 | Viktor Beispiel | 1979-10-03 | Musterweg 14, 30159 Hannover | Employed (engineer), Beispielmaschinenbau GmbH, Werkstraße 50, Hannover | EUR 3,300 | Salary savings | **Confirmed hit** | **Rejection: R5** | Rejection by rules, without LLM |

- K12: home address on the Reeperbahn; also contradictory: self-employed, but salary savings and an employer confirmation.
- K13: home address on the Reeperbahn, otherwise consistent. Under the demo policy, the address alone leads to manual review.
- K14: only the employer is located on the Reeperbahn; the home address is unremarkable. No reason for review.
- K15: information, salary and employer confirmation are consistent. The synthetic sanctions screening reports a confirmed hit (reference SAN-SYN-K15). R5 rejects before Laya is called. The screening status "confirmed hit" does not occur in the training data.

The tables are a compact demonstration schema; complete customer files will be worked out in the next planning step.
