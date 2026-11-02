import json
import sys
import tempfile
import threading
import unittest
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import archive_scout as scout


class FakeClient:
    def __init__(self):
        self.calls = []

    def json(self, url, headers=None):
        self.calls.append((url, headers))
        if "openalex" in url:
            return {"results": [{"id": "https://openalex.org/W1", "title": "Astronomía antigua",
                                "publication_year": 1910, "language": "es", "cited_by_count": 2,
                                "abstract_inverted_index": {"Estudio": [0], "histórico": [1]},
                                "open_access": {"oa_url": "https://example.org/paper"},
                                "best_oa_location": {"pdf_url": "https://example.org/paper.pdf"}}]}
        if "v1.1/search" in url:
            return {"total": 1, "objectIDs": [123]}
        return {"objectID": 123, "title": "Astrolabe", "objectDate": "18th century",
                "objectURL": "https://www.metmuseum.org/art/collection/search/123",
                "department": "Scientific Instruments", "isHighlight": False}


class OllamaHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers["Content-Length"])
        payload = json.loads(self.rfile.read(length))
        assert payload["stream"] is False
        body = json.dumps({"response": "English title: Ancient astronomy"}).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_args):
        pass


class ArchiveScoutTests(unittest.TestCase):
    def test_search_normalizes_both_sources_and_uses_paged_met_api(self):
        client = FakeClient()
        records, errors = scout.search_archives(client, "astronomy", 10, 1)
        self.assertFalse(errors)
        self.assertEqual({item["kind"] for item in records}, {"paper", "museum_object"})
        self.assertEqual(next(item for item in records if item["kind"] == "paper")["abstract"],
                         "Estudio histórico")
        self.assertTrue(any("/v1.1/search" in url and "limit=1" in url for url, _ in client.calls))

    def test_chunks_preserve_every_character(self):
        sample = " \n " + ("a short phrase with symbols αβγ.\n" * 120) + " ending  "
        chunks = scout.text_chunks(sample, 100)
        self.assertEqual("".join(chunks), sample)
        self.assertTrue(all(len(chunk) <= 100 for chunk in chunks))

    def test_local_ollama_request_and_response(self):
        server = HTTPServer(("127.0.0.1", 0), OllamaHandler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            llm = scout.Ollama("test-model", "http://127.0.0.1:%d" % server.server_port)
            self.assertIn("Ancient astronomy", llm.generate("Translate this"))
        finally:
            server.shutdown()
            server.server_close()
            thread.join()

    def test_private_or_insecure_archive_urls_are_refused(self):
        for url in ("http://example.com/a.pdf", "https://127.0.0.1/a.pdf"):
            with self.assertRaises(scout.ArchiveError):
                scout.public_https(url)

    def test_pdf_redirect_obeys_target_policy(self):
        redirect = scout.SafeRedirect(lambda _url: False)
        request = urllib.request.Request("https://example.org/paper.pdf")
        with patch.object(scout, "public_https", side_effect=urllib.parse.urlparse):
            with self.assertRaises(scout.ArchiveError):
                redirect.redirect_request(request, None, 302, "Found", {},
                                          "https://example.net/paper.pdf")

    def test_robots_crawl_delay_and_disallow_are_honored(self):
        client = scout.ArchiveClient(delay=1.5)
        robots = b"User-agent: *\nCrawl-delay: 5\nDisallow: /private\n"
        with patch.object(scout, "public_https", side_effect=urllib.parse.urlparse), \
                patch.object(client, "_request", return_value=robots):
            self.assertTrue(client._robots_allowed("https://example.org/paper.pdf"))
            self.assertFalse(client._robots_allowed("https://example.org/private/paper.pdf"))
        self.assertEqual(client.host_delay["example.org"], 5)

    def test_saved_archive_memory_retrieves_relevant_record(self):
        with tempfile.TemporaryDirectory() as directory:
            memory = scout.ArchiveMemory(Path(directory) / "memory.sqlite")
            try:
                memory.save([{"kind": "paper", "id": "W1", "title": "Ancient astronomy",
                              "abstract": "Stars and instruments", "interest_score": 10},
                             {"kind": "paper", "id": "W2", "title": "Marine biology",
                              "abstract": "Ocean organisms", "interest_score": 99}])
                self.assertEqual([item["id"] for item in memory.find("astronomy instruments")], ["W1"])
            finally:
                memory.close()

    def test_ask_uses_saved_evidence_and_prints_source_link(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "memory.sqlite"
            memory = scout.ArchiveMemory(path)
            memory.save([{"kind": "paper", "id": "W1", "title": "Ancient astronomy",
                          "abstract": "Stars and instruments", "interest_score": 10,
                          "url": "https://openalex.org/W1"}])
            memory.close()
            args = type("Args", (), {"database": str(path), "question": "astronomy instruments",
                                     "model": "test-model", "ollama_url": "http://127.0.0.1:11434"})()
            from contextlib import redirect_stdout
            import io
            output = io.StringIO()
            with patch.object(scout.Ollama, "generate", return_value="[W1] The record describes instruments."), \
                    redirect_stdout(output):
                self.assertEqual(scout.run_ask(args), 0)
            self.assertIn("https://openalex.org/W1", output.getvalue())

    def test_full_pdf_translation_and_exact_resume(self):
        stream = b"BT /F1 12 Tf 50 250 Td (Bonjour monde.) Tj ET"
        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 300] "
            b"/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            b"<< /Length %d >>\nstream\n" % len(stream) + stream + b"\nendstream",
        ]
        pdf = b"%PDF-1.4\n"
        offsets = [0]
        for number, item in enumerate(objects, 1):
            offsets.append(len(pdf))
            pdf += ("%d 0 obj\n" % number).encode() + item + b"\nendobj\n"
        xref = len(pdf)
        pdf += b"xref\n0 6\n0000000000 65535 f \n"
        pdf += b"".join(("%010d 00000 n \n" % offset).encode() for offset in offsets[1:])
        pdf += b"trailer\n<< /Root 1 0 R /Size 6 >>\nstartxref\n"
        pdf += str(xref).encode() + b"\n%%EOF\n"

        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            results = root / "results.json"
            output = root / "translation.md"
            results.write_text(json.dumps({"records": [{"kind": "paper", "id": "W1",
                            "title": "Test", "pdf_url": "https://example.org/paper.pdf"}]}))
            args = type("Args", (), {"results": str(results), "paper_id": "W1",
                                     "output": str(output), "model": "test-model",
                                     "ollama_url": "http://127.0.0.1:11434", "delay": 1.5})()
            with patch.object(scout.ArchiveClient, "pdf", return_value=pdf), \
                    patch.object(scout.Ollama, "generate", return_value="Hello world.") as generate:
                self.assertEqual(scout.run_translate(args), 0)
                self.assertEqual(scout.run_translate(args), 0)
                self.assertEqual(generate.call_count, 1)
            self.assertIn("Hello world.", output.read_text())
            self.assertIn("Bonjour monde.", output.read_text())


if __name__ == "__main__":
    unittest.main()
