# News and follows

Creators can publish news posts with a title, description and optional image. Signed-in users
see a paginated feed from creators they follow. News details and images are publicly readable;
creating, editing and deleting a post requires its author. News comments use the shared
comments system, but do not have game ratings.

## API

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/news?page=0&size=9` | session | Paginated feed from followed users; size is capped at 20 |
| `POST` | `/api/news` | session | Create a post with multipart title, description and optional image |
| `GET` | `/api/news/{id}` | public | Read post details |
| `GET` | `/api/news/{id}/image` | public | Read the post image |
| `PUT` | `/api/news/{id}` | author | Update post fields/image |
| `DELETE` | `/api/news/{id}` | author | Delete a post |
| `PUT` | `/api/follows/{username}` | session | Follow a creator |
| `DELETE` | `/api/follows/{username}` | session | Unfollow a creator |
| `GET` | `/api/follows/{username}/status` | session | Check whether the caller follows the creator |
| `GET` | `/api/follows/{username}/counts` | public | Read follower/following counts |
| `GET` / `POST` | `/api/news/{id}/comments` | public read / session write | List or add comments |

Follow operations are idempotent; users cannot follow themselves. News images are stored in
the database. The backend validates PNG/JPEG content and limits images to 5 MB. The current
publish form also advertises WebP in its browser file picker, but backend validation does not
accept WebP yet; see [Known Issues](Known-Issues.md).
