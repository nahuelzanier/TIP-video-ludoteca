# Known Issues and Limitations

This page records limitations confirmed in the current code. Older branch comparisons and
resolved defects were removed because they no longer describe the working tree.

## Product limitations

- **Volume preference:** saved to the user's settings, but Ludarium cannot control audio in an
  arbitrary uploaded HTML game because the game iframe has no shared volume-control contract.
- **Translations:** English and Spanish are selectable, but translation coverage is incomplete;
  strings without a translation continue to use their fallback text.
- **News image formats:** the news form's file picker advertises WebP, while backend image
  validation accepts PNG and JPEG. Use PNG or JPEG until these are aligned.
- **Save states:** there is no generic runtime save-state feature. Games can implement their
  own persistence, but Ludarium does not currently provide a common runtime snapshot interface.

## Deployment and operations

- The repository contains a CI workflow but no production deployment configuration. Configure
  a separate game-content origin before exposing user-uploaded games publicly; see
  [Game Isolation](Game-Isolation.md).
- Backend integration tests use Testcontainers and require Docker locally. See
  [Testing](Testing.md).
- The `dev` profile includes a local seed user and is intended only for a local development
  database. Do not point it at shared or production data; see [Getting Started](Getting-Started.md).

Update this page when a limitation is fixed or when new verified limitations are discovered.
