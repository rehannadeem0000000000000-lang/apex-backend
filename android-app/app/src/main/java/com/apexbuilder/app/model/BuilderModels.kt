package com.apexbuilder.app.model

import android.net.Uri

enum class SourceMode {
    Upload,
    Website,
}

enum class UploadType {
    Html,
    Zip,
}

enum class SplashMode {
    Off,
    None,
    Custom,
}

enum class BuildPhase {
    Upload,
    Prepare,
    Build,
    Ready,
    Failed,
}

enum class AppPage {
    Editor,
    Progress,
}

data class BuilderForm(
    val sourceMode: SourceMode = SourceMode.Upload,
    val uploadType: UploadType = UploadType.Html,
    val websiteUrl: String = "",
    val appName: String = "",
    val packageSuffix: String = "myapp",
    val appLinksDomain: String = "",
    val permissions: Set<String> = setOf("INTERNET"),
    val dex2cProtection: Boolean = false,
    val stringProtection: Boolean = false,
    val cachePagesOffline: Boolean = false,
    val splashMode: SplashMode = SplashMode.Off,
    val signAsRelease: Boolean = true,
)

data class GitHubSettings(
    val owner: String = "",
    val repository: String = "",
    val branch: String = "main",
    val workflowFile: String = "build-apk.yml",
    val token: String = "",
) {
    val isComplete: Boolean
        get() = owner.isNotBlank() &&
            repository.isNotBlank() &&
            branch.isNotBlank() &&
            workflowFile.isNotBlank() &&
            token.isNotBlank()
}

data class BuilderUiState(
    val form: BuilderForm = BuilderForm(),
    val sourceUri: Uri? = null,
    val sourceFileName: String? = null,
    val iconUri: Uri? = null,
    val iconFileName: String? = null,
    val googleServicesUri: Uri? = null,
    val googleServicesFileName: String? = null,
    val splashUri: Uri? = null,
    val splashFileName: String? = null,
    val github: GitHubSettings = GitHubSettings(),
    val page: AppPage = AppPage.Editor,
    val phase: BuildPhase? = null,
    val currentStep: Int = -1,
    val isRunning: Boolean = false,
    val logLines: List<String> = emptyList(),
    val buildId: String? = null,
    val apkPath: String? = null,
    val errorMessage: String? = null,
    val downloadMessage: String? = null,
    val message: String? = null,
)

data class BuildInput(
    val form: BuilderForm,
    val sourceUri: Uri?,
    val sourceFileName: String?,
    val iconUri: Uri?,
    val iconFileName: String?,
    val googleServicesUri: Uri?,
    val googleServicesFileName: String?,
    val splashUri: Uri?,
    val splashFileName: String?,
    val github: GitHubSettings,
)

data class BuildResult(
    val buildId: String,
    val apkFile: java.io.File,
)

data class BuildProgressUpdate(
    val phase: BuildPhase,
    val step: Int,
    val message: String,
)

data class FileAsset(
    val fileName: String,
    val bytes: ByteArray,
)

data class PermissionOption(
    val key: String,
    val title: String,
    val description: String,
)

val permissionOptions = listOf(
    PermissionOption("INTERNET", "Internet", "Network access"),
    PermissionOption("CAMERA", "Camera", "Capture photos"),
    PermissionOption("MICROPHONE", "Microphone", "Record audio"),
    PermissionOption("LOCATION", "Location", "Precise location"),
    PermissionOption("STORAGE", "Storage", "Read local files"),
    PermissionOption("CONTACTS", "Contacts", "Read contacts"),
    PermissionOption("SMS", "SMS", "Read and send SMS"),
    PermissionOption("BLUETOOTH", "Bluetooth", "Nearby devices"),
    PermissionOption("VIBRATE", "Vibrate", "Haptic feedback"),
    PermissionOption("INSTALL_UNKNOWN_APPS", "Install apps", "Unknown sources"),
)