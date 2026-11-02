# Archive Scout

For a Windows `.exe` with a numbered menu and 100 topic-specific research heads, see [Ladon](ladon/README.md).

A local Python program that finds less cited open access papers and museum collection objects, then uses a local Ollama model to translate and explain the records in English. It uses the [OpenAlex works API](https://help.openalex.org/api/) and [The Met Collection API](https://metmuseum.github.io/) instead of crawling arbitrary websites. It does not bypass access controls. Scores are discovery hints based on title match, age, citation count, available abstract, and Met highlight status; they do not prove a record is secret or important.

## Install

Requires Python 3.9 or newer. From this directory:

```bash
python3 -m venv .venv
. .venv/bin/activate
python -m pip install -r requirements.txt
```

On Windows PowerShell, use `py -3 -m venv .venv`, then `.venv\\Scripts\\python.exe -m pip install -r requirements.txt`. Run the commands below with `.venv\\Scripts\\python.exe` in place of `python`; activation is optional.

For AI features, install [Ollama](https://docs.ollama.com/) separately, start it, and pull a model:

```bash
ollama pull gemma3:4b
```

The model runs on your own machine. No API key is needed for small OpenAlex queries; optionally set `OPENALEX_API_KEY` for a [larger usage allowance](https://help.openalex.org/api/authentication/) without storing it in the program's output.

## Discover records

```bash
python archive_scout.py scan --query "ancient astronomy" --output findings.json
```

The command saves `findings.json`, `findings.md`, and a local `archive-scout.sqlite` catalog. It translates and explains the highest ranked five records through Ollama. Choose `--ai-limit 20` to process more. To run discovery without Ollama:

```bash
python archive_scout.py scan --query "ancient astronomy" --no-ai --output findings.json
```

`--paper-limit` (default 20, maximum 100) and `--museum-limit` (default 10, maximum 30) bound API use. Archive requests are spaced at least 1.5 seconds apart per host by default; `--delay` can increase that interval. HTTP 429 and temporary server failures receive bounded backoff. Access denial stops that request and appears as an error. The Met search uses its paginated `/v1.1/search` endpoint.

Run more scans to add records to the same local catalog, then ask about them:

```bash
python archive_scout.py ask --question "Which objects relate to early astronomy?"
```

This retrieves matching saved records and gives them to Ollama as evidence. The answer lists source IDs and links. This is local retrieval, not training of model weights.

## Translate an entire open access paper

Find a paper ID and `pdf_url` in `findings.json`, then run:

```bash
python archive_scout.py translate --results findings.json --paper-id W1234567890 --output paper-english.md
```

This command only accepts a direct, public HTTPS PDF URL exposed in the OpenAlex record. It checks the PDF host's `robots.txt` (including redirect targets and crawl delays), limits the download to 50 MB, extracts every page with `pypdf`, and sends every extracted text segment to Ollama. The output contains the English translation and collapsible original text per segment. A `.progress.json` file enables exact resumption after interruption. Image-only PDFs require OCR and stop with a clear error; the program does not silently omit pages. PDF availability and reuse rights vary by publisher or repository; consult the linked source before distributing translated text.

The scan translates catalog metadata and abstracts. Full paper translation happens only with the `translate` command. Search results, the SQLite catalog, and translations remain on the local machine.

## Tests

```bash
python -m unittest discover -s tests -v
```

The tests use synthetic records and a fake local Ollama server. They do not require archive credentials or a downloaded model.
