package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestHundredDistinctHeads(t *testing.T) {
	heads := allHeads()
	if len(heads) != 100 {
		t.Fatalf("got %d heads, want 100", len(heads))
	}
	seen := map[string]bool{}
	for index, head := range heads {
		if head.ID != index+1 || seen[head.Topic] || head.Category == "" {
			t.Fatalf("head %d is invalid or duplicated: %+v", index, head)
		}
		seen[head.Topic] = true
	}
}

func TestSourceConfigurationAndKeyBoundary(t *testing.T) {
	t.Setenv("OPENALEX_API_KEY", "")
	t.Setenv("LADON_OPENALEX_WORKS_URL", "https://api.openalex.org/new/works")
	t.Setenv("LADON_MET_SEARCH_URL", "https://collectionapi.metmuseum.org/new/search")
	t.Setenv("LADON_MET_OBJECT_BASE_URL", "https://collectionapi.metmuseum.org/new/objects")
	config, err := newSourceConfig()
	if err != nil || config.OpenAlexWorks != "https://api.openalex.org/new/works" ||
		config.MetSearch != "https://collectionapi.metmuseum.org/new/search" ||
		config.MetObjectBase != "https://collectionapi.metmuseum.org/new/objects/" {
		t.Fatalf("source overrides: %+v, %v", config, err)
	}
	t.Setenv("LADON_DATA_DIR", t.TempDir())
	app, err := newApp(strings.NewReader(""), &bytes.Buffer{})
	if err != nil || app.fetcher.sources != config {
		t.Fatalf("app did not use source overrides: %+v, %v", app, err)
	}
	t.Setenv("LADON_OPENALEX_WORKS_URL", "http://127.0.0.1/works")
	if _, err := newSourceConfig(); err == nil {
		t.Fatal("private HTTP endpoint should be rejected")
	}
	t.Setenv("LADON_OPENALEX_WORKS_URL", "https://example.org/works")
	t.Setenv("OPENALEX_API_KEY", "synthetic-test-key")
	if _, err := newSourceConfig(); err == nil || !strings.Contains(err.Error(), "api.openalex.org") {
		t.Fatalf("API key must stay on its expected host: %v", err)
	}
}

func TestSharedCatalogPersistsAndRetrieves(t *testing.T) {
	path := filepath.Join(t.TempDir(), "catalog.json")
	catalog := Catalog{Records: map[string]Record{}, Scanned: map[int]string{1: "2026-01-01"}}
	first := Record{Kind: "paper", ID: "W1", Title: "Ancient astronomy", Abstract: "Early instruments", URL: "https://openalex.org/W1", Score: 10}
	second := Record{Kind: "paper", ID: "W2", Title: "Marine biology", Abstract: "Ocean organisms", URL: "https://openalex.org/W2", Score: 99}
	catalog.Records[catalogKey(first)] = first
	catalog.Records[catalogKey(second)] = second
	if err := saveCatalog(path, catalog); err != nil {
		t.Fatal(err)
	}
	loaded, err := loadCatalog(path)
	if err != nil {
		t.Fatal(err)
	}
	result := loaded.find("astronomy instruments", 5)
	if len(result) != 1 || result[0].ID != "W1" || loaded.Scanned[1] == "" {
		t.Fatalf("wrong retrieval: %+v", result)
	}
}

