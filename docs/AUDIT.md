# Initial audit and validation record

Date: 2026-11-13. Baseline: `5687941f577e2f498b13c878f0a56b12847380b5`.

## Source and history

The original folder is `Movies_App-android`. Its Git origin is `Riri-the-one/MovieAppAndroid`; the local HEAD and GitHub main both identify the same initial commit. A local clone with independent objects was created for corrections, preserving the original folder. Some original working-tree files are OneDrive placeholders whose reads time out. Therefore uncommitted differences in that original working tree have not been fully compared; the audited implementation is the committed baseline.

## Baseline findings

Java Android application, XML views, activities/adapters and static service helpers; no Flutter or Kotlin application implementation. Firebase Authentication/Firestore/Analytics, TMDB, Gemini and Google Maps are integrated. Personal preferences, image profiles, catalogue pagination, trailers and demonstration QR tickets exist. A Gemini quiz activity exists but is not declared in the manifest and has no drawer entry.

Significant issues found by source inspection:

1. Firestore favorites/feedback updates validate only incoming ownership, allowing another authenticated user to overwrite a known document and become its owner.
2. Reading a favorite that does not exist can fail its resource-based rule instead of returning absent.
3. Quiz answer submission can award points repeatedly for the same question; new question requests can overlap correction; failed generation can retain the old question; substring verdict parsing accepts unintended output.
4. Scores use read/overwrite rather than a transaction, losing concurrent increments.
5. Feedback is duplicated using separate writes and reports success when the second write fails.
6. Account deletion ignores database failures, omits nested feedback, and uses an unbounded batch; Auth deletion is attempted even when data cleanup failed.
7. Maps client key is hardcoded; Firebase client configuration is tracked. These are not proof of a private service-account credential or of an active unrestricted key. Provider restrictions are unknown and no live validation was performed.
8. Approximate-only location grants are rejected by the permission callback.
9. Images are decoded at full resolution before resizing, risking excess memory use.
10. There is no README or CI. Existing Android test classes are generated templates, not feature coverage. IDE files are tracked.

## Corrections in the finalization branch

Owner-preserving update rules; absent favorite reads; registered/reachable quiz; duplicate submission guard and restored question/completed state; exact quiz verdicts; atomic score increments; atomic feedback copies; failure-aware bounded account cleanup including nested feedback; recent-login precheck; approximate location support; bounded image decoding; local Maps placeholder; ignored client configuration; setup example, README, CI and regression tests. No Firebase service or real user data was changed.

Account deletion still cannot atomically coordinate Firebase Authentication and Firestore. If an Auth deletion fails after successful data cleanup, the account can remain without application data. Recent login is checked before cleanup, but a backend is required for a durable deletion workflow. Scores and AI verdicts are demonstrative, not tamper-proof.

## Executed local checks

| Command/check | Actual result |
| --- | --- |
| `gh auth status` using the existing gh.exe installation | Failed: stored CLI token invalid |
| GitHub connector identity/repository read | Succeeded: Riri-the-one; MovieAppAndroid accessible |
| Local Git log/origin and GitHub tree inspection | Same baseline commit and repository |
| HTTPS Git clone from terminal | Failed: github.com:443 unavailable |
| Local Git clone, independent object copies | Succeeded; original history preserved |
| Java runtime | JDK 21.0.10 available |
| SDK inventory | Platforms 32, 34, 36.1; build tools including 36.0.0 and 36.1.0 |
| `gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon` | Failed before configuration: Gradle 9.2.1 download denied by terminal networking |
| `scripts/test-quiz.ps1` with JAVA_HOME pointing to existing JDK | 9 regression checks passed |
| `git diff --check` | Passed after changes |
| Source credential-pattern scan, excluding ignored original client config | No match for Google API key/private-key/token patterns after sanitizing tracked files; not a complete historical credential audit |
| ADB device discovery | Failed to initialize its user directory under sandbox; no device/visual validation established |

Firestore emulator checks and full Android checks are defined in CI; their definition does not imply they passed. Remote results must be checked on the PR. No screenshots were generated. No release build/signing was attempted. The Android framework-dependent changes still require CI and device validation.

## Publication constraints

No force push or history rewrite is permitted. The public baseline retains the original client identifiers even after removing them from the new tree. The owner should review restrictions/rotation in provider consoles. No original credentials are copied into documentation or test fixtures. This branch must not be represented as production-ready while service setup and device checks remain outstanding.
