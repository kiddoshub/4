package main

// A head is a research lens. All heads share one catalog and one polite request queue.
type Head struct {
	ID       int
	Category string
	Topic    string
}

var headGroups = []struct {
	category string
	topics   []string
}{
	{"Astronomy", []string{"ancient star catalogs", "historic astrolabes", "lunar calendars", "early observatories", "comet observations", "eclipse records", "celestial maps", "navigation by stars", "planetary tables", "astronomical instruments"}},
	{"Archaeology", []string{"ancient inscriptions", "burial practices", "ceramic trade routes", "underwater archaeology", "urban excavations", "stone tools", "ritual sites", "archaeological dating", "ancient metallurgy", "lost settlements"}},
	{"Manuscripts", []string{"illuminated manuscripts", "palimpsests", "endangered scripts", "ancient translation", "oral history transcripts", "handwritten scientific notebooks", "medieval marginalia", "early printed books", "historical dictionaries", "papyrus fragments"}},
	{"Maps and Navigation", []string{"portolan charts", "historical sea routes", "early world maps", "survey instruments", "river mapping", "polar exploration maps", "indigenous cartography", "railway maps", "city plans", "navigation manuals"}},
	{"Natural History", []string{"botanical specimens", "extinct species records", "historic herbariums", "fossil collections", "insect taxonomy", "marine specimens", "animal migration records", "seed banks", "bird illustrations", "natural history expeditions"}},
	{"Medicine", []string{"historical medical instruments", "epidemic records", "traditional pharmacology", "anatomical atlases", "public health archives", "early microscopy", "surgical history", "hospital records history", "medical botany", "vaccination history"}},
	{"Engineering", []string{"water clocks", "ancient machinery", "bridge design history", "steam engine drawings", "early electrical instruments", "printing technology", "historical textile machines", "mechanical calculators", "optical instruments", "industrial patents history"}},
	{"Art and Materials", []string{"pigment analysis", "textile conservation", "ceramic glazes", "sculpture casting", "photographic processes", "museum provenance research", "glassmaking history", "woodworking techniques", "metalwork decoration", "paper conservation"}},
	{"Music and Culture", []string{"historical musical instruments", "folk song archives", "dance notation", "ancient theater", "ceremonial masks", "oral traditions", "early sound recordings", "music manuscripts", "festival history", "cultural exchange artifacts"}},
	{"Climate and Earth", []string{"historic weather logs", "glacier records", "volcanic eruptions history", "pollen climate archives", "tree ring studies", "early seismographs", "ocean temperature history", "drought chronicles", "flood maps", "geological field notebooks"}},
}

func allHeads() []Head {
	heads := make([]Head, 0, 100)
	for _, group := range headGroups {
		for _, topic := range group.topics {
			heads = append(heads, Head{ID: len(heads) + 1, Category: group.category, Topic: topic})
		}
	}
	return heads
}
