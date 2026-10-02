# Testing & CI

The backend has a real test suite: **8 classes, 56 tests**. The frontend has none. CI runs
the backend tests and, for the frontend, only a lint and a build.

## Running the tests

```bash
cd backend
./mvnw test
```

Single class or single method:

```bash
./mvnw test -Dtest=CommentServiceTests
./mvnw test -Dtest=CommentApiControllerTests#unauthenticatedCannotPublishComment
```

Reports land in `backend/target/surefire-reports/`.

## Test inventory

| Class | Tests | Style | Covers |
| --- | --- | --- | --- |
| `BackendApplicationTests` | 1 | `@SpringBootTest` | context loads |
| `comments/CommentServiceTests` | 15 | Mockito unit | comment creation, replies, ordering, `isGameAuthor` |
| `users/UserProfileServiceTests` | 9 | Mockito unit | profile reads, description edits, ownership |
| `controller/CommentApiControllerTests` | 9 | `@WebMvcTest` | comment endpoints through the security filter chain |
| `config/DevDataSeederTests` | 7 | Mockito unit | seed user, idempotency, reassignment, publishing |
| `controller/ProfileApiControllerTests` | 6 | `@WebMvcTest` | profile endpoints through the security filter chain |
| `games/GameServiceTests` | 5 | Mockito unit | publish rules and ownership |
| `controller/GamePublishControllerTests` | 4 | `@WebMvcTest` | publish endpoint through the security filter chain |
| **Total** | **56** | | |

All assertions use **AssertJ**.

## Two testing styles

### Unit tests with Mockito

Service classes are tested with plain Mockito, with no Spring context:

```java
@ExtendWith(MockitoExtension.class)
class CommentServiceTests {

    @Mock CommentRepository comments;
    @Mock GameRepository games;
    @Mock UserRepository users;

    @InjectMocks CommentService commentService;
```

Ids and timestamps are faked with `ReflectionTestUtils.setField(...)`, because JPA would
normally assign them.

`GameServiceTests` takes a slightly different route and constructs the service by hand:

```java
@BeforeEach
void setUp() {
    service = new GameService(games, users);
}
```

`DevDataSeederTests` needs `@MockitoSettings(strictness = Strictness.LENIENT)`, because the
seeder calls `log.info` with injected values that are not stubbed.

### Web layer tests with `@WebMvcTest`

The three controller tests exercise the full security filter chain, which is what makes them
valuable: they verify the `permitAll` rules really are public and that protected routes
really return `401`.

```java
@WebMvcTest(CommentApiController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class CommentApiControllerTests {

    @Autowired MockMvc mockMvc;

    @MockitoBean CommentService commentService;
```

The `@Import` set is needed because `SecurityConfig` reads `app.frontend-origin` and the
slice does not load `WebConfig`. `@MockitoBean` replaces the service so no database is
needed.

Requests are authenticated with Spring Security's test post-processors:

```java
mockMvc.perform(post("/api/games/game1/comments")
        .with(user("lucas2@gmail.com").roles("USER"))
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                { "content": "Me gustó mucho.", "rating": 4 }
                """))
    .andExpect(status().isCreated());
```

Notice `user("lucas2@gmail.com")` — the principal is the email, exactly like production.

## What the suite asserts well

| Area | Notable cases |
| --- | --- |
| Comment threading | a reply to a reply is flattened onto the root; nesting stays one level; replies keep ascending order while roots descend |
| Comment validation | blank content rejected, trimming applied, rating accepted/omitted, rating on a reply rejected |
| Privacy | the public listing never contains `email` or `passwordHash` |
| Ownership | the non-owner cannot publish (`403`) or edit a profile (`403`), and the service must not save |
| Idempotency | `publish` twice is fine; the `dev` seeder run twice does not duplicate the user |
| Password safety | the seeder stores a hash, not the plaintext seed password |
| Session rules | unauthenticated writes return `401` **and the service is never called** |
| Normalisation | blank description clears it to `null`; username comparisons are case-insensitive |

## What is not covered

| Gap | Why it matters |
| --- | --- |
| `AuthController` | register/login/me/logout have no test at all — the largest untested surface |
| `GameUploadService` | none of the ZIP limits, path validation, magic-byte checks or `index.html` requirement is tested |
| `GameApiController` | catalog pagination and clamping are untested |
| `SearchController` | all five validation branches are untested |
| `GameController` | the path-traversal hardening is untested |
| `GameCoverController` | untested |
| `DatabaseUserDetailsService` | untested |
| `GlobalExceptionHandler` | the mapping table is untested |
| The tags feature | `origin/feature/tags` ships with no tests |
| The frontend | no tests of any kind |

The upload service is the riskiest gap: it is the only code that processes untrusted binary
input, and it is the only one with no tests.

## Frontend

There is no test runner. `package.json` has no `test` script, and there is no Vitest, Jest,
Playwright, Cypress or Testing Library in `dependencies` or `devDependencies`.

The gate that CI applies instead:

```bash
npm run lint     # eslint .
npm run build    # tsc -b && vite build
```

`tsc -b` is the closest thing to a test: because `tsconfig.app.json` does **not** enable
`strict`, its value is limited. See [Known Issues](Known-Issues.md).

## Continuous integration

`.github/workflows/ci.yml` — the only workflow in the repository.

```yaml
on:
  push:
    branches: [main]
  pull_request:
```

Triggers on every pull request and on pushes to `main`.

### Backend job

```yaml
runs-on: ubuntu-latest
defaults:
  run:
    working-directory: backend

steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with:
      distribution: temurin
      java-version: '21'
      cache: maven
  - run: ./mvnw test
```

### Frontend job

```yaml
runs-on: ubuntu-latest
defaults:
  run:
    working-directory: frontend

steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-node@v4
    with:
      node-version: 24
      cache: npm
      cache-dependency-path: frontend/package-lock.json
  - run: npm ci
  - run: npm run lint
  - run: npm run build
```

## CI gaps

| Gap | Consequence |
| --- | --- |
| **No PostgreSQL service in the backend job** | `BackendApplicationTests.contextLoads()` is a full `@SpringBootTest` that needs a reachable database. With no service container and no `DB_PASSWORD`, this is the most likely thing to fail in CI. |
| **No test profile** | there is no `application-test.properties`, no H2, no Testcontainers. |
| CI runs only on `main` pushes and PRs | the 24 feature-branch commits were never validated by CI unless a PR was opened. |
| No coverage report | coverage is not measured or enforced. |
| No build/deploy job | nothing is published anywhere; see [Getting Started](Getting-Started.md#there-is-no-deployment). |
| `main` is behind | `main` sits at PR #1 while the working branches carry upload, search, profiles and comments. See [Known Issues](Known-Issues.md). |

## Adding a test

For a service, follow the Mockito pattern:

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTests {

    @Mock SomeRepository repository;
    @InjectMocks MyService service;

    @Test
    void returnsEmptyWhenNothingMatches() {
        // arrange
        // act
        // assert
    }
}
```

For a controller, copy the `@WebMvcTest` header from
`controller/ProfileApiControllerTests.java`, which is the smallest of the three, and remember
the two `@MockitoBean`s / `@Import`s it needs.

## Related

- [Architecture](Architecture.md) — where each class under test sits
- [API](API.md) — the endpoints the web-layer tests exercise
- [Known Issues](Known-Issues.md) — the CI and coverage gaps in context