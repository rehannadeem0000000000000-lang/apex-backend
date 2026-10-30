package com.apexbuilder.app.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.apexbuilder.app.model.BuildInput
import com.apexbuilder.app.model.BuildProgressUpdate
import com.apexbuilder.app.model.BuildPhase
import com.apexbuilder.app.model.BuildResult
import com.apexbuilder.app.model.FileAsset
import com.apexbuilder.app.model.SourceMode
import com.apexbuilder.app.model.UploadType
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipInputStream

class GitHubBuildRepository(
    private val context: Context,
    private val api: GitHubApi,
) {
    private val assetReader = AssetReader(context)
    private val json = Json { encodeDefaults = true; explicitNulls = false }

    suspend fun build(
        input: BuildInput,
        onProgress: (BuildProgressUpdate) -> Unit,
    ): BuildResult {
        validateSettings(input)
        val buildId = UUID.randomUUID().toString()
        val owner = input.github.owner.trim()
        val repository = input.github.repository.trim()
        val branch = input.github.branch.trim()
        val workflow = input.github.workflowFile.trim().substringAfterLast('/')
        if (!workflow.matches(Regex("[A-Za-z0-9._-]+\\.ya?ml", RegexOption.IGNORE_CASE))) {
            throw IOException("Enter a workflow filename ending in .yml or .yaml.")
        }
        val authorization = "Bearer ${input.github.token.trim()}"
        val rootPath = "apexbuilder-inputs/$buildId"
        val uploadedPaths = LinkedHashMap<String, String?>()

        onProgress(BuildProgressUpdate(BuildPhase.Upload, 0, "Starting build $buildId"))

        val sourceAsset = if (input.form.sourceMode == SourceMode.Upload) {
            val sourceUri = input.sourceUri ?: throw IOException("Choose an HTML or ZIP source file first.")
            val fallback = if (input.form.uploadType == UploadType.Html) "index.html" else "project.zip"
            assetReader.read(sourceUri, fallback).also { asset ->
                val path = "$rootPath/source/${asset.fileName}"
                uploadAsset(authorization, owner, repository, branch, path, asset)
                uploadedPaths["source_path"] = path
                onProgress(BuildProgressUpdate(BuildPhase.Upload, 0, "Uploaded source file ${asset.fileName}"))
            }
        } else {
            null
        }

        val iconPath = uploadOptionalAsset(
            authorization = authorization,
            owner = owner,
            repository = repository,
            branch = branch,
            rootPath = rootPath,
            uri = input.iconUri,
            fallbackName = input.iconFileName ?: "app-icon.png",
            assetDirectory = "icon",
            onUploaded = { path ->
                uploadedPaths["icon_path"] = path
                onProgress(BuildProgressUpdate(BuildPhase.Upload, 0, "Uploaded app icon"))
            },
        )
        val googleServicesPath = uploadOptionalAsset(
            authorization = authorization,
            owner = owner,
            repository = repository,
            branch = branch,
            rootPath = rootPath,
            uri = input.googleServicesUri,
            fallbackName = input.googleServicesFileName ?: "google-services.json",
            assetDirectory = "google-services",
            onUploaded = { path ->
                uploadedPaths["google_services_path"] = path
                onProgress(BuildProgressUpdate(BuildPhase.Upload, 0, "Uploaded Google services file"))
            },
        )
        val splashPath = uploadOptionalAsset(
            authorization = authorization,
            owner = owner,
            repository = repository,
            branch = branch,
            rootPath = rootPath,
            uri = input.splashUri,
            fallbackName = input.splashFileName ?: "splash-asset",
            assetDirectory = "splash",
            onUploaded = { path ->
                uploadedPaths["splash_asset_path"] = path
                onProgress(BuildProgressUpdate(BuildPhase.Upload, 0, "Uploaded splash asset"))
            },
        )

        val sourceType = when (input.form.sourceMode) {
            SourceMode.Website -> "website"
            SourceMode.Upload -> if (input.form.uploadType == UploadType.Html) "html" else "zip"
        }
        val config = CloudBuildConfig(
            buildId = buildId,
            appName = input.form.appName.trim(),
            packageName = "com.${input.form.packageSuffix.trim()}",
            sourceType = sourceType,
            websiteUrl = input.form.websiteUrl.trim().takeIf { input.form.sourceMode == SourceMode.Website },
            sourcePath = sourceAsset?.let { uploadedPaths["source_path"] },
            appLinksDomain = input.form.appLinksDomain.trim().ifBlank { null },
            iconPath = iconPath,
            googleServicesPath = googleServicesPath,
            permissions = input.form.permissions.sorted(),
            protection = CloudProtectionConfig(
                dex2c = input.form.dex2cProtection,
                strings = input.form.stringProtection,
            ),
            cachePagesOffline = input.form.cachePagesOffline,
            startup = CloudStartupConfig(
                mode = input.form.splashMode.name.lowercase(),
                assetPath = splashPath,
            ),
            signAsRelease = input.form.signAsRelease,
        )
        val configPath = "$rootPath/config.json"
        val configAsset = FileAsset(
            fileName = "config.json",
            bytes = json.encodeToString(config).toByteArray(Charsets.UTF_8),
        )
        uploadAsset(authorization, owner, repository, branch, configPath, configAsset)
        onProgress(BuildProgressUpdate(BuildPhase.Prepare, 1, "Uploaded build configuration"))

        val dispatchStartedAt = Instant.now()
        val dispatchResponse = api.dispatchWorkflow(
            authorization = authorization,
            owner = owner,
            repository = repository,
            workflow = workflow,
            body = WorkflowDispatchRequest(
                ref = branch,
                inputs = buildMap {
                    put("build_id", buildId)
                    put("config_path", configPath)
                    uploadedPaths["source_path"]?.let { put("source_path", it) }
                },
            ),
        )
        if (!dispatchResponse.isSuccessful) {
            val details = dispatchResponse.errorBody()?.use(ResponseBody::string).orEmpty()
            throw IOException("GitHub rejected workflow dispatch (${dispatchResponse.code()}): ${details.take(400)}")
        }
        dispatchResponse.body()?.close()
        onProgress(BuildProgressUpdate(BuildPhase.Build, 2, "GitHub Actions workflow dispatched"))

        val run = awaitWorkflowRun(
            authorization = authorization,
            owner = owner,
            repository = repository,
            workflow = workflow,
            branch = branch,
            dispatchStartedAt = dispatchStartedAt,
            onProgress = onProgress,
        )
        val artifacts = api.artifacts(authorization, owner, repository, run.id).artifacts
        val artifact = artifacts.firstOrNull { !it.expired && it.name == "apexbuilder-$buildId" }
            ?: throw IOException("The workflow completed, but no downloadable artifact was uploaded.")

        val artifactResponse = api.downloadArtifact(authorization, owner, repository, artifact.id)
        if (!artifactResponse.isSuccessful) {
            val details = artifactResponse.errorBody()?.use(ResponseBody::string).orEmpty()
            throw IOException("Could not download the build artifact (${artifactResponse.code()}): ${details.take(400)}")
        }
        val body = artifactResponse.body() ?: throw IOException("GitHub returned an empty artifact archive.")
        if (body.contentLength() > MAX_ARTIFACT_ARCHIVE_BYTES) {
            body.close()
            throw IOException("The build artifact is larger than 250 MB.")
        }
        val apk = extractApk(body, buildId)
        onProgress(BuildProgressUpdate(BuildPhase.Ready, 3, "APK downloaded and ready to save"))
        return BuildResult(buildId = buildId, apkFile = apk)
    }

    private suspend fun uploadOptionalAsset(
        authorization: String,
        owner: String,
        repository: String,
        branch: String,
        rootPath: String,
        uri: Uri?,
        fallbackName: String,
        assetDirectory: String,
        onUploaded: (String) -> Unit,
    ): String? {
        if (uri == null) return null
        val asset = assetReader.read(uri, fallbackName)
        val path = "$rootPath/assets/$assetDirectory/${asset.fileName}"
        uploadAsset(authorization, owner, repository, branch, path, asset)
        onUploaded(path)
        return path
    }

    private suspend fun uploadAsset(
        authorization: String,
        owner: String,
        repository: String,
        branch: String,
        path: String,
        asset: FileAsset,
    ) {
        api.putContent(
            authorization = authorization,
            owner = owner,
            repository = repository,
            path = path,
            body = PutContentRequest(
                message = "ApexBuilder input ${path.substringAfterLast('/')}",
                content = Base64.encodeToString(asset.bytes, Base64.NO_WRAP),
                branch = branch,
            ),
        )
    }

    private suspend fun awaitWorkflowRun(
        authorization: String,
        owner: String,
        repository: String,
        workflow: String,
        branch: String,
        dispatchStartedAt: Instant,
        onProgress: (BuildProgressUpdate) -> Unit,
    ): WorkflowRun {
        val deadline = System.currentTimeMillis() + BUILD_TIMEOUT_MILLIS
        var run: WorkflowRun? = null
        var reportedStatus = ""
        var reportedWaiting = false

        while (System.currentTimeMillis() < deadline) {
            val runs = api.workflowRuns(
                authorization = authorization,
                owner = owner,
                repository = repository,
                workflow = workflow,
                branch = branch,
            ).workflowRuns

            if (run == null) {
                run = runs
                    .asSequence()
                    .filter { it.event == "workflow_dispatch" && it.headBranch == branch }
                    .filter { candidate ->
                        val createdAt = runCatching { Instant.parse(candidate.createdAt) }.getOrNull()
                        createdAt != null && !createdAt.isBefore(dispatchStartedAt.minusSeconds(15))
                    }
                    .maxByOrNull { it.createdAt }
                if (run == null && !reportedWaiting) {
                    onProgress(BuildProgressUpdate(BuildPhase.Build, 2, "Waiting for the dispatched workflow run"))
                    reportedWaiting = true
                }
            }

            if (run != null) {
                val currentId = run.id
                val latest = runs.firstOrNull { it.id == currentId } ?: run
                run = latest
                if (latest.status != reportedStatus) {
                    reportedStatus = latest.status
                    onProgress(BuildProgressUpdate(BuildPhase.Build, 2, "Workflow status: ${latest.status}"))
                }
                if (latest.status == "completed") {
                    if (latest.conclusion != "success") {
                        val link = latest.htmlUrl?.let { " $it" }.orEmpty()
                        throw IOException("GitHub Actions finished with ${latest.conclusion ?: "an unknown result"}.$link")
                    }
                    return latest
                }
            }
            delay(POLL_INTERVAL_MILLIS)
        }
        throw IOException("The GitHub Actions build timed out after 15 minutes.")
    }

    private fun extractApk(body: ResponseBody, buildId: String): File {
        val outputFile = File(context.cacheDir, "apexbuilder-$buildId.apk")
        var totalBytes = 0L
        try {
            ZipInputStream(body.byteStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                if (totalBytes > MAX_APK_BYTES) {
                                    throw IOException("The APK is larger than 200 MB.")
                                }
                                output.write(buffer, 0, count)
                            }
                        }
                        if (outputFile.length() == 0L) throw IOException("The APK artifact was empty.")
                        return outputFile
                    }
                    zip.closeEntry()
                }
            }
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        }
        outputFile.delete()
        throw IOException("No APK file was found inside the GitHub artifact archive.")
    }

    private fun validateSettings(input: BuildInput) {
        val settings = input.github
        if (!settings.isComplete) throw IOException("Complete the GitHub repository settings before building.")
        if (!settings.owner.matches(Regex("[A-Za-z0-9-]{1,39}"))) {
            throw IOException("Enter a valid GitHub owner name.")
        }
        if (!settings.repository.matches(Regex("[A-Za-z0-9._-]{1,100}"))) {
            throw IOException("Enter a valid GitHub repository name.")
        }
        if (settings.branch.contains("..") || settings.branch.startsWith("-") || settings.branch.contains(" ")) {
            throw IOException("Enter a valid Git branch name.")
        }
        val packageSuffix = input.form.packageSuffix.trim()
        if (!packageSuffix.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) {
            throw IOException("Enter a valid package suffix, such as myapp.")
        }
        if (input.form.appName.isBlank()) throw IOException("Enter an app name before building.")
        if (input.form.sourceMode == SourceMode.Website && !input.form.websiteUrl.matches(Regex("https?://.+", RegexOption.IGNORE_CASE))) {
            throw IOException("Enter a valid website URL beginning with https://.")
        }
    }

    @Serializable
    private data class CloudBuildConfig(
        @SerialName("build_id") val buildId: String,
        @SerialName("app_name") val appName: String,
        @SerialName("package_name") val packageName: String,
        @SerialName("source_type") val sourceType: String,
        @SerialName("website_url") val websiteUrl: String? = null,
        @SerialName("source_path") val sourcePath: String? = null,
        @SerialName("app_links_domain") val appLinksDomain: String? = null,
        @SerialName("icon_path") val iconPath: String? = null,
        @SerialName("google_services_path") val googleServicesPath: String? = null,
        val permissions: List<String>,
        val protection: CloudProtectionConfig,
        @SerialName("cache_pages_offline") val cachePagesOffline: Boolean,
        val startup: CloudStartupConfig,
        @SerialName("sign_as_release") val signAsRelease: Boolean,
    )

    @Serializable
    private data class CloudProtectionConfig(
        val dex2c: Boolean,
        val strings: Boolean,
    )

    @Serializable
    private data class CloudStartupConfig(
        val mode: String,
        @SerialName("asset_path") val assetPath: String? = null,
    )

    private companion object {
        const val POLL_INTERVAL_MILLIS = 5_000L
        const val BUILD_TIMEOUT_MILLIS = 15L * 60L * 1_000L
        const val MAX_ARTIFACT_ARCHIVE_BYTES = 250L * 1024L * 1024L
        const val MAX_APK_BYTES = 200L * 1024L * 1024L
    }
}