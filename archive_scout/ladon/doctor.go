package main

import (
	"fmt"
	"os"
	"path/filepath"
)

// doctor reports the readiness of local storage and each optional integration.
// It leaves the catalog untouched and uses the same rate-limited HTTP client as scans.
func (app *App) doctor() bool {
	fmt.Fprintln(app.output, "\nLADON SYSTEM CHECK")
	ready := true
	if len(app.heads) == 100 {
		fmt.Fprintln(app.output, "PASS  100 research heads loaded")
	} else {
		fmt.Fprintf(app.output, "FAIL  Expected 100 heads; found %d\n", len(app.heads))
		ready = false
	}
	probe, err := os.CreateTemp(app.dataDir, ".ladon-check-*")
	if err == nil {
		name := probe.Name()
		if closeErr := probe.Close(); closeErr != nil {
			err = closeErr
		}
		if removeErr := os.Remove(name); removeErr != nil && err == nil {
			err = removeErr
		}
	}
	if err != nil {
		fmt.Fprintf(app.output, "FAIL  Data folder is not writable: %v\n", err)
		ready = false
	} else {
		fmt.Fprintf(app.output, "PASS  Data folder is writable: %s\n", app.dataDir)
	}
	if app.catalog.Recovered {
		fmt.Fprintln(app.output, "WARN  Catalog recovered from backup; next successful scan will restore the primary file")
	} else {
		fmt.Fprintln(app.output, "PASS  Catalog loaded")
	}
	if _, err := os.Stat(filepath.Join(app.dataDir, "catalog.json.bak")); err == nil {
		fmt.Fprintln(app.output, "PASS  Catalog backup exists")
	} else {
		fmt.Fprintln(app.output, "INFO  Backup appears after the second catalog save")
	}
	var works struct {
		Results []struct {
			ID string `json:"id"`
		} `json:"results"`
	}
	if err := app.fetcher.json(app.fetcher.sources.OpenAlexWorks+"?per-page=1&search=astronomy", os.Getenv("OPENALEX_API_KEY"), &works); err != nil {
		fmt.Fprintf(app.output, "WARN  OpenAlex: %v\n", err)
		ready = false
	} else if len(works.Results) == 0 || works.Results[0].ID == "" {
		fmt.Fprintln(app.output, "WARN  OpenAlex returned no usable test record")
		ready = false
	} else {
		fmt.Fprintln(app.output, "PASS  OpenAlex API returned a record")
	}
	var objects struct {
		IDs []int `json:"objectIDs"`
	}
	if err := app.fetcher.json(app.fetcher.sources.MetSearch+"?q=astronomy&limit=1&offset=0", "", &objects); err != nil {
		fmt.Fprintf(app.output, "WARN  Met Collection API: %v\n", err)
		ready = false
	} else if len(objects.IDs) == 0 {
		fmt.Fprintln(app.output, "WARN  Met Collection API returned no test object")
		ready = false
	} else {
		fmt.Fprintln(app.output, "PASS  Met Collection API returned an object")
	}
	if err := app.llm.checkModel(); err != nil {
		fmt.Fprintf(app.output, "WARN  Local AI: %v\n", err)
	} else {
		fmt.Fprintf(app.output, "PASS  Local AI model is installed: %s\n", app.llm.model)
	}
	return ready
}