func TestOfflineSavedResearchSearchAndDetails(t *testing.T) {
	directory := t.TempDir()
	t.Setenv("LADON_DATA_DIR", directory)
	t.Setenv("LADON_NO_AI", "1")
	catalog := emptyCatalog()
	item := Record{Kind: "paper", ID: "W123", Title: "Historic Sky Atlas", Year: 1902,
		Authors: []string{"Ida Historian"}, Topic: "Ancient astronomy", Abstract: "Early observatory maps",
		URL: "https://openalex.org/W123", PDFURL: "https://example.org/atlas.pdf", AIEnglish: "English explanation", Heads: []int{1}}
	catalog.Records[catalogKey(item)] = item
	catalog.Records["museum_object:7"] = Record{Kind: "museum_object", ID: "7", Title: "Bronze compass",
		Department: "Maritime history", URL: "https://www.metmuseum.org/art/collection/search/7"}
	if err := saveCatalog(filepath.Join(directory, "catalog.json"), catalog); err != nil {
		t.Fatal(err)
	}
	var output bytes.Buffer
	app, err := newApp(strings.NewReader("12\nhistorian\n1\ne\nq\n0\n"), &output)
	if err != nil {
		t.Fatal(err)
	}
	app.fetcher = nil
	app.llm = nil // Offline search must not contact either integration.
	if err := app.menu(); err != nil {
		t.Fatal(err)
	}
	for _, expected := range []string{"SAVED RESEARCH", "Historic Sky Atlas", "Authors: Ida Historian",
		"Source: https://openalex.org/W123", "PDF: https://example.org/atlas.pdf", "English explanation", "Found by heads: [1]", "Export saved:"} {
		if !strings.Contains(output.String(), expected) {
			t.Fatalf("offline search missing %q in %s", expected, output.String())
		}
	}
	exports, err := filepath.Glob(filepath.Join(directory, "exports", "search-*.md"))
	if err != nil || len(exports) != 1 {
		t.Fatalf("expected one export, found %v: %v", exports, err)
	}
	markdown, err := os.ReadFile(exports[0])
	if err != nil || !strings.Contains(string(markdown), "Source: https://openalex.org/W123") ||
		!strings.Contains(string(markdown), "Authors: Ida Historian") ||
		strings.Contains(string(markdown), "Bronze compass") {
		t.Fatalf("export did not contain exactly the matching source evidence: %v, %s", err, markdown)
	}
	for _, query := range []string{"W123", "astronomy", "maritime"} {
		if len(app.catalog.find(query, 20)) != 1 {
			t.Fatalf("metadata query %q did not find one record", query)
		}
	}
}

func TestOfflineSearchPagesAndExportsEveryMatch(t *testing.T) {
	directory := t.TempDir()
	t.Setenv("LADON_DATA_DIR", directory)
	t.Setenv("LADON_NO_AI", "1")
	catalog := emptyCatalog()
	for number := 1; number <= 25; number++ {
		id := fmt.Sprintf("W%03d", number)
		item := Record{Kind: "paper", ID: id, Title: fmt.Sprintf("Ancient record %03d", number),
			URL: "https://openalex.org/" + id}
		catalog.Records[catalogKey(item)] = item
	}
	if err := saveCatalog(filepath.Join(directory, "catalog.json"), catalog); err != nil {
		t.Fatal(err)
	}
	var output bytes.Buffer
	app, err := newApp(strings.NewReader("12\nancient\nn\nn\n25\np\n11\ne\nq\n0\n"), &output)
	if err != nil {
		t.Fatal(err)
	}
	app.fetcher, app.llm = nil, nil
	if err := app.menu(); err != nil {
		t.Fatal(err)
	}
	for _, expected := range []string{"25 matches | Page 1 of 3", "25 matches | Page 2 of 3",
		"25 matches | Page 3 of 3", "25. [paper W025]", "Source: https://openalex.org/W025",
		"11. [paper W011]", "Source: https://openalex.org/W011", "Export saved:"} {
		if !strings.Contains(output.String(), expected) {
			t.Fatalf("paged search missing %q in %s", expected, output.String())
		}
	}
	exports, err := filepath.Glob(filepath.Join(directory, "exports", "search-*.md"))
	if err != nil || len(exports) != 1 {
		t.Fatalf("expected one export, got %v: %v", exports, err)
	}
	markdown, err := os.ReadFile(exports[0])
	if err != nil || strings.Count(string(markdown), "## ") != 25 {
		t.Fatalf("export omitted matches: %v", err)
	}
}

