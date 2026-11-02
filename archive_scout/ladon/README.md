# Ladon for Windows

Ladon is a native Windows console program with a numbered menu. It has 100 research heads across ten areas. Each head searches a different topic; all heads share one local catalog, one archive request queue, and one Ollama connection. The heads are scheduled in sequence to respect archive rate limits, and completed scans remain saved if you stop midway.

## Start

1. Extract `Ladon-Windows.zip` and double-click `Ladon.exe`. The numbered menu opens in a console window. Python and Go are not needed to run the EXE.
2. Choose **1** to browse heads in pages of ten, **2** to scan one head, **3** to scan the next five, or **4** to scan all remaining heads. A full scan can take several minutes.
3. Choose **6** to review discoveries, **7** to ask a question about saved records, **8** to translate a paper with a direct open access PDF, **10** to see records found by multiple heads, **11** to check the system, or **12** to search all saved records without internet or Ollama.

Ladon saves `catalog.json`, `catalog.json.bak`, per-head reports, and translations under `%USERPROFILE%\Ladon`. The backup holds the previous valid catalog and is used automatically if the primary file is damaged; Ladon announces recovery when it starts. A backup is created after the second save. It uses the public [OpenAlex works API](https://help.openalex.org/api/) and [The Met Collection API](https://metmuseum.github.io/). Internet access is needed to search archives and download PDFs. Scores are discovery hints based on query match, age, citations, and available metadata; they do not establish secrecy or significance.

Each numbered head has its own research topic, and every head writes to the same catalog. A record found by multiple heads retains the head numbers that found it. A custom search does not count toward the 100 numbered heads. A batch stops on an archive or save error, leaving the failed head available for retry. A later scan keeps the existing English summary when the new source result has none.

**Search saved research** accepts title words, author names, topics, museum departments, or record IDs. It ranks all matches from the local catalog and shows ten per page. Choose **N** or **P** to move between pages, or enter any result number to see its source URL, metadata, abstract or museum description, available PDF link, and any English summary. Choose **E** to export every match as a Markdown research brief under `%USERPROFILE%\Ladon\exports`; choose **Q** to return to the menu. Search and export use the records already on your computer and make no network or AI request.

## Local AI

For English summaries, questions, and paper translation, install [Ollama](https://docs.ollama.com/) on the same machine and run:

```powershell
ollama pull gemma3:4b
```

Ladon contacts Ollama only at a loopback address. It checks the installed model list before using AI and gives a concrete setup message if the selected model is missing. If Ollama is unavailable, archive search still works and AI summaries pause for that session. Set `LADON_MODEL` to a different installed model if desired. Ladon retains and retrieves records locally; it does not retrain model weights. The 100 heads are research lenses sharing one model and catalog; their number is not a measure of intelligence.

An optional `OPENALEX_API_KEY` environment variable provides a [larger OpenAlex usage allowance](https://help.openalex.org/api/authentication/). The key is sent as an HTTP bearer header and never saved in reports. `LADON_DATA_DIR` changes the data folder; `LADON_NO_AI=1` runs scans without AI summaries. `LADON_OLLAMA_URL` accepts a local HTTP URL such as `http://127.0.0.1:11434`.

If an archive moves its API, set `LADON_OPENALEX_WORKS_URL`, `LADON_MET_SEARCH_URL`, or `LADON_MET_OBJECT_BASE_URL` to its new public HTTPS endpoint. These settings change where Ladon requests data; an incompatible response schema still needs an adapter update. The OpenAlex key is accepted only when its endpoint remains on `api.openalex.org`. The current endpoint values appear in menu option **9**.

Use `Ladon.exe -doctor` to check writable storage, the current OpenAlex and Met responses, and the selected local model. `Ladon.exe -version` shows the version and build date. The system check makes small live API requests and reports source failures without changing saved research. Run it after a source or model upgrade. If a newer Ladon writes a catalog schema this build cannot read, this build stops with an update message rather than downgrading the data.

## Access and translation limits

Ladon spaces requests to each archive host by at least 1.5 seconds, backs off on HTTP 429 and temporary failures, and does not bypass access denials. PDF downloads also check `robots.txt` on the PDF host and redirect targets, including crawl delay directives. PDFs are limited to 50 MB. Translation saves each completed segment and its extracted original so an interrupted run can resume. Image-only pages are flagged for OCR; a wholly image-only PDF cannot be translated by this version. Verify machine translation against the source before citation and check source rights before redistribution.

## Build and test from source

The source is in this folder. It uses Go 1.27.1, `github.com/ledongthuc/pdf`, and `github.com/temoto/robotstxt`; exact versions are pinned in `go.mod` and `go.sum`.

```bash
go test ./...
go vet ./...
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath \
  -ldflags="-s -w -X main.buildDate=$(date -u +%Y-%m-%d)" -o Ladon.exe .
python3 package_release.py
```

`package_release.py` creates `Ladon-Windows.zip` with the EXE, README, license notices, and `SHA256SUMS.txt`. It verifies the archive before replacing an older ZIP. The repository includes a workflow template at `ci/ladon.yml` that runs unit tests, `go vet`, a Windows x64 build, and this packaging step. A maintainer with workflow-file write access can copy it to `.github/workflows/ladon.yml` to activate it. This build is unsigned and targets Windows x64.
