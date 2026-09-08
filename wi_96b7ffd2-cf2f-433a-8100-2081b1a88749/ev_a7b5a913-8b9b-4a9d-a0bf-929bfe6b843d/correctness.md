# Lens: correctness
Mode: self-review (in-run; requester no nested Task)
Diff: sessionBinding reuse under attach lock + concurrent ensure test
Findings: none medium+
Reuse gated on same mcpSessionId + preliminary worklinkSessionId; access assert + live client; dead incumbent falls through; cross-transport/eject unchanged.
