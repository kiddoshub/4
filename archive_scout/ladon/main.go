package main

import (
	"bufio"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"
)

const version = "1.3.0"

var buildDate = "development"

type App struct {
	heads     []Head
	catalog   Catalog
	dataDir   string
	fetcher   *Fetcher
	search    func(*Fetcher, string, int, int) ([]Record, []error)
	llm       *Ollama
	aiEnabled bool
	aiChecked bool
	input     *bufio.Reader
	output    io.Writer
}

func newApp(input io.Reader, output io.Writer) (*App, error) {
	directory, err := dataDirectory()
	if err != nil {
		return nil, err
	}
	if err := os.MkdirAll(directory, 0755); err != nil {
		return nil, err
	}
	catalog, err := loadCatalog(filepath.Join(directory, "catalog.json"))
	if err != nil {
		return nil, err
	}
	llm, err := newOllama()
	if err != nil {
		return nil, err
	}
	sources, err := newSourceConfig()
	if err != nil {
		return nil, err
	}
	fetcher := newFetcher()
	fetcher.sources = sources
	return &App{heads: allHeads(), catalog: catalog, dataDir: directory, fetcher: fetcher,
		search: searchArchives,
		llm:    llm, aiEnabled: os.Getenv("LADON_NO_AI") != "1",
		input: bufio.NewReader(input), output: output}, nil
}

func (app *App) ask(prompt string) (string, error) {
	fmt.Fprint(app.output, prompt)
	line, err := app.input.ReadString('\n')
	if err != nil && !errors.Is(err, io.EOF) {
		return "", err
	}
	if errors.Is(err, io.EOF) && line == "" {
		return "", io.EOF
	}
	return strings.TrimSpace(line), nil
}

func (app *App) printHeader() {
	fmt.Fprintln(app.output, "\n============================================================")
	fmt.Fprintln(app.output, "  LADON  |  100 research heads, one shared memory")
	fmt.Fprintln(app.output, "============================================================")
	fmt.Fprintf(app.output, "  Scanned heads: %d/100    Saved records: %d\n", len(app.catalog.Scanned), len(app.catalog.Records))
	fmt.Fprintf(app.output, "  Data folder: %s\n\n", app.dataDir)
	fmt.Fprintln(app.output, "  1  Browse the 100 heads")
	fmt.Fprintln(app.output, "  2  Scan one head")
	fmt.Fprintln(app.output, "  3  Scan the next 5 unscanned heads")
	fmt.Fprintln(app.output, "  4  Scan all remaining heads (may take several minutes)")
	fmt.Fprintln(app.output, "  5  Custom archive search")
	fmt.Fprintln(app.output, "  6  View top discoveries")
	fmt.Fprintln(app.output, "  7  Ask Ladon about saved records")
	fmt.Fprintln(app.output, "  8  Translate an open access paper")
	fmt.Fprintln(app.output, "  9  Settings and help")
	fmt.Fprintln(app.output, " 10  Find cross-head connections")
	fmt.Fprintln(app.output, " 11  Run system check")
	fmt.Fprintln(app.output, " 12  Search saved research (works offline)")
	fmt.Fprintln(app.output, "  0  Exit")
	fmt.Fprintln(app.output)
}

func (app *App) printHeads() {
	fmt.Fprintln(app.output, "\nLADON'S 100 HEADS")
	previous := ""
	for _, head := range app.heads {
		if head.Category != previous {
			fmt.Fprintf(app.output, "\n%s\n", strings.ToUpper(head.Category))
			previous = head.Category
		}
		status := " "
		if app.catalog.Scanned[head.ID] != "" {
			status = "*"
		}
		fmt.Fprintf(app.output, "  %3d [%s] %s\n", head.ID, status, head.Topic)
	}
	fmt.Fprintln(app.output, "\n* = scanned at least once")
}

