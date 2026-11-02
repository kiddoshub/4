package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/temoto/robotstxt"
)

const userAgent = "Ladon/1.3 (research archive client)"
const maximumPDF = 50 << 20

var errPermanentRedirect = errors.New("redirect violates access policy")

type robotsPolicy struct {
	group    *robotstxt.Group
	allowAll bool
	denyAll  bool
}

type Fetcher struct {
	sources      SourceConfig
	client       *http.Client
	pdfClient    *http.Client
	mu           sync.Mutex
	next         map[string]time.Time
	delays       map[string]time.Duration
	robots       map[string]robotsPolicy
	defaultDelay time.Duration
}

func newFetcher() *Fetcher {
	f := &Fetcher{sources: defaultSources(), next: map[string]time.Time{}, delays: map[string]time.Duration{},
		robots: map[string]robotsPolicy{}, defaultDelay: 1500 * time.Millisecond}
	f.client = &http.Client{Timeout: 40 * time.Second, CheckRedirect: f.redirect(false)}
	f.pdfClient = &http.Client{Timeout: 2 * time.Minute, CheckRedirect: f.redirect(true)}
	return f
}

func publicURL(raw string) (*url.URL, error) {
	parsed, err := url.Parse(raw)
	if err != nil || parsed.Scheme != "https" || parsed.Hostname() == "" || parsed.User != nil {
		return nil, fmt.Errorf("public HTTPS URL required: %s", raw)
	}
	addresses, err := net.LookupIP(parsed.Hostname())
	if err != nil || len(addresses) == 0 {
		return nil, fmt.Errorf("cannot resolve %s", parsed.Hostname())
	}
	for _, address := range addresses {
		if !address.IsGlobalUnicast() || address.IsPrivate() || address.IsLoopback() || address.IsLinkLocalUnicast() {
			return nil, fmt.Errorf("nonpublic address refused: %s", parsed.Hostname())
		}
	}
	return parsed, nil
}

func (f *Fetcher) redirect(pdf bool) func(*http.Request, []*http.Request) error {
	return func(request *http.Request, previous []*http.Request) error {
		parsed, err := publicURL(request.URL.String())
		if err != nil {
			return fmt.Errorf("%w: %v", errPermanentRedirect, err)
		}
		if len(previous) >= 10 {
			return fmt.Errorf("%w: too many redirects", errPermanentRedirect)
		}
		if len(previous) > 0 && previous[0].Header.Get("Authorization") != "" &&
			parsed.Hostname() != previous[0].URL.Hostname() {
			return fmt.Errorf("%w: refusing to forward API key to another host", errPermanentRedirect)
		}
		if pdf {
			if err := f.robotsAllowed(request.URL.String()); err != nil {
				return fmt.Errorf("%w: %v", errPermanentRedirect, err)
			}
			f.wait(parsed.Hostname())
		}
		return nil
	}
}

func (f *Fetcher) wait(host string) {
	f.mu.Lock()
	now := time.Now()
	at := f.next[host]
	if at.Before(now) {
		at = now
	}
	delay := f.delays[host]
	if delay < f.defaultDelay {
		delay = f.defaultDelay
	}
	f.next[host] = at.Add(delay)
	f.mu.Unlock()
	if until := time.Until(at); until > 0 {
		time.Sleep(until)
	}
}

func retryDelay(header string, attempt int) (time.Duration, error) {
	delay := time.Duration(1<<attempt) * time.Second
	if seconds, err := strconv.Atoi(header); err == nil {
		delay = time.Duration(seconds) * time.Second
	} else if deadline, err := http.ParseTime(header); err == nil {
		delay = time.Until(deadline)
	}
	if delay < time.Second {
		delay = time.Second
	}
	if delay > time.Minute {
		return 0, errors.New("server requested a wait over one minute; retry later")
	}
	return delay, nil
}

