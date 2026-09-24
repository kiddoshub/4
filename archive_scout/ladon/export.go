package main

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"
)

func (app *App) exportSearchResults(query string, records []Record) (string, error) {
	if len(records) == 0 {
		return "", errors.New("there are no matching records to export")
	}
	directory := filepath.Join(app.dataDir, "exports")
	if err := os.MkdirAll(directory, 0755); err != nil {
		return "", err
	}
	now := time.Now().UTC()
	path := filepath.Join(directory, "search-"+now.Format("20060102-150405.000000000")+".md")
	var report strings.Builder
	fmt.Fprintf(&report, "# Ladon saved research\n\nQuery: %s\n\nGenerated: %s UTC\n\n",
		strings.Join(strings.Fields(query), " "), now.Format("2006-01-02 15:04:05"))
	report.WriteString("These are archive records and discovery hints. Verify details at the linked sources before citation.\n\n")
	for index, record := range records {
		fmt.Fprintf(&report, "## %d. %s\n\n", index+1, strings.Join(strings.Fields(record.Title), " "))
		fmt.Fprintf(&report, "Type: %s | ID: %s | Interest score: %.2f\n\nSource: %s\n\n",
			record.Kind, record.ID, record.Score, record.URL)
		if record.Year > 0 {
			fmt.Fprintf(&report, "Year: %d\n\n", record.Year)
		} else if record.Date != "" {
			fmt.Fprintf(&report, "Date: %s\n\n", record.Date)
		}
		if len(record.Authors) > 0 {
			fmt.Fprintf(&report, "Authors: %s\n\n", strings.Join(record.Authors, ", "))
		}
		if record.Topic != "" {
			fmt.Fprintf(&report, "Topic: %s\n\n", record.Topic)
		}
		if record.Department != "" {
			fmt.Fprintf(&report, "Museum department: %s\n\n", record.Department)
		}
		if len(record.Heads) > 0 {
			fmt.Fprintf(&report, "Found by heads: %v\n\n", record.Heads)
		}
		if record.OpenAccessURL != "" {
			fmt.Fprintf(&report, "Open access page: %s\n\n", record.OpenAccessURL)
		}
		if record.PDFURL != "" {
			fmt.Fprintf(&report, "PDF: %s\n\n", record.PDFURL)
		}
		if record.Abstract != "" {
			fmt.Fprintf(&report, "Abstract:\n%s\n\n", record.Abstract)
		}
		if record.Description != "" {
			fmt.Fprintf(&report, "Catalog description:\n%s\n\n", record.Description)
		}
		if record.AIEnglish != "" {
			fmt.Fprintf(&report, "English summary:\n%s\n\n", record.AIEnglish)
		}
	}
	if err := writeAtomic(path, []byte(report.String())); err != nil {
		return "", err
	}
	return path, nil
}
