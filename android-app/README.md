# ApexBuilder Android client

The native app is in `android-app/app`. It uses JDK 17, Gradle 8.7, AGP 8.4.0, Kotlin 1.9.24, Compose compiler 1.5.14, and Compose BOM 2024.05.00.

Build the client with:

```sh
gradle :app:assembleDebug --stacktrace --no-daemon
```

## GitHub build backend

The app uploads source assets and `config.json` to the selected repository branch, dispatches the configured workflow, polls that workflow's runs, and downloads its APK artifact. The workflow selected in the app must:

- Listen for `workflow_dispatch` and declare string inputs named `build_id` and `config_path`; `source_path` is also supplied for HTML and ZIP uploads.
- Check out the selected branch so the uploaded files are present.
- Read the snake_case build configuration at `config_path`.
- Upload the completed APK as an artifact named `apexbuilder-{build_id}`.

The included `.github/workflows/build-frontend.yml` is the requested client CI workflow. It assembles the ApexBuilder Android client; it is not the user-project backend workflow and does not declare the build inputs above. Add or select a separate compatible workflow in the configured repository to compile submitted projects.

Use a private repository for uploaded source and Google services files. A fine-grained GitHub token needs Contents write and Actions read/write permissions. For release signing, configure the signing keystore as secrets in the backend workflow; never upload signing keys through the app.