package com.apexbuilder.app

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apexbuilder.app.data.AppContainer
import com.apexbuilder.app.model.AppPage
import com.apexbuilder.app.model.BuildInput
import com.apexbuilder.app.model.BuildPhase
import com.apexbuilder.app.model.BuildProgressUpdate
import com.apexbuilder.app.model.BuilderForm
import com.apexbuilder.app.model.BuilderUiState
import com.apexbuilder.app.model.GitHubSettings
import com.apexbuilder.app.model.SourceMode
import com.apexbuilder.app.model.SplashMode
import com.apexbuilder.app.model.UploadType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BuilderViewModel(
    application: Application,
    private val container: AppContainer,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(BuilderUiState())
    val uiState: StateFlow<BuilderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val savedSettings = runCatching { container.githubSettingsStore.load() }.getOrDefault(GitHubSettings())
            _uiState.update { it.copy(github = savedSettings) }
        }
    }

    fun updateForm(transform: (BuilderForm) -> BuilderForm) {
        _uiState.update { state -> state.copy(form = transform(state.form), message = null) }
    }

    fun updateGitHub(transform: (GitHubSettings) -> GitHubSettings) {
        _uiState.update { state -> state.copy(github = transform(state.github), message = null) }
    }

    fun saveGitHubSettings() {
        val settings = _uiState.value.github
        viewModelScope.launch(Dispatchers.IO) {
            try {
                container.githubSettingsStore.save(settings)
                _uiState.update { it.copy(message = "GitHub settings saved securely on this device.") }
            } catch (error: Exception) {
                _uiState.update { it.copy(message = error.message ?: "Could not save GitHub settings.") }
            }
        }
    }

    fun setSource(uri: Uri, fileName: String) {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val expected = if (_uiState.value.form.uploadType == UploadType.Html) setOf("html", "htm") else setOf("zip")
        if (extension !in expected) {
            showMessage(if (_uiState.value.form.uploadType == UploadType.Html) "Choose a .html file." else "Choose a .zip archive.")
            return
        }
        _uiState.update { it.copy(sourceUri = uri, sourceFileName = fileName, message = null) }
    }

    fun setUploadType(uploadType: UploadType) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(uploadType = uploadType),
                sourceUri = null,
                sourceFileName = null,
                message = null,
            )
        }
    }

    fun setIcon(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val decoded = runCatching {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                options.outWidth > 0 && options.outHeight > 0 && options.outWidth == options.outHeight
            }.getOrDefault(false)
            if (!decoded) {
                showMessage("App icons must be square PNG, JPEG, or WebP images.")
                return@launch
            }
            _uiState.update { it.copy(iconUri = uri, iconFileName = fileName, message = null) }
        }
    }

    fun setGoogleServices(uri: Uri, fileName: String) {
        if (!fileName.endsWith(".json", ignoreCase = true)) {
            showMessage("Select a google-services.json file.")
            return
        }
        _uiState.update { it.copy(googleServicesUri = uri, googleServicesFileName = fileName, message = null) }
    }

    fun setSplash(uri: Uri, fileName: String) {
        _uiState.update { it.copy(splashUri = uri, splashFileName = fileName, message = null) }
    }

    fun startBuild() {
        val current = _uiState.value
        val validationError = validate(current)
        if (validationError != null) {
            showMessage(validationError)
            return
        }
        if (current.isRunning) return

        val input = BuildInput(
            form = current.form,
            sourceUri = current.sourceUri,
            sourceFileName = current.sourceFileName,
            iconUri = current.iconUri,
            iconFileName = current.iconFileName,
            googleServicesUri = current.googleServicesUri,
            googleServicesFileName = current.googleServicesFileName,
            splashUri = current.splashUri,
            splashFileName = current.splashFileName,
            github = current.github,
        )
        _uiState.update {
            it.copy(
                page = AppPage.Progress,
                phase = BuildPhase.Upload,
                currentStep = 0,
                isRunning = true,
                logLines = listOf(timestamped("Build session started")),
                errorMessage = null,
                apkPath = null,
                buildId = null,
                downloadMessage = null,
                message = null,
            )
        }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    container.githubSettingsStore.save(current.github)
                    container.buildRepository.build(input, ::handleProgress)
                }
                _uiState.update {
                    it.copy(
                        phase = BuildPhase.Ready,
                        currentStep = 3,
                        isRunning = false,
                        buildId = result.buildId,
                        apkPath = result.apkFile.absolutePath,
                        logLines = it.logLines + timestamped("APK artifact is ready to save"),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val message = readableError(error)
                _uiState.update {
                    it.copy(
                        phase = BuildPhase.Failed,
                        isRunning = false,
                        errorMessage = message,
                        logLines = it.logLines + timestamped("Build failed: $message"),
                    )
                }
            }
        }
    }

    fun returnToEditor() {
        if (_uiState.value.isRunning) return
        _uiState.update {
            it.copy(
                page = AppPage.Editor,
                phase = null,
                currentStep = -1,
                errorMessage = null,
                downloadMessage = null,
            )
        }
    }

    fun saveApkToDownloads() {
        val path = _uiState.value.apkPath ?: return showMessage("Build an APK before downloading.")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            showMessage("Choose Save APK to select the Downloads folder on this Android version.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val source = File(path)
            if (!source.exists()) {
                _uiState.update { it.copy(downloadMessage = "The APK cache expired. Run the build again.") }
                return@launch
            }
            val appName = _uiState.value.form.appName
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .take(40)
                .ifBlank { "ApexBuilder" }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$appName.apk")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            var destination: Uri? = null
            try {
                val created = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Android could not create a Downloads entry.")
                destination = created
                resolver.openOutputStream(created, "w")?.use { output ->
                    FileInputStream(source).use { input -> input.copyTo(output) }
                } ?: throw IOException("Android could not open the Downloads file.")
                resolver.update(created, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
                _uiState.update { it.copy(downloadMessage = "Saved to Downloads/$appName.apk") }
            } catch (cancelled: CancellationException) {
                destination?.let { resolver.delete(it, null, null) }
                throw cancelled
            } catch (error: Exception) {
                destination?.let { resolver.delete(it, null, null) }
                _uiState.update { it.copy(downloadMessage = error.message ?: "Could not save the APK.") }
            }
        }
    }

    fun copyApkTo(uri: Uri) {
        val path = _uiState.value.apkPath ?: return showMessage("Build an APK before downloading.")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val source = File(path)
                if (!source.exists()) throw IOException("The APK cache expired. Run the build again.")
                val resolver = getApplication<Application>().contentResolver
                resolver.openOutputStream(uri, "w")?.use { output ->
                    FileInputStream(source).use { input -> input.copyTo(output) }
                } ?: throw IOException("Could not open the selected destination.")
                _uiState.update { it.copy(downloadMessage = "APK saved successfully.") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(downloadMessage = error.message ?: "Could not save the APK.") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, downloadMessage = null) }
    }

    private fun handleProgress(update: BuildProgressUpdate) {
        _uiState.update { state ->
            state.copy(
                phase = update.phase,
                currentStep = update.step,
                logLines = (state.logLines + timestamped(update.message)).takeLast(250),
            )
        }
    }

    private fun validate(state: BuilderUiState): String? {
        val form = state.form
        val settings = state.github
        if (form.sourceMode == SourceMode.Upload && state.sourceUri == null) {
            return "Choose an HTML file or ZIP project first."
        }
        if (form.sourceMode == SourceMode.Website) {
            val uri = runCatching { Uri.parse(form.websiteUrl.trim()) }.getOrNull()
            if (uri?.scheme !in listOf("http", "https") || uri?.host.isNullOrBlank()) {
                return "Enter a valid website URL beginning with https://."
            }
        }
        if (form.appName.isBlank()) return "Enter an app name."
        if (form.appName.length > 50) return "App names must be 50 characters or fewer."
        if (!form.packageSuffix.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) {
            return "Use a valid package suffix, such as myapp."
        }
        if (form.splashMode == SplashMode.Custom && state.splashUri == null) {
            return "Upload an image or MP4 for the custom startup screen."
        }
        if (!settings.isComplete) return "Complete the GitHub owner, repository, workflow, branch, and token settings."
        if (!settings.owner.matches(Regex("[A-Za-z0-9-]{1,39}"))) return "Enter a valid GitHub owner name."
        if (!settings.repository.matches(Regex("[A-Za-z0-9._-]{1,100}"))) return "Enter a valid GitHub repository name."
        if (settings.branch.contains("..") || settings.branch.startsWith("-") || settings.branch.contains(" ")) {
            return "Enter a valid Git branch name."
        }
        val workflowName = settings.workflowFile.trim().substringAfterLast('/')
        if (!workflowName.matches(Regex("[A-Za-z0-9._-]+\\.ya?ml", RegexOption.IGNORE_CASE))) {
            return "Workflow file must end in .yml or .yaml."
        }
        return null
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun readableError(error: Exception): String {
        if (error is HttpException) {
            val body = runCatching { error.response()?.errorBody()?.use { it.string() } }.getOrNull()
                ?.take(350)
                .orEmpty()
            return "GitHub API returned ${error.code()}${if (body.isBlank()) "." else ": $body"}"
        }
        return error.message?.takeIf(String::isNotBlank) ?: "The build could not be completed."
    }

    private fun timestamped(message: String): String {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        return "$time  $message"
    }
}

class BuilderViewModelFactory(
    private val application: Application,
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BuilderViewModel::class.java)) {
            return BuilderViewModel(application, container) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}