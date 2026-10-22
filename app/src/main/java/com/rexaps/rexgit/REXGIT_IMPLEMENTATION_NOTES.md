# RexGit — implementation notes

This ZIP contains the Kotlin source files for the RexGit pre-release changes. The uploaded source ZIP did not include a Gradle project, Android manifest, or dependency configuration, so this package could not be built into an APK in this environment.

## Implemented in this source pass

- Standard Git Push is available without enabling GitDev. GitDev is optional and adds folder scope/version metadata when selected.
- Commit types include Change, Fix, Add, Upgrade, Update, Testing, and Delete, with descriptions in the Push dialog. Upgrade can trigger a version bump while GitDev metadata mode is enabled.
- `token.env` reads and writes multiple accounts with the format `Account-1-Username=...`, `Account-1-Token=...`, then Account-2 and onward.
- Account-owned repository storage is `Download/RexAps/Github/<GitHub-username>/<Repo-name>/`.
- Repositories cloned from a different owner through GitHub search are stored at `Download/RexAps/Github/Global/<Owner>/<Repo-name>/`.
- The local workspace scanner uses the new account/global folder structure and intentionally ignores old repositories sitting directly in `Github/<Repo-name>/`.
- Opening a repository defaults to README preview and has README, Code, Actions, and Release tabs. The README renderer supports common Markdown structures; this is a lightweight native preview, not a complete GitHub Markdown engine.
- Code editing retains syntax highlighting, search, and copy tools, and now includes a line-number gutter.
- The Actions tab loads workflows and recent runs, displays statuses/timestamps, opens the run on GitHub, and offers downloads for log ZIPs and run artifacts.
- The Release tab lists releases/tags/notes and downloads release assets inside RexGit.
- GitHub-wide repository search can clone repos into the new account/global directory layout.

## Configuration and validation

- Keep real personal access tokens private. The sample format is only a format example; do not use example tokens.
- The actual Android app manifest must include `android.permission.INTERNET` for GitHub API calls and downloads. The original uploaded ZIP did not include the manifest, so this could not be verified here.
- The app's existing Android storage permission/access setup must allow the shared `Download/RexAps/Github` directory. If Android blocks shared storage access, the existing code falls back to the app-private directory.
- GitHub API visibility and artifact downloads depend on the token's repository/workflow permissions and on GitHub's API/rate-limit rules.
- Static Kotlin parsing did not report syntax-parser errors, but a full Android/Compose compile and runtime test remain outstanding because Gradle files and dependencies were not part of the uploaded ZIP.