func (f *Fetcher) get(raw string, maximum int64, pdf bool, bearer string) ([]byte, error) {
	parsed, err := publicURL(raw)
	if err != nil {
		return nil, err
	}
	if pdf {
		if err := f.robotsAllowed(raw); err != nil {
			return nil, err
		}
	}
	for attempt := 0; attempt < 4; attempt++ {
		f.wait(parsed.Hostname())
		request, err := http.NewRequest(http.MethodGet, raw, nil)
		if err != nil {
			return nil, err
		}
		request.Header.Set("User-Agent", userAgent)
		if pdf {
			request.Header.Set("Accept", "application/pdf")
		} else {
			request.Header.Set("Accept", "application/json")
		}
		if bearer != "" {
			request.Header.Set("Authorization", "Bearer "+bearer)
		}
		client := f.client
		if pdf {
			client = f.pdfClient
		}
		response, err := client.Do(request)
		if err != nil {
			if errors.Is(err, errPermanentRedirect) {
				return nil, err
			}
			if attempt == 3 {
				return nil, fmt.Errorf("request failed: %w", err)
			}
			time.Sleep(time.Duration(1<<attempt) * time.Second)
			continue
		}
		if response.StatusCode == http.StatusTooManyRequests || response.StatusCode >= 500 {
			response.Body.Close()
			if attempt == 3 {
				return nil, fmt.Errorf("HTTP %d after retries: %s", response.StatusCode, raw)
			}
			delay, err := retryDelay(response.Header.Get("Retry-After"), attempt)
			if err != nil {
				return nil, err
			}
			time.Sleep(delay)
			continue
		}
		if response.StatusCode != http.StatusOK {
			response.Body.Close()
			return nil, fmt.Errorf("HTTP %d: %s", response.StatusCode, raw)
		}
		if response.ContentLength > maximum {
			response.Body.Close()
			return nil, fmt.Errorf("response exceeds %d bytes", maximum)
		}
		body, err := io.ReadAll(io.LimitReader(response.Body, maximum+1))
		response.Body.Close()
		if err != nil {
			return nil, err
		}
		if int64(len(body)) > maximum {
			return nil, fmt.Errorf("response exceeds %d bytes", maximum)
		}
		return body, nil
	}
	return nil, errors.New("request failed after retries")
}

func (f *Fetcher) robotsAllowed(raw string) error {
	parsed, err := publicURL(raw)
	if err != nil {
		return err
	}
	origin := "https://" + parsed.Host
	f.mu.Lock()
	policy, found := f.robots[origin]
	f.mu.Unlock()
	if !found {
		robotsURL := origin + "/robots.txt"
		f.wait(parsed.Hostname())
		request, _ := http.NewRequest(http.MethodGet, robotsURL, nil)
		request.Header.Set("User-Agent", userAgent)
		response, err := f.client.Do(request)
		if err != nil {
			return fmt.Errorf("cannot verify robots.txt: %w", err)
		}
		body, readErr := io.ReadAll(io.LimitReader(response.Body, 1<<20+1))
		response.Body.Close()
		if readErr != nil || len(body) > 1<<20 {
			return errors.New("cannot safely read robots.txt")
		}
		switch response.StatusCode {
		case http.StatusNotFound, http.StatusGone:
			policy.allowAll = true
		case http.StatusOK:
			robots, err := robotstxt.FromBytes(body)
			if err != nil {
				return fmt.Errorf("invalid robots.txt: %w", err)
			}
			policy.group = robots.FindGroup(userAgent)
			if policy.group != nil && policy.group.CrawlDelay > f.defaultDelay {
				f.mu.Lock()
				f.delays[parsed.Hostname()] = policy.group.CrawlDelay
				f.mu.Unlock()
			}
		default:
			policy.denyAll = true
		}
		f.mu.Lock()
		f.robots[origin] = policy
		f.mu.Unlock()
	}
	if policy.denyAll || (policy.group != nil && !policy.group.Test(parsed.RequestURI())) {
		return fmt.Errorf("robots.txt disallows or cannot confirm access: %s", raw)
	}
	return nil
}

func (f *Fetcher) json(raw string, bearer string, target any) error {
	data, err := f.get(raw, 12<<20, false, bearer)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(data, target); err != nil {
		return fmt.Errorf("invalid JSON from %s: %w", raw, err)
	}
	return nil
}

func (f *Fetcher) pdf(raw string) ([]byte, error) {
	data, err := f.get(raw, maximumPDF, true, "")
	if err != nil {
		return nil, err
	}
	if !strings.HasPrefix(string(data[:min(len(data), 5)]), "%PDF-") {
		return nil, errors.New("open access URL did not return a PDF")
	}
	return data, nil
}
