# Ladon and Archive Scout

[Ladon](archive_scout/ladon/README.md) is the Windows x64 research program in this repository. It searches OpenAlex papers and The Met collection through their public APIs, stores findings locally, and provides 100 topic heads, offline search, and Markdown exports. A local Ollama model can add English summaries and translate eligible text PDFs.

To build and verify Ladon from source, go to `archive_scout/ladon` and run `go test ./...`, `go vet ./...`, the Windows crossbuild command in its README, and `python3 package_release.py`. A [GitHub Actions workflow template](archive_scout/ladon/ci/ladon.yml) is included. A repository maintainer can copy it to `.github/workflows/ladon.yml` after granting workflow-file write access.

The earlier [Python Archive Scout](archive_scout/README.md) is available separately. The static site at the repository root is independent of the research tools.