func (app *App) browseHeads() error {
	page := 0
	for {
		start := page * 10
		end := start + 10
		fmt.Fprintf(app.output, "\nRESEARCH HEADS | Page %d of 10 | %s\n",
			page+1, strings.ToUpper(app.heads[start].Category))
		for _, head := range app.heads[start:end] {
			status := " "
			if app.catalog.Scanned[head.ID] != "" {
				status = "*"
			}
			fmt.Fprintf(app.output, "  %3d [%s] %s\n", head.ID, status, head.Topic)
		}
		choice, err := app.ask("N next | P previous | number to scan | Q menu: ")
		if err != nil {
			return err
		}
		switch strings.ToLower(choice) {
		case "n":
			if page < 9 {
				page++
			}
		case "p":
			if page > 0 {
				page--
			}
		case "q", "":
			return nil
		default:
			id, err := strconv.Atoi(choice)
			if err != nil || id < 1 || id > len(app.heads) {
				fmt.Fprintln(app.output, "Choose N, P, Q, or a head number from 1 to 100.")
				continue
			}
			app.scan(app.heads[id-1])
			page = (id - 1) / 10
		}
	}
}

func (app *App) scan(head Head) bool {
	fmt.Fprintf(app.output, "\nHead %03d | %s | %s\n", head.ID, head.Category, head.Topic)
	query := head.Topic
	records, problems := app.search(app.fetcher, query, 3, 1)
	if len(records) > 0 && app.aiEnabled {
		if !app.aiChecked {
			if err := app.llm.checkModel(); err != nil {
				fmt.Fprintf(app.output, "AI note: %v\nAI summaries paused for this session. Search still works.\n", err)
				app.aiEnabled = false
			} else {
				app.aiChecked = true
			}
		}
		if app.aiEnabled {
			if err := app.llm.enrich(&records[0], head); err != nil {
				fmt.Fprintf(app.output, "AI note: %v\nAI summaries paused for this session. Search still works.\n", err)
				app.aiEnabled = false
			}
		}
	}
	for _, problem := range problems {
		fmt.Fprintf(app.output, "Archive note: %v\n", problem)
	}
	for index, record := range records {
		key := catalogKey(record)
		records[index] = mergeRecord(app.catalog.Records[key], record, head.ID)
		app.catalog.Records[key] = records[index]
	}
	if len(problems) == 0 && head.ID > 0 {
		app.catalog.Scanned[head.ID] = time.Now().UTC().Format(time.RFC3339)
	}
	if err := app.saveReport(head, records, problems); err != nil {
		fmt.Fprintf(app.output, "Could not save report: %v\n", err)
		return false
	}
	if err := saveCatalog(filepath.Join(app.dataDir, "catalog.json"), app.catalog); err != nil {
		fmt.Fprintf(app.output, "Could not save catalog: %v\n", err)
		return false
	}
	app.catalog.Recovered = false
	fmt.Fprintf(app.output, "Saved %d records. Total in shared memory: %d.\n", len(records), len(app.catalog.Records))
	return len(problems) == 0
}

func (app *App) saveReport(head Head, records []Record, problems []error) error {
	directory := filepath.Join(app.dataDir, "reports")
	if err := os.MkdirAll(directory, 0755); err != nil {
		return err
	}
	base := fmt.Sprintf("head-%03d", head.ID)
	if head.ID == 0 {
		base = "custom-" + time.Now().UTC().Format("20060102-150405.000000000")
	}
	payload := struct {
		Head    Head     `json:"head"`
		Records []Record `json:"records"`
		Errors  []string `json:"errors"`
	}{Head: head, Records: records, Errors: []string{}}
	for _, problem := range problems {
		payload.Errors = append(payload.Errors, problem.Error())
	}
	data, err := json.MarshalIndent(payload, "", "  ")
	if err != nil {
		return err
	}
	if err := writeAtomic(filepath.Join(directory, base+".json"), append(data, '\n')); err != nil {
		return err
	}
	var report strings.Builder
	fmt.Fprintf(&report, "# Ladon head %03d: %s\n\n", head.ID, head.Topic)
	report.WriteString("Scores are discovery hints, not proof that a record is secret or important.\n\n")
	for _, record := range records {
		fmt.Fprintf(&report, "## %s [%s]\n\nSource: %s\n\nScore: %.2f\n\n",
			record.Title, record.Kind, record.URL, record.Score)
		if record.Abstract != "" {
			fmt.Fprintf(&report, "Abstract: %s\n\n", record.Abstract)
		}
		if record.Description != "" {
			fmt.Fprintf(&report, "Catalog data: %s\n\n", record.Description)
		}
		if record.PDFURL != "" {
			fmt.Fprintf(&report, "Open access PDF: %s\n\n", record.PDFURL)
		}
		if record.AIEnglish != "" {
			fmt.Fprintf(&report, "%s\n\n", record.AIEnglish)
		}
	}
	for _, problem := range problems {
		fmt.Fprintf(&report, "- Retrieval error: %v\n", problem)
	}
	return writeAtomic(filepath.Join(directory, base+".md"), []byte(report.String()))
}

