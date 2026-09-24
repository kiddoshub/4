package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
)

type Record struct {
	Kind          string   `json:"kind"`
	ID            string   `json:"id"`
	Title         string   `json:"title"`
	Year          int      `json:"year,omitempty"`
	Date          string   `json:"date,omitempty"`
	Language      string   `json:"language,omitempty"`
	Authors       []string `json:"authors,omitempty"`
	Citations     int      `json:"citations,omitempty"`
	Abstract      string   `json:"abstract,omitempty"`
	Description   string   `json:"description,omitempty"`
	URL           string   `json:"url"`
	OpenAccessURL string   `json:"open_access_url,omitempty"`
	PDFURL        string   `json:"pdf_url,omitempty"`
	Topic         string   `json:"topic,omitempty"`
	ImageURL      string   `json:"image_url,omitempty"`
	Department    string   `json:"department,omitempty"`
	IsHighlight   bool     `json:"is_highlight,omitempty"`
	Score         float64  `json:"interest_score"`
	AIEnglish     string   `json:"ai_english,omitempty"`
	Heads         []int    `json:"heads,omitempty"`
}

type Catalog struct {
	SchemaVersion int               `json:"schema_version"`
	Records       map[string]Record `json:"records"`
	Scanned       map[int]string    `json:"scanned_heads"`
	Recovered     bool              `json:"-"`
}

const catalogSchemaVersion = 1

var errFutureSchema = errors.New("catalog was written by a newer Ladon version")

func catalogKey(record Record) string { return record.Kind + ":" + record.ID }

func emptyCatalog() Catalog {
	return Catalog{SchemaVersion: catalogSchemaVersion, Records: map[string]Record{}, Scanned: map[int]string{}}
}

func decodeCatalog(data []byte) (Catalog, error) {
	var catalog Catalog
	if err := json.Unmarshal(data, &catalog); err != nil {
		return Catalog{}, fmt.Errorf("catalog is damaged: %w", err)
	}
	if catalog.SchemaVersion > catalogSchemaVersion || catalog.SchemaVersion < 0 {
		return Catalog{}, fmt.Errorf("%w (schema %d); update Ladon", errFutureSchema, catalog.SchemaVersion)
	}
	if catalog.Records == nil || catalog.Scanned == nil {
		return Catalog{}, errors.New("catalog is missing required fields")
	}
	// Older Ladon catalogs had no schema_version field.
	catalog.SchemaVersion = catalogSchemaVersion
	delete(catalog.Scanned, 0) // Custom searches are not numbered heads.
	return catalog, nil
}

func loadCatalog(path string) (Catalog, error) {
	data, err := os.ReadFile(path)
	if err == nil {
		catalog, decodeErr := decodeCatalog(data)
		if decodeErr == nil {
			return catalog, nil
		}
		if errors.Is(decodeErr, errFutureSchema) {
			return Catalog{}, decodeErr
		}
		err = decodeErr
	}
	backup, backupErr := os.ReadFile(path + ".bak")
	if backupErr == nil {
		catalog, decodeErr := decodeCatalog(backup)
		if decodeErr == nil {
			catalog.Recovered = true
			return catalog, nil
		}
		backupErr = decodeErr
	}
	if errors.Is(err, os.ErrNotExist) && errors.Is(backupErr, os.ErrNotExist) {
		return emptyCatalog(), nil
	}
	if backupErr != nil && !errors.Is(backupErr, os.ErrNotExist) {
		return Catalog{}, fmt.Errorf("catalog and backup could not be read: primary: %v; backup: %v", err, backupErr)
	}
	return Catalog{}, fmt.Errorf("catalog could not be read and no valid backup exists: %w", err)
}

func saveCatalog(path string, catalog Catalog) error {
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
		return err
	}
	catalog.SchemaVersion = catalogSchemaVersion
	data, err := json.MarshalIndent(catalog, "", "  ")
	if err != nil {
		return err
	}
	if previous, err := os.ReadFile(path); err == nil {
		if _, err := decodeCatalog(previous); err == nil {
			if err := writeAtomic(path+".bak", previous); err != nil {
				return fmt.Errorf("catalog backup failed; original was left intact: %w", err)
			}
		}
	} else if !errors.Is(err, os.ErrNotExist) {
		return err
	}
	return writeAtomic(path, append(data, '\n'))
}

