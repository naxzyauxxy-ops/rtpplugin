# HavocRTP

**MADE BY AUXXY**

Network-aware random teleport plugin with region menus, now with licensing.

---

## Licensing

This build is gated by [Havoc Licensing](../HavocLicensing). Customers set one
line in `config.yml`:

```yaml
license:
  key: "RTPQ-XXXX-XXXX-XXXX-XXXX"
```

The API URL and public key are **compiled into the jar**, not exposed in the
config, so customers cannot repoint the plugin at a fake licensing server.

### Before you build

Open `src/main/java/me/purplertp/plugin/license/LicenseConstants.java` and set:

| Constant | Value |
|---|---|
| `API_URL` | `https://your-host:25619/api/v1` |
| `PUBLIC_KEY` | The Ed25519 key from your dashboard (`npm run keygen`) |
| `PRODUCT` | `havocrtp` — already set, matches the product slug on the site |

If `PUBLIC_KEY` is blank or `API_URL` still says `your-host`, the plugin refuses
every licence with a clear log message rather than failing silently.

### Behaviour

- Validates on startup, off the main thread, then re-checks every 3 hours.
- Every response is Ed25519-signed and carries a per-request nonce, so a captured
  "valid" reply cannot be replayed.
- If your licensing server is unreachable, the last good result keeps the plugin
  running for **72 hours** so an outage on your side never takes a paying
  customer offline. A signed *rejection* clears that cache immediately, so
  revoking a licence bites on the next check.
- An invalid licence disables the plugin.

---

## Building

```bash
mvn clean package
```

Produces `target/HavocRTPNA-2.2.0.jar`. GitHub Actions builds it on every push;
tag `v2.2.0` to publish a release.

Requires Java 21, Spigot/Paper 1.21+.

---

## Note on the source

This project was reconstructed by decompiling `HavocRTPNA-2.2.0.jar`, since the
original source was not available. The logic is your compiled code recovered
intact — but variable names inside method bodies are regenerated, and comments
from the original are gone. If you still have the real source anywhere, prefer
it and copy the `license/` package plus the `onEnable` hook across instead.

---

MADE BY AUXXY