func (app *App) scanNext(limit int) {
	count := 0
	for _, head := range app.heads {
		if app.catalog.Scanned[head.ID] != "" {
			continue
		}
		if !app.scan(head) {
			fmt.Fprintln(app.output, "Batch paused after an archive or save error. Completed heads are saved; retry later.")
			return
		}
		count++
		if count >= limit {
			break
		}
	}
	if count == 0 {
		fmt.Fprintln(app.output, "All 100 heads have been scanned.")
	}
}

func (app *App) showTop() {
	items := app.catalog.top(20)
	if len(items) == 0 {
		fmt.Fprintln(app.output, "No records yet. Scan a head first.")
		return
	}
	fmt.Fprintln(app.output, "\nTOP DISCOVERIES")
	for index, item := range items {
		fmt.Fprintf(app.output, "%2d. [%s %s] %s (score %.2f)\n    %s\n",
			index+1, item.Kind, item.ID, item.Title, item.Score, item.URL)
	}
}

func (app *App) showConnections() {
	items := make([]Record, 0)
	for _, record := range app.catalog.Records {
		if len(record.Heads) >= 2 {
			items = append(items, record)
		}
	}
	sort.Slice(items, func(i, j int) bool {
		if len(items[i].Heads) == len(items[j].Heads) {
			return items[i].Score > items[j].Score
		}
		return len(items[i].Heads) > len(items[j].Heads)
	})
	if len(items) == 0 {
		fmt.Fprintln(app.output, "No records have been found by multiple heads yet. Scan more topics.")
		return
	}
	fmt.Fprintln(app.output, "\nCROSS-HEAD CONNECTIONS")
	for index, record := range items {
		if index == 20 {
			break
		}
		fmt.Fprintf(app.output, "%2d. %s [heads %v]\n    %s\n", index+1, record.Title, record.Heads, record.URL)
	}
}

func (app *App) searchSaved() error {
	if len(app.catalog.Records) == 0 {
		fmt.Fprintln(app.output, "No saved records yet. Scan a head first.")
		return nil
	}
	query, err := app.ask("Search title, author, topic, or record ID (blank returns): ")
	if errors.Is(err, io.EOF) || query == "" {
		return nil
	}
	if err != nil {
		return err
	}
	items := app.catalog.find(query, 20)
	if len(items) == 0 {
		fmt.Fprintln(app.output, "No saved records match those words. Try a broader search.")
		return nil
	}
	fmt.Fprintf(app.output, "\nSAVED RESEARCH | %d matching records shown\n", len(items))
	for index, item := range items {
		fmt.Fprintf(app.output, "%2d. [%s %s] %s\n", index+1, item.Kind, item.ID, item.Title)
	}
	for {
		choice, err := app.ask("Record number for details | E export matches | Q menu: ")
		if errors.Is(err, io.EOF) || strings.EqualFold(choice, "q") || choice == "" {
			return nil
		}
		if err != nil {
			return err
		}
		if strings.EqualFold(choice, "e") {
			path, err := app.exportSearchResults(query, items)
			if err != nil {
				fmt.Fprintf(app.output, "Could not export results: %v\n", err)
			} else {
				fmt.Fprintf(app.output, "Export saved: %s\n", path)
			}
			continue
		}
		index, err := strconv.Atoi(choice)
		if err != nil || index < 1 || index > len(items) {
			fmt.Fprintf(app.output, "Choose a number from 1 to %d, E, or Q.\n", len(items))
			continue
		}
		app.showRecord(items[index-1])
	}
}