func TestAbstractReconstructionAndScoring(t *testing.T) {
	abstract := abstractText(map[string][]int{"historic": {1}, "A": {0}, "record": {2}})
	if abstract != "A historic record" {
		t.Fatalf("got %q", abstract)
	}
	old := Record{Kind: "paper", Title: "Ancient astronomy", Year: 1900, Abstract: abstract, Citations: 1}
	popular := old
	popular.Citations = 1000
	if interestScore(old, "ancient astronomy") <= interestScore(popular, "ancient astronomy") {
		t.Fatal("older less-cited record should rank higher")
	}
}

func TestLocalOllamaProtocol(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/api/tags" {
			fmt.Fprint(w, `{"models":[{"name":"test-model"}]}`)
			return
		}
		if r.URL.Path != "/api/generate" || r.Method != http.MethodPost {
			t.Errorf("unexpected request: %s %s", r.Method, r.URL.Path)
		}
		var payload map[string]any
		if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
			t.Error(err)
		}
		if payload["stream"] != false || payload["model"] != "test-model" {
			t.Errorf("wrong Ollama payload: %+v", payload)
		}
		fmt.Fprint(w, `{"response":"English title: Ancient astronomy"}`)
	}))
	defer server.Close()
	t.Setenv("LADON_OLLAMA_URL", server.URL)
	t.Setenv("LADON_MODEL", "test-model")
	llm, err := newOllama()
	if err != nil {
		t.Fatal(err)
	}
	if err := llm.checkModel(); err != nil {
		t.Fatal(err)
	}
	answer, err := llm.generate("Translate this")
	if err != nil || !strings.Contains(answer, "Ancient astronomy") {
		t.Fatalf("answer %q, error %v", answer, err)
	}
}

func TestOllamaMissingModelHasActionableError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprint(w, `{"models":[{"name":"another:latest"}]}`)
	}))
	defer server.Close()
	t.Setenv("LADON_OLLAMA_URL", server.URL)
	t.Setenv("LADON_MODEL", "wanted:latest")
	llm, err := newOllama()
	if err != nil {
		t.Fatal(err)
	}
	if err := llm.checkModel(); err == nil || !strings.Contains(err.Error(), "ollama pull wanted:latest") {
		t.Fatalf("missing model error: %v", err)
	}
}

func TestCatalogBackupRecoveryAndSchemaMigration(t *testing.T) {
	path := filepath.Join(t.TempDir(), "catalog.json")
	legacy := `{"records":{"paper:W1":{"kind":"paper","id":"W1","title":"Old find","url":"https://openalex.org/W1"}},"scanned_heads":{"0":"wrong","1":"done"}}`
	if err := os.WriteFile(path, []byte(legacy), 0600); err != nil {
		t.Fatal(err)
	}
	catalog, err := loadCatalog(path)
	if err != nil || catalog.SchemaVersion != catalogSchemaVersion || len(catalog.Scanned) != 1 {
		t.Fatalf("legacy migration: %+v, %v", catalog, err)
	}
	if err := saveCatalog(path, catalog); err != nil {
		t.Fatal(err)
	}
	catalog.Records["paper:W2"] = Record{Kind: "paper", ID: "W2", Title: "New find"}
	if err := saveCatalog(path, catalog); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte("{"), 0600); err != nil {
		t.Fatal(err)
	}
	recovered, err := loadCatalog(path)
	if err != nil || !recovered.Recovered || recovered.Records["paper:W1"].Title != "Old find" || len(recovered.Records) != 1 {
		t.Fatalf("backup recovery: %+v, %v", recovered, err)
	}
	if err := saveCatalog(path, recovered); err != nil {
		t.Fatal(err)
	}
	if _, err := loadCatalog(path); err != nil {
		t.Fatalf("repaired catalog: %v", err)
	}
	if err := os.WriteFile(path, []byte(`{"schema_version":999,"records":{},"scanned_heads":{}}`), 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := loadCatalog(path); !errors.Is(err, errFutureSchema) {
		t.Fatalf("future schema must stop downgrade, got %v", err)
	}
}

