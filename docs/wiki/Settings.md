# Settings and account data

The Settings page is available at `/settings` to signed-in users. It keeps account identity
separate from optional preferences so the database does not duplicate username, email or
password data.

## Stored data

`app_user` remains the source of truth for username, email and password hash. Account updates
are made through the account API. A one-to-one `user_settings` row, keyed by the user's ID,
stores volume, language and theme. The row is created or populated on demand and is loaded
after login, so preferences follow the account across devices.

| Preference | Values / behavior |
| --- | --- |
| Volume | Integer from 0 to 100 |
| Language | `en` or `es` |
| Theme | `system`, `light` or `dark` |

Account actions include changing username and password and logging out. Changing a password
requires the current password; passwords are hashed by Spring Security and never stored in
the settings table.

## API

All endpoints below require a session and CSRF protection for writes.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/users/me/settings` | Read preferences |
| `PUT` | `/api/users/me/settings` | Update preferences |
| `PATCH` | `/api/users/me/username` | Change username |
| `PATCH` | `/api/users/me/password` | Change password using current and new password |
| `POST` | `/api/auth/logout` | End the current session |

The user settings resource is not a second identity record: its primary key is also a foreign
key to `app_user.id` (`ON DELETE CASCADE`). See [Database](Database.md#migrations) and
[Authentication](Authentication.md).

## Current behavior

Theme selection is applied to the document and the interface supports system, light and dark
themes. The language setting updates the document language and uses the frontend translation
catalog where a translated string exists; untranslated interface text may remain in its
fallback language. Volume is persisted as a preference, but arbitrary uploaded HTML games do
not expose a common audio API for Ludarium to control their runtime volume.