func mergeRecord(previous, current Record, headID int) Record {
	if current.AIEnglish == "" {
		current.AIEnglish = previous.AIEnglish
	}
	if current.Title == "" || current.Title == "Untitled" {
		if previous.Title != "" {
			current.Title = previous.Title
		}
	}
	if current.URL == "" {
		current.URL = previous.URL
	}
	if current.Abstract == "" {
		current.Abstract = previous.Abstract
	}
	if current.Description == "" {
		current.Description = previous.Description
	}
	if current.PDFURL == "" {
		current.PDFURL = previous.PDFURL
	}
	if current.OpenAccessURL == "" {
		current.OpenAccessURL = previous.OpenAccessURL
	}
	if current.Score < previous.Score {
		current.Score = previous.Score
	}
	heads := map[int]bool{}
	for _, id := range previous.Heads {
		if id > 0 {
			heads[id] = true
		}
	}
	if headID > 0 {
		heads[headID] = true
	}
	for id := range heads {
		current.Heads = append(current.Heads, id)
	}
	sort.Ints(current.Heads)
	return current
}

func writeAtomic(path string, data []byte) error {
	file, err := os.CreateTemp(filepath.Dir(path), ".ladon-*")
	if err != nil {
		return err
	}
	defer os.Remove(file.Name())
	if err := file.Chmod(0600); err != nil {
		file.Close()
		return err
	}
	if _, err := file.Write(data); err != nil {
		file.Close()
		return err
	}
	if err := file.Sync(); err != nil {
		file.Close()
		return err
	}
	if err := file.Close(); err != nil {
		return err
	}
	return os.Rename(file.Name(), path)
}

var wordPattern = regexp.MustCompile(`[^\pL\pN]+`)

func words(text string) map[string]bool {
	result := map[string]bool{}
	for _, word := range wordPattern.Split(strings.ToLower(text), -1) {
		if len([]rune(word)) >= 3 {
			result[word] = true
		}
	}
	return result
}

func matches(a, b map[string]bool) int {
	count := 0
	for word := range a {
		if b[word] {
			count++
		}
	}
	return count
}

func (catalog Catalog) find(question string, limit int) []Record {
	terms := words(question)
	type ranked struct {
		record Record
		score  int
	}
	items := []ranked{}
	for _, record := range catalog.Records {
		metadata := record.ID + " " + record.Topic + " " + record.Department + " " + strings.Join(record.Authors, " ")
		score := 4*matches(terms, words(record.Title)) + 2*matches(terms, words(metadata)) +
			matches(terms, words(record.Abstract+" "+record.Description+" "+record.AIEnglish))
		if score > 0 {
			items = append(items, ranked{record, score})
		}
	}
	sort.Slice(items, func(i, j int) bool {
		if items[i].score == items[j].score {
			if items[i].record.Score == items[j].record.Score {
				return catalogKey(items[i].record) < catalogKey(items[j].record)
			}
			return items[i].record.Score > items[j].record.Score
		}
		return items[i].score > items[j].score
	})
	if limit > len(items) {
		limit = len(items)
	}
	result := make([]Record, 0, limit)
	for _, item := range items[:limit] {
		result = append(result, item.record)
	}
	return result
}

func (catalog Catalog) top(limit int) []Record {
	items := make([]Record, 0, len(catalog.Records))
	for _, item := range catalog.Records {
		items = append(items, item)
	}
	sort.Slice(items, func(i, j int) bool { return items[i].Score > items[j].Score })
	if limit > len(items) {
		limit = len(items)
	}
	return items[:limit]
}

func dataDirectory() (string, error) {
	if override := os.Getenv("LADON_DATA_DIR"); override != "" {
		return override, nil
	}
	home, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}
	return filepath.Join(home, "Ladon"), nil
}