func TestHeadsShareMemoryWithoutCountingCustomSearch(t *testing.T) {
	t.Setenv("LADON_DATA_DIR", t.TempDir())
	t.Setenv("LADON_NO_AI", "1")
	var output bytes.Buffer
	app, err := newApp(strings.NewReader(""), &output)
	if err != nil {
		t.Fatal(err)
	}
	key := "paper:W1"
	app.catalog.Records[key] = Record{Kind: "paper", ID: "W1", Title: "Saved", AIEnglish: "Prior English summary", Score: 99}
	app.search = func(_ *Fetcher, _ string, _, _ int) ([]Record, []error) {
		return []Record{{Kind: "paper", ID: "W1", Title: "Current", URL: "https://openalex.org/W1", Score: 1}}, nil
	}
	for _, head := range []Head{app.heads[0], app.heads[1], {ID: 0, Category: "Custom", Topic: "custom"}} {
		if !app.scan(head) {
			t.Fatalf("scan failed: %s", output.String())
		}
	}
	record := app.catalog.Records[key]
	if len(app.catalog.Scanned) != 2 || len(record.Heads) != 2 || record.Heads[0] != 1 || record.Heads[1] != 2 ||
		record.AIEnglish != "Prior English summary" || record.Score != 99 {
		t.Fatalf("shared memory lost data: %+v, scanned %+v", record, app.catalog.Scanned)
	}
	app.showConnections()
	if !strings.Contains(output.String(), "CROSS-HEAD CONNECTIONS") {
		t.Fatal("cross-head connection was not shown")
	}
}

func TestBatchPausesOnProviderFailure(t *testing.T) {
	t.Setenv("LADON_DATA_DIR", t.TempDir())
	t.Setenv("LADON_NO_AI", "1")
	var output bytes.Buffer
	app, err := newApp(strings.NewReader(""), &output)
	if err != nil {
		t.Fatal(err)
	}
	calls := 0
	app.search = func(_ *Fetcher, _ string, _, _ int) ([]Record, []error) {
		calls++
		return nil, []error{errors.New("provider unavailable")}
	}
	app.scanNext(100)
	if calls != 1 || len(app.catalog.Scanned) != 0 || !strings.Contains(output.String(), "Batch paused") {
		t.Fatalf("batch should pause and preserve retry: calls=%d, scanned=%d, output=%s", calls, len(app.catalog.Scanned), output.String())
	}
}

func TestTranslationRejectsUnsafeRecordID(t *testing.T) {
	if _, err := translatePDFData(nil, Record{ID: "../other"}, samplePDF(), t.TempDir(), nil); err == nil {
		t.Fatal("unsafe paper ID should not become a file path")
	}
}

func TestMenuIsReadableAndExits(t *testing.T) {
	t.Setenv("LADON_DATA_DIR", t.TempDir())
	t.Setenv("LADON_NO_AI", "1")
	var output bytes.Buffer
	app, err := newApp(strings.NewReader("9\n0\n"), &output)
	if err != nil {
		t.Fatal(err)
	}
	if err := app.menu(); err != nil {
		t.Fatal(err)
	}
	for _, expected := range []string{"100 research heads", "Scan one head", "Translate an open access paper", "Search saved research", "Ladon is resting"} {
		if !strings.Contains(output.String(), expected) {
			t.Fatalf("menu missing %q", expected)
		}
	}
}

func TestHeadBrowserPages(t *testing.T) {
	t.Setenv("LADON_DATA_DIR", t.TempDir())
	var output bytes.Buffer
	app, err := newApp(strings.NewReader("1\nn\np\nq\n0\n"), &output)
	if err != nil {
		t.Fatal(err)
	}
	if err := app.menu(); err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(output.String(), "Page 1 of 10") || !strings.Contains(output.String(), "Page 2 of 10") {
		t.Fatal("head browser did not navigate between pages")
	}
}

func TestTextChunksPreserveAllText(t *testing.T) {
	source := "  αβγ " + strings.Repeat("Old observatory notes.\n", 100) + " end  "
	chunks := textChunks(source, 80)
	if strings.Join(chunks, "") != source {
		t.Fatal("chunking lost text")
	}
	for _, chunk := range chunks {
		if len([]rune(chunk)) > 80 {
			t.Fatal("chunk exceeds requested size")
		}
	}
}

