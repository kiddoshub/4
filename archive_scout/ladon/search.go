package main

import (
	"fmt"
	"math"
	"net/url"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"
)

const openAlexURL = "https://api.openalex.org/works"
const metSearchURL = "https://collectionapi.metmuseum.org/public/collection/v1.1/search"
const metObjectURL = "https://collectionapi.metmuseum.org/public/collection/v1/objects/"

type openAlexWork struct {
	ID          string           `json:"id"`
	Title       string           `json:"title"`
	Year        int              `json:"publication_year"`
	Language    string           `json:"language"`
	Citations   int              `json:"cited_by_count"`
	Abstract    map[string][]int `json:"abstract_inverted_index"`
	DOI         string           `json:"doi"`
	Authorships []struct {
		Author struct {
			Name string `json:"display_name"`
		} `json:"author"`
	} `json:"authorships"`
	OpenAccess struct {
		URL string `json:"oa_url"`
	} `json:"open_access"`
	Location struct {
		PDFURL string `json:"pdf_url"`
		URL    string `json:"landing_page_url"`
	} `json:"best_oa_location"`
	PrimaryTopic struct {
		Name string `json:"display_name"`
	} `json:"primary_topic"`
}

type metObject struct {
	ID          int    `json:"objectID"`
	Title       string `json:"title"`
	ObjectName  string `json:"objectName"`
	Culture     string `json:"culture"`
	Period      string `json:"period"`
	Date        string `json:"objectDate"`
	Medium      string `json:"medium"`
	Artist      string `json:"artistDisplayName"`
	URL         string `json:"objectURL"`
	ImageURL    string `json:"primaryImageSmall"`
	Department  string `json:"department"`
	IsHighlight bool   `json:"isHighlight"`
}

func abstractText(inverted map[string][]int) string {
	positions := map[int]string{}
	maximum := -1
	for word, indexes := range inverted {
		for _, index := range indexes {
			if index >= 0 && index < 100000 {
				positions[index] = word
				if index > maximum {
					maximum = index
				}
			}
		}
	}
	result := make([]string, maximum+1)
	for index, word := range positions {
		result[index] = word
	}
	return strings.TrimSpace(strings.Join(result, " "))
}

func paperRecord(work openAlexWork) Record {
	identifier := work.ID[strings.LastIndex(work.ID, "/")+1:]
	title := work.Title
	if title == "" {
		title = "Untitled"
	}
	link := work.DOI
	if link == "" {
		link = work.ID
	}
	openURL := work.OpenAccess.URL
	if openURL == "" {
		openURL = work.Location.URL
	}
	record := Record{Kind: "paper", ID: identifier, Title: title, Year: work.Year,
		Language: work.Language, Citations: work.Citations, Abstract: abstractText(work.Abstract),
		URL: link, OpenAccessURL: openURL, PDFURL: work.Location.PDFURL, Topic: work.PrimaryTopic.Name}
	for _, authorship := range work.Authorships {
		if authorship.Author.Name != "" {
			record.Authors = append(record.Authors, authorship.Author.Name)
		}
	}
	return record
}

func museumRecord(object metObject) Record {
	title := object.Title
	if title == "" {
		title = "Untitled"
	}
	link := object.URL
	if link == "" {
		link = "https://www.metmuseum.org/art/collection/search/" + strconv.Itoa(object.ID)
	}
	parts := []string{}
	for _, part := range []string{object.ObjectName, object.Culture, object.Period, object.Date, object.Medium, object.Artist} {
		if part != "" {
			parts = append(parts, part)
		}
	}
	return Record{Kind: "museum_object", ID: strconv.Itoa(object.ID), Title: title,
		Date: object.Date, Description: strings.Join(parts, "; "), URL: link,
		ImageURL: object.ImageURL, Department: object.Department, IsHighlight: object.IsHighlight}
}

func interestScore(record Record, query string) float64 {
	terms := words(query)
	match := float64(matches(terms, words(record.Title))) / math.Max(1, float64(len(terms)))
	score := 25 * match
	if record.Kind == "paper" {
		age := time.Now().Year() - record.Year
		if age < 0 || record.Year == 0 {
			age = 0
		}
		score += math.Min(float64(age), 50)/3 + 25/math.Sqrt(float64(record.Citations)+1)
		if record.Abstract != "" {
			score += 8
		}
	} else {
		if record.Description != "" {
			score += 8
		}
		if record.ImageURL != "" {
			score += 5
		}
		if record.IsHighlight {
			score -= 12
		}
	}
	return math.Round(score*100) / 100
}

func searchArchives(fetcher *Fetcher, query string, papers, objects int) ([]Record, []error) {
	records := []Record{}
	problems := []error{}
	if papers > 0 {
		values := url.Values{"search": {query}, "filter": {"type:article,is_oa:true"},
			"per-page": {strconv.Itoa(papers)}, "page": {"1"}}
		var result struct {
			Results []openAlexWork `json:"results"`
		}
		if err := fetcher.json(fetcher.sources.OpenAlexWorks+"?"+values.Encode(), os.Getenv("OPENALEX_API_KEY"), &result); err != nil {
			problems = append(problems, fmt.Errorf("OpenAlex: %w", err))
		} else if result.Results == nil {
			problems = append(problems, fmt.Errorf("OpenAlex: response is missing the results list; API format may have changed"))
		} else {
			for _, work := range result.Results {
				if !paperIDPattern.MatchString(work.ID[strings.LastIndex(work.ID, "/")+1:]) {
					problems = append(problems, fmt.Errorf("OpenAlex: skipped record with invalid work ID"))
					continue
				}
				records = append(records, paperRecord(work))
			}
		}
	}
	if objects > 0 {
		values := url.Values{"q": {query}, "limit": {strconv.Itoa(objects)}, "offset": {"0"}}
		var result struct {
			IDs   []int `json:"objectIDs"`
			Total *int  `json:"total"`
		}
		if err := fetcher.json(fetcher.sources.MetSearch+"?"+values.Encode(), "", &result); err != nil {
			problems = append(problems, fmt.Errorf("Met search: %w", err))
		} else if result.Total == nil || (result.IDs == nil && *result.Total > 0) {
			problems = append(problems, fmt.Errorf("Met search: response is missing object IDs; API format may have changed"))
		} else {
			for _, id := range result.IDs {
				if id <= 0 {
					problems = append(problems, fmt.Errorf("Met search: skipped invalid object ID"))
					continue
				}
				var object metObject
				if err := fetcher.json(fetcher.sources.MetObjectBase+strconv.Itoa(id), "", &object); err != nil {
					problems = append(problems, fmt.Errorf("Met object %d: %w", id, err))
					continue
				}
				if object.ID != id {
					problems = append(problems, fmt.Errorf("Met object %d: response ID did not match", id))
					continue
				}
				records = append(records, museumRecord(object))
			}
		}
	}
	for index := range records {
		records[index].Score = interestScore(records[index], query)
	}
	sort.Slice(records, func(i, j int) bool { return records[i].Score > records[j].Score })
	return records, problems
}
