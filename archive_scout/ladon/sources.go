package main

import (
	"errors"
	"fmt"
	"net"
	"net/url"
	"os"
	"strings"
)

// SourceConfig keeps provider URLs together so API moves do not require code edits.
// Fetcher still validates DNS and the final destination before each request.
type SourceConfig struct {
	OpenAlexWorks string
	MetSearch     string
	MetObjectBase string
}

func defaultSources() SourceConfig {
	return SourceConfig{OpenAlexWorks: openAlexURL, MetSearch: metSearchURL, MetObjectBase: metObjectURL}
}

func configuredSourceURL(variable, fallback string, objectBase bool) (string, error) {
	raw := strings.TrimSpace(os.Getenv(variable))
	if raw == "" {
		return fallback, nil
	}
	parsed, err := url.Parse(raw)
	if err != nil || parsed.Scheme != "https" || parsed.Hostname() == "" || parsed.User != nil ||
		parsed.RawQuery != "" || parsed.Fragment != "" {
		return "", fmt.Errorf("%s must be a public HTTPS endpoint without credentials, query, or fragment", variable)
	}
	host := strings.ToLower(parsed.Hostname())
	if host == "localhost" || strings.HasSuffix(host, ".localhost") {
		return "", fmt.Errorf("%s must point to a public host", variable)
	}
	if address := net.ParseIP(host); address != nil && (!address.IsGlobalUnicast() || address.IsPrivate() || address.IsLoopback()) {
		return "", fmt.Errorf("%s must point to a public host", variable)
	}
	if objectBase && !strings.HasSuffix(parsed.Path, "/") {
		parsed.Path += "/"
	}
	return parsed.String(), nil
}

func newSourceConfig() (SourceConfig, error) {
	defaults := defaultSources()
	works, err := configuredSourceURL("LADON_OPENALEX_WORKS_URL", defaults.OpenAlexWorks, false)
	if err != nil {
		return SourceConfig{}, err
	}
	if os.Getenv("OPENALEX_API_KEY") != "" {
		parsed, _ := url.Parse(works)
		if parsed.Hostname() != "api.openalex.org" {
			return SourceConfig{}, errors.New("OPENALEX_API_KEY may only be sent to api.openalex.org; clear it before using a different OpenAlex endpoint")
		}
	}
	search, err := configuredSourceURL("LADON_MET_SEARCH_URL", defaults.MetSearch, false)
	if err != nil {
		return SourceConfig{}, err
	}
	objects, err := configuredSourceURL("LADON_MET_OBJECT_BASE_URL", defaults.MetObjectBase, true)
	if err != nil {
		return SourceConfig{}, err
	}
	return SourceConfig{OpenAlexWorks: works, MetSearch: search, MetObjectBase: objects}, nil
}
