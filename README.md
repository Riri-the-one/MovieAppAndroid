# Movie App Android

A Java Android application for browsing movies, discovering trailers and keeping personal movie preferences, with Firebase accounts and a French-language cinema assistant.

The app brings catalogue discovery, personal feedback and cinema exploration into one mobile interface. It is a portfolio project; no academic provenance beyond the available source has been established.

## Features

- Email/password registration, login, profile editing and account deletion.
- TMDB popular movies with pagination, grid/list views and filtering of loaded titles.
- Movie details, posters, release dates and YouTube trailers, with French/English fallback.
- Favorites and like/dislike preferences stored in Cloud Firestore.
- Gemini chat using the user's movie preferences; optional speech input.
- A cinema map with optional device location and a cinema quiz with a personal score.
- A generated QR ticket **demonstration**. It does not book a cinema seat or process payments.

## Stack and structure

Java (source compatibility 11), Android XML layouts, AndroidX AppCompat 1.7.1, Material 1.13.0, RecyclerView 1.4.0, Volley 1.2.1, Glide 4.16.0 and ZXing Embedded 4.3.0. Firebase BoM 34.13.0 supplies Authentication, Firestore and Analytics. Google Maps 19.0.0 and Location 21.3.0 support the map.

The build uses AGP 9.0.1, Gradle 9.2.1 and JDK 21. Minimum Android API is 31, target API is 36, and compilation uses API 36.1. Exact AndroidX versions are in `gradle/libs.versions.toml`.

The existing activity-based architecture is retained:

```text
app/src/main/java/com/example/movies_app/
  *Activity.java       Screens and navigation
  *Adapter.java       RecyclerView presentation
  AuthHelper.java     Firebase session/account operations
  FirestoreHelper.java Profiles, favorites, feedback and scores
  GeminiHelper.java   Gemini request/response handling
  TmdbApi.java        Catalogue endpoint and image URLs
  QuizRound.java      Local quiz submission state
app/src/main/res/     Layouts, strings, themes and icons
firestore.rules      Proposed database authorization rules
tests/               Quiz state and emulator regression checks
```

## Set up and run

1. Install Android Studio, JDK 21 and Android SDK platform 36.1/build tools 36.0.0. Clone this repository into a locally available directory. OneDrive placeholders must be downloaded before building.
2. Copy `local.properties.example` to `local.properties`. Set `sdk.dir` if Android Studio has not created it. Add your own `TMDB_API_KEY`, `GEMINI_API_KEY` and `MAPS_API_KEY` for the services you want to use.
3. Create your own Firebase project, register package `com.example.movies_app`, enable email/password Authentication and create a Firestore database. Download its Android configuration into `app/google-services.json` (ignored by Git).
4. Review `firestore.rules` against your own data and test it in an emulator before deploying it yourself. This repository does not deploy Firebase rules or change a live service.
5. Build and install on Android 12 or later:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

On macOS/Linux, use `bash gradlew` instead of `.\gradlew.bat`.

For **compilation only**, without a Firebase account, run `python scripts/create-demo-config.py` before building. It writes a fictional client configuration and refuses to overwrite a real one. Authentication and network features will not work with that fixture. Missing TMDB/Gemini keys produce an explanatory message; Google Maps requires its own configured key.

## Security and data

- Local keys and Firebase configuration are excluded from new commits. The original public history contains a Maps client key and Firebase client identifiers. Their restrictions and current validity have not been verified. The project owner should review them in the provider consoles; no key was tested against a live API.
- A key compiled into an APK is recoverable. Use Android package/signing restrictions for Maps; a production Gemini integration needs a server-side gateway rather than a bundled unrestricted credential.
- Feedback updates and favorites updates must retain their original owner. Regression tests cover cross-account reads, writes and owner changes in the Firestore emulator.
- Feedback copies are written in a batch; score increments use a transaction. Scores remain client-controlled and are unsuitable for a competitive leaderboard.
- Account cleanup propagates failures and deletes nested feedback in bounded batches. Authentication and Firestore deletion cannot form one atomic transaction; a trusted backend is the appropriate production solution.
- Profile pictures are compressed and stored in the user's Firestore profile. Profile data, preferences and chat context may be sensitive. Use fictional data for demonstrations.

## Tests and validation

```powershell
.\scripts\test-quiz.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon
```

The independent Java regression harness exercises missing questions, duplicate taps, retries and completed rounds. It requires JDK 21 and no Android or Firebase runtime.

Firestore tests follow the [Firebase emulator testing API](https://firebase.google.com/docs/rules/unit-tests). With Node 22 and JDK 21:

```sh
npm install --no-save --package-lock=false @firebase/rules-unit-testing@4.0.1 firebase@11.10.0 firebase-tools@14.7.0
npx firebase emulators:exec --project demo-movies-portfolio --config firebase.test.json --only firestore 'node --test tests/firestore.test.cjs'
```

Only the fictional `demo-movies-portfolio` project is used by this test command. GitHub Actions runs these tests plus Android unit tests, lint and a debug build with fictional Firebase configuration. See [Actions](https://github.com/Riri-the-one/MovieAppAndroid/actions) for actual remote results.

Validation on 13 November 2026: **9 quiz regression checks passed locally**. [GitHub Actions](https://github.com/Riri-the-one/MovieAppAndroid/actions/runs/43510428415) also passed all **4 Firestore emulator tests**, Android unit tests, Android Lint and `assembleDebug`. The local Gradle invocation stopped at its distribution download because terminal networking is restricted. CI uses fictional Firebase configuration and does not validate live service access. No application screenshot or visual device validation is claimed. Details are in [the audit](docs/AUDIT.md).

## Limitations and next steps

- Search filters downloaded movies; it is not a complete TMDB search.
- External features require valid service configuration and network access. Gemini output is probabilistic and is not an authoritative quiz judge.
- No real booking, payment or ticket verification service exists.
- Firebase rules must be reviewed and deployed separately; committed fixes do not protect an already deployed database automatically.
- Complete device checks for rotation, offline behavior, speech permission, approximate location and account cleanup are still needed. No authentic screenshots are available from this environment.
- Next priorities: server-side AI access and scoring, automated device tests, and incremental separation of network calls from activities.

## Third-party services and assets

Movie metadata and images are retrieved from [TMDB](https://www.themoviedb.org/). This product uses the TMDB API but is not endorsed or certified by TMDB. Movie titles, posters and packaged illustrations belong to their respective owners; the original asset provenance is not documented and no new ownership claim is made. Libraries retain their upstream licenses. Existing Git authorship and history are preserved; this work does not establish a license for the original application code.