func (app *App) showRecord(record Record) {
	fmt.Fprintf(app.output, "\n%s\n", record.Title)
	fmt.Fprintf(app.output, "Type: %s    ID: %s\n", record.Kind, record.ID)
	if record.Year > 0 {
		fmt.Fprintf(app.output, "Year: %d\n", record.Year)
	} else if record.Date != "" {
		fmt.Fprintf(app.output, "Date: %s\n", record.Date)
	}
	if len(record.Authors) > 0 {
		fmt.Fprintf(app.output, "Authors: %s\n", strings.Join(record.Authors, ", "))
	}
	if record.Topic != "" {
		fmt.Fprintf(app.output, "Topic: %s\n", record.Topic)
	}
	if record.Department != "" {
		fmt.Fprintf(app.output, "Museum department: %s\n", record.Department)
	}
	if len(record.Heads) > 0 {
		fmt.Fprintf(app.output, "Found by heads: %v\n", record.Heads)
	}
	fmt.Fprintf(app.output, "Source: %s\n", record.URL)
	if record.OpenAccessURL != "" {
		fmt.Fprintf(app.output, "Open access page: %s\n", record.OpenAccessURL)
	}
	if record.PDFURL != "" {
		fmt.Fprintf(app.output, "PDF: %s\n", record.PDFURL)
	}
	if record.Abstract != "" {
		fmt.Fprintf(app.output, "\nAbstract:\n%s\n", record.Abstract)
	}
	if record.Description != "" {
		fmt.Fprintf(app.output, "\nCatalog description:\n%s\n", record.Description)
	}
	if record.AIEnglish != "" {
		fmt.Fprintf(app.output, "\nEnglish summary:\n%s\n", record.AIEnglish)
	}
	fmt.Fprintln(app.output)
}

func (app *App) askAI() error {
	if err := app.llm.checkModel(); err != nil {
		return err
	}
	question, err := app.ask("Question: ")
	if err != nil || question == "" {
		return err
	}
	records := app.catalog.find(question, 5)
	answer, err := app.llm.answer(question, records)
	if err != nil {
		return err
	}
	fmt.Fprintf(app.output, "\n%s\n\nSources:\n", answer)
	for _, record := range records {
		fmt.Fprintf(app.output, "  [%s] %s\n", record.ID, record.URL)
	}
	return nil
}

func (app *App) translate() error {
	if err := app.llm.checkModel(); err != nil {
		return err
	}
	available := []Record{}
	for _, item := range app.catalog.Records {
		if item.Kind == "paper" && item.PDFURL != "" {
			available = append(available, item)
		}
	}
	sort.Slice(available, func(i, j int) bool { return available[i].Score > available[j].Score })
	if len(available) == 0 {
		return errors.New("no saved papers have a direct PDF; scan more heads")
	}
	fmt.Fprintln(app.output, "\nOPEN ACCESS PAPERS WITH PDF LINKS")
	for index, item := range available {
		fmt.Fprintf(app.output, "%3d. [%s] %s\n", index+1, item.ID, item.Title)
		if index == 19 {
			fmt.Fprintf(app.output, "...and %d more (enter a paper ID to select one).\n", len(available)-20)
			break
		}
	}
	choice, err := app.ask("Enter list number or paper ID: ")
	if err != nil {
		return err
	}
	var record Record
	selected := false
	if index, err := strconv.Atoi(choice); err == nil && index >= 1 && index <= len(available) {
		record, selected = available[index-1], true
	} else {
		for _, item := range available {
			if strings.EqualFold(item.ID, choice) {
				record, selected = item, true
				break
			}
		}
	}
	if !selected {
		return errors.New("paper not found in the numbered list or saved catalog")
	}
	fmt.Fprintf(app.output, "Downloading and translating %s. Progress is saved after each segment.\n", record.ID)
	path, err := translatePaper(app.fetcher, app.llm, record,
		filepath.Join(app.dataDir, "translations"), func(note string) { fmt.Fprintln(app.output, note) })
	if err != nil {
		return err
	}
	fmt.Fprintf(app.output, "Translation saved: %s\n", path)
	return nil
}

