# Authentication and authorization

The storefront uses Keycloak Authorization Code with PKCE. Each Java service
independently verifies access tokens and enforces permissions. Customer data
ownership remains inside commerce, not in prompts or model arguments.

## Browser and identity flow

The SPA redirects to the realm authorization endpoint with a random state,
PKCE challenge, client ID and registered redirect URI. Keycloak handles
registration/login and returns a short-lived authorization code. The browser
exchanges that code with its verifier, keeping tokens in memory.

State correlates the redirect with the initiating browser action. PKCE binds
code exchange to that browser session without embedding a client secret in
the SPA. The access token authorizes API calls; an ID token describes the
login and is not an API bearer credential.

`frontend/src/auth/identity.ts`, `AuthContext.tsx` and `api/client.ts`
coordinate initialization, token refresh before protected requests, logout and
permission checks. UI checks improve the experience; API checks remain mandatory.
Logout ends the refresh session, but an already-issued access token is not
immediately revoked at every API.

## Java API enforcement

`ai-service/.../security/SecurityConfiguration.java` validates RS256 signature,
issuer, audience, nonblank subject, issued-at and expiry. Timestamp validation
has zero clock skew. JWKS fetching has a three-second connect/read timeout.

| AI endpoint | Access |
|---|---|
| GET /health, GET /suggestions | Public |
| GET /me | USERS_READ |
| POST /chat | USERS_WRITE |
| Other endpoints | Denied |

Invalid/missing tokens produce 401; missing permissions produce 403;
unavailable identity verification produces a sanitized 503.
`/me` returns subject and name, falling back through `name`,
`preferred_username`, then Customer. It never returns the bearer token.

AI forwards the verified bearer only to commerce HTTP APIs, never to the model.
No tool accepts a customer ID. Commerce's `SecurityConfig` and
`CurrentCustomerService` resolve the customer from the verified identity.
Cart and order operations use that customer; another customer's order is
indistinguishable from a missing order (404).

The admin product UI requires admin permissions; commerce independently
enforces them on writes. Products and images remain public reads. See
[the setup guide](../AUTHENTICATION.md) for realm import, roles, origins,
redirects and deployment configuration.

## Verification and limits

### Account menu presentation — 2026-10-17

Problem: logout occupied a primary header action. Implemented in
`frontend/src/App.tsx` and `styles.css`: Account opens a native disclosure with
My account and Log out (Log in for guests). The existing account route and
identity functions remain unchanged. Escape and outside clicks dismiss it.
The cart quantity badge now sits on the cart icon, including zero. Header gold
is reserved for cart and the search action; the brand mark is neutral navy.
Header grid rules avoid squeezing search and navigation at tablet widths.
No authentication API exchange, permissions or session handling changed.

Codex observed all 34 frontend tests and production build passing.
`App.test.tsx` verifies menu placement, logout/login dispatch, dismissal and
the item badge. No real logout/login journey or screenshot was observed:
browser connectors were unavailable and native Computer Use lacked permissions.
Learning: relocating account controls changes discoverability, not authorization;
the server remains responsible for enforcement. Next: visually verify the menu,
badge updates and identity redirects in a connected browser.

Verified on 2026-10-16: generated signing keys and local JWKS fixtures test
signature, issuer, audience, expiry (including just-expired tokens), issued-at
and verification outages. API tests verify permissions and safe identity
responses. Commerce tests cover independent permissions and customer isolation.
Frontend component tests cover protected UI behavior.

`IdentityFlowIT` is an opt-in Java registration/PKCE/logout test restricted
to a disposable Keycloak instance at localhost:18081; invoke `identityTest`
only after preparing that disposable realm. It was compiled but not run.
No real user realm, paid model or complete browser flow was exercised.

Learning: authentication identifies a caller; permissions authorize an action;
ownership limits which customer's data that action can read. None belongs to
the model. Next: use the trusted local setup to smoke-test login, refresh,
logout and two-customer isolation. Production hardening and immediate access-token
revocation remain outside the verified demo behavior.