func TestPDFExtraction(t *testing.T) {
	segments, err := extractPDF(samplePDF())
	if err != nil || len(segments) != 1 || !strings.Contains(segments[0].Original, "Bonjour monde.") {
		t.Fatalf("segments %+v, error %v", segments, err)
	}
}

func samplePDF() []byte {
	stream := []byte("BT /F1 12 Tf 50 250 Td (Bonjour monde.) Tj ET")
	objects := [][]byte{
		[]byte("<< /Type /Catalog /Pages 2 0 R >>"),
		[]byte("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
		[]byte("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 300] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"),
		[]byte("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
		[]byte(fmt.Sprintf("<< /Length %d >>\nstream\n%s\nendstream", len(stream), stream)),
	}
	var document bytes.Buffer
	document.WriteString("%PDF-1.4\n")
	offsets := []int{0}
	for index, object := range objects {
		offsets = append(offsets, document.Len())
		fmt.Fprintf(&document, "%d 0 obj\n%s\nendobj\n", index+1, object)
	}
	xref := document.Len()
	document.WriteString("xref\n0 6\n0000000000 65535 f \n")
	for _, offset := range offsets[1:] {
		fmt.Fprintf(&document, "%010d 00000 n \n", offset)
	}
	fmt.Fprintf(&document, "trailer\n<< /Root 1 0 R /Size 6 >>\nstartxref\n%d\n%%%%EOF\n", xref)
	return document.Bytes()
}

func TestTranslationProgressResumesWithoutRepeat(t *testing.T) {
	calls := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls++
		fmt.Fprint(w, `{"response":"Hello world."}`)
	}))
	defer server.Close()
	t.Setenv("LADON_OLLAMA_URL", server.URL)
	llm, err := newOllama()
	if err != nil {
		t.Fatal(err)
	}
	directory := t.TempDir()
	record := Record{Kind: "paper", ID: "W1", Title: "Test", PDFURL: "https://example.org/paper.pdf"}
	for run := 0; run < 2; run++ {
		path, err := translatePDFData(llm, record, samplePDF(), directory, nil)
		if err != nil {
			t.Fatal(err)
		}
		text, err := os.ReadFile(path)
		if err != nil || !strings.Contains(string(text), "Hello world.") ||
			!strings.Contains(string(text), "Bonjour monde.") {
			t.Fatalf("bad saved translation %q: %v", text, err)
		}
	}
	if calls != 1 {
		t.Fatalf("Ollama called %d times, want 1 after resume", calls)
	}
}

func TestLiveOpenAccessPDF(t *testing.T) {
	if os.Getenv("LADON_LIVE_TEST") != "1" {
		t.Skip("set LADON_LIVE_TEST=1 for the public PDF integration check")
	}
	data, err := newFetcher().pdf("https://arxiv.org/pdf/astro-ph/0510738")
	if err != nil {
		t.Fatal(err)
	}
	segments, err := extractPDF(data)
	if err != nil || len(segments) < 2 {
		t.Fatalf("extracted %d segments, error %v", len(segments), err)
	}
}

func TestURLAndRetryPolicy(t *testing.T) {
	for _, raw := range []string{"http://example.org/file.pdf", "https://127.0.0.1/file.pdf"} {
		if _, err := publicURL(raw); err == nil {
			t.Errorf("accepted unsafe URL %s", raw)
		}
	}
	if _, err := retryDelay("120", 0); err == nil {
		t.Fatal("should stop when server asks for a long wait")
	}
	if delay, err := retryDelay("2", 0); err != nil || delay != 2*time.Second {
		t.Fatalf("retry delay %v, error %v", delay, err)
	}
}

func TestDamagedCatalogFailsClearly(t *testing.T) {
	path := filepath.Join(t.TempDir(), "catalog.json")
	if err := os.WriteFile(path, []byte("{"), 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := loadCatalog(path); err == nil {
		t.Fatal("damaged catalog should not be silently reset")
	}
}