func (app *App) help() {
	fmt.Fprintln(app.output, "\nSETTINGS AND HELP")
	fmt.Fprintf(app.output, "Version: %s (built %s)\nData folder: %s\nLocal model: %s\n", version, buildDate, app.dataDir, app.llm.model)
	fmt.Fprintf(app.output, "AI summaries this session: %v\n", app.aiEnabled)
	fmt.Fprintf(app.output, "OpenAlex API key configured: %v\n", os.Getenv("OPENALEX_API_KEY") != "")
	fmt.Fprintln(app.output, "Sources: OpenAlex works and The Met Collection API.")
	fmt.Fprintf(app.output, "OpenAlex endpoint: %s\nMet search endpoint: %s\nMet object endpoint: %s\n",
		app.fetcher.sources.OpenAlexWorks, app.fetcher.sources.MetSearch, app.fetcher.sources.MetObjectBase)
	fmt.Fprintln(app.output, "Requests are spaced per host and retry politely. PDFs obey robots.txt.")
	fmt.Fprintln(app.output, "For AI, start Ollama and run: ollama pull gemma3:4b")
	fmt.Fprintln(app.output, "Set LADON_MODEL to choose another local model.")
	fmt.Fprintln(app.output, "Set LADON_NO_AI=1 to scan without AI summaries.")
	fmt.Fprintln(app.output, "Set LADON_DATA_DIR to move the local catalog and reports.")
	fmt.Fprintln(app.output, "Press Ctrl+C to stop a long scan; completed heads stay saved.")
	fmt.Fprintln(app.output, "Use menu option 11 or Ladon.exe -doctor after API or model changes.")
	fmt.Fprintln(app.output, "Use menu option 12 to search and export saved records without internet or Ollama.")
}

func (app *App) menu() error {
	for {
		app.printHeader()
		choice, err := app.ask("Choose 0-12: ")
		if errors.Is(err, io.EOF) {
			return nil
		}
		if err != nil {
			return err
		}
		switch choice {
		case "0":
			fmt.Fprintln(app.output, "Ladon is resting. Your research is saved.")
			return nil
		case "1":
			if err := app.browseHeads(); err != nil {
				return err
			}
		case "2":
			entry, err := app.ask("Head number (1-100): ")
			if err != nil {
				return err
			}
			id, err := strconv.Atoi(entry)
			if err != nil || id < 1 || id > len(app.heads) {
				fmt.Fprintln(app.output, "Enter a number from 1 to 100.")
				continue
			}
			app.scan(app.heads[id-1])
		case "3":
			app.scanNext(5)
		case "4":
			app.scanNext(100)
		case "5":
			query, err := app.ask("Research topic: ")
			if err != nil {
				return err
			}
			if query != "" {
				app.scan(Head{ID: 0, Category: "Custom", Topic: query})
			}
		case "6":
			app.showTop()
		case "7":
			if err := app.askAI(); err != nil {
				fmt.Fprintf(app.output, "AI note: %v\n", err)
			}
		case "8":
			if err := app.translate(); err != nil {
				fmt.Fprintf(app.output, "Translation note: %v\n", err)
			}
		case "9":
			app.help()
		case "10":
			app.showConnections()
		case "11":
			app.doctor()
		case "12":
			if err := app.searchSaved(); err != nil {
				return err
			}
		default:
			fmt.Fprintln(app.output, "Enter a menu number from 0 to 12.")
		}
	}
}

func main() {
	configureConsole()
	headFlag := flag.Int("head", 0, "scan one head by number without opening the menu")
	listFlag := flag.Bool("list", false, "list the 100 research heads")
	versionFlag := flag.Bool("version", false, "print version")
	doctorFlag := flag.Bool("doctor", false, "check archive APIs, local storage, and the AI model")
	flag.Parse()
	if *versionFlag {
		fmt.Printf("Ladon %s (built %s)\n", version, buildDate)
		return
	}
	app, err := newApp(os.Stdin, os.Stdout)
	if err != nil {
		fmt.Fprintln(os.Stderr, "Ladon could not start:", err)
		os.Exit(1)
	}
	if app.catalog.Recovered {
		fmt.Fprintln(os.Stdout, "Catalog recovered from backup. The next successful scan will repair the primary file.")
	}
	if *doctorFlag {
		if !app.doctor() {
			os.Exit(1)
		}
		return
	}
	if *listFlag {
		app.printHeads()
		return
	}
	if *headFlag != 0 {
		if *headFlag < 1 || *headFlag > len(app.heads) {
			fmt.Fprintln(os.Stderr, "Head number must be 1-100")
			os.Exit(2)
		}
		if !app.scan(app.heads[*headFlag-1]) {
			os.Exit(1)
		}
		return
	}
	if err := app.menu(); err != nil {
		fmt.Fprintln(os.Stderr, "Ladon stopped:", err)
		os.Exit(1)
	}
}
