# Release signing

ClipFlow releases use a long-lived debug-style certificate, not a freshly
generated per-machine debug key:

- Certificate SHA-256: `56a8b875f3a3096589094a0b2fc3e5fb0c0ae30f3b93e6c46e134a5f1eadaac3`
- Certificate SHA-1: `f9071add3d714bf6e5a02d18c09733f485122cae`

The keystore is intentionally **not committed** (`*.keystore` is ignored).
Place it at:

```text
signing/clipflow-release.keystore
```

`app/build.gradle.kts` prefers that path and falls back to
`~/.android/debug.keystore`. CI restores the same certificate from the
`CLIPFLOW_RELEASE_KEYSTORE_BASE64` secret.

Store password / key password: `android`; alias: `androiddebugkey`.
