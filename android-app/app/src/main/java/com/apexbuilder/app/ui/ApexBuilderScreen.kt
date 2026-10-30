package com.apexbuilder.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexbuilder.app.BuilderViewModel
import com.apexbuilder.app.model.AppPage
import com.apexbuilder.app.model.BuildPhase
import com.apexbuilder.app.model.BuilderUiState
import com.apexbuilder.app.model.GitHubSettings
import com.apexbuilder.app.model.PermissionOption
import com.apexbuilder.app.model.SourceMode
import com.apexbuilder.app.model.SplashMode
import com.apexbuilder.app.model.UploadType
import com.apexbuilder.app.model.permissionOptions

private val PageBackground = Color(0xFF101310)
private val SurfaceColor = Color(0xFF171B18)
private val SurfaceRaised = Color(0xFF1E241E)
private val BorderColor = Color(0xFF323A32)
private val MutedText = Color(0xFF89948A)
private val SoftText = Color(0xFFBDC7BC)
private val Accent = Color(0xFFD1FA62)
private val Mono = FontFamily.Monospace

@Composable
fun ApexBuilderScreen(viewModel: BuilderViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Scaffold(
        containerColor = PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PageBackground),
        ) {
            if (state.page == AppPage.Progress) {
                BuildProgressScreen(state, viewModel)
            } else {
                BuilderEditorScreen(state, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuilderEditorScreen(state: BuilderUiState, viewModel: BuilderViewModel) {
    val context = LocalContext.current
    var advancedOpen by rememberSaveable { mutableStateOf(false) }
    var permissionsOpen by rememberSaveable { mutableStateOf(true) }
    var protectionOpen by rememberSaveable { mutableStateOf(false) }
    var backendOpen by rememberSaveable { mutableStateOf(false) }

    val sourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            holdReadPermission(context, uri)
            viewModel.setSource(uri, displayName(context, uri))
        }
    }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            holdReadPermission(context, uri)
            viewModel.setIcon(uri, displayName(context, uri))
        }
    }
    val googlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            holdReadPermission(context, uri)
            viewModel.setGoogleServices(uri, displayName(context, uri))
        }
    }
    val splashPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            holdReadPermission(context, uri)
            viewModel.setSplash(uri, displayName(context, uri))
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(subtitle = "NEW BUILD")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(25.dp))
            Text("APEX / ANDROID", color = Color(0xFF9BAA8A), fontSize = 10.sp, fontFamily = Mono, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Build your app.",
                color = Color(0xFFF0F3EE),
                fontSize = 29.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.7).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text("Turn a website or project folder into an Android build.", color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(21.dp))

            SectionCard(number = "01", title = "Choose a source", subtitle = "Start with a website or upload project files.") {
                SourceModePicker(state.form.sourceMode, onSelect = { mode ->
                    viewModel.updateForm { it.copy(sourceMode = mode) }
                })
                Spacer(Modifier.height(14.dp))
                if (state.form.sourceMode == SourceMode.Upload) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        UploadTypeButton(
                            modifier = Modifier.weight(1f),
                            title = "HTML file",
                            description = "One .html document",
                            active = state.form.uploadType == UploadType.Html,
                            marker = "HTML",
                            onClick = { viewModel.setUploadType(UploadType.Html) },
                        )
                        UploadTypeButton(
                            modifier = Modifier.weight(1f),
                            title = "ZIP project",
                            description = "Zipped project folder",
                            active = state.form.uploadType == UploadType.Zip,
                            marker = "ZIP",
                            onClick = { viewModel.setUploadType(UploadType.Zip) },
                        )
                    }
                    Spacer(Modifier.height(11.dp))
                    OutlinedButton(
                        onClick = {
                            val mimeTypes = if (state.form.uploadType == UploadType.Html) {
                                arrayOf("text/html", "text/*", "application/octet-stream")
                            } else {
                                arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                            }
                            sourcePicker.launch(mimeTypes)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftText),
                    ) {
                        Icon(if (state.sourceFileName == null) Icons.Outlined.FileUpload else Icons.Outlined.InsertDriveFile, null, tint = Accent)
                        Spacer(Modifier.width(9.dp))
                        Text(state.sourceFileName ?: "Choose file", maxLines = 1, fontSize = 13.sp)
                    }
                    Text(
                        state.sourceFileName?.let { "Selected: $it" } ?: "Maximum file size: 25 MB",
                        Modifier.padding(top = 7.dp, start = 2.dp),
                        color = MutedText,
                        fontSize = 10.sp,
                    )
                } else {
                    ApexTextField(
                        value = state.form.websiteUrl,
                        onValueChange = { value -> viewModel.updateForm { it.copy(websiteUrl = value) } },
                        label = "WEBSITE URL",
                        placeholder = "https://yourwebsite.com",
                        keyboardType = KeyboardType.Uri,
                    )
                    Text("The site will open inside a native Android WebView.", Modifier.padding(start = 2.dp, top = 6.dp), color = MutedText, fontSize = 10.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Outlined.Security, null, tint = Color(0xFF8E9D80), modifier = Modifier.size(15.dp))
                    Text("Files are sent directly to the configured GitHub repository.", color = MutedText, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionCard(number = "02", title = "App details", subtitle = "Set the identity and behavior of your app.") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceRaised),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.iconFileName != null) {
                                Text("IMG", color = Accent, fontSize = 12.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Outlined.Image, null, tint = Color(0xFF9DAA92), modifier = Modifier.size(24.dp))
                            }
                        }
                        TextButton(
                            onClick = { iconPicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                        ) {
                            Text(if (state.iconFileName == null) "APP ICON" else "REPLACE", color = Color(0xFFA9BF7F), fontSize = 8.sp, letterSpacing = 0.6.sp)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        ApexTextField(
                            value = state.form.appName,
                            onValueChange = { value -> viewModel.updateForm { it.copy(appName = value.take(50)) } },
                            label = "APP NAME",
                            placeholder = "My app",
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("PACKAGE NAME", color = MutedText, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.7.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF131714))
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("com.", color = Color(0xFFA7B298), fontSize = 13.sp, fontFamily = Mono)
                            OutlinedTextField(
                                value = state.form.packageSuffix,
                                onValueChange = { value ->
                                    viewModel.updateForm { form -> form.copy(packageSuffix = value.filter { it.isLetterOrDigit() || it == '_' }) }
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text("myapp", color = Color(0xFF657065)) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = Mono, fontSize = 13.sp),
                                colors = textFieldColors(),
                            )
                            if (state.form.packageSuffix.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) {
                                Icon(Icons.Outlined.Check, null, tint = Accent, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                ExpandableHeader(
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    title = "Advanced",
                    subtitle = "App links and Google Sign-In",
                    expanded = advancedOpen,
                    onClick = { advancedOpen = !advancedOpen },
                )
                AnimatedVisibility(advancedOpen) {
                    Column(Modifier.padding(start = 5.dp, end = 5.dp, bottom = 12.dp)) {
                        ApexTextField(
                            value = state.form.appLinksDomain,
                            onValueChange = { value -> viewModel.updateForm { it.copy(appLinksDomain = value) } },
                            label = "APP LINKS DOMAIN",
                            placeholder = "links.yourdomain.com",
                            keyboardType = KeyboardType.Uri,
                        )
                        Spacer(Modifier.height(12.dp))
                        FileOptionRow(
                            title = "Google Sign-In",
                            detail = state.googleServicesFileName ?: "Upload google-services.json",
                            buttonText = if (state.googleServicesFileName == null) "Upload JSON" else "Replace",
                            onClick = { googlePicker.launch(arrayOf("application/json", "text/json", "application/octet-stream")) },
                        )
                    }
                }

                ExpandableHeader(
                    icon = { Icon(Icons.Outlined.Lock, null) },
                    title = "Permissions",
                    subtitle = "${state.form.permissions.size} enabled",
                    expanded = permissionsOpen,
                    onClick = { permissionsOpen = !permissionsOpen },
                )
                AnimatedVisibility(permissionsOpen) {
                    Column(Modifier.padding(bottom = 10.dp)) {
                        permissionOptions.chunked(2).forEach { rowOptions ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowOptions.forEach { option ->
                                    PermissionToggle(
                                        option = option,
                                        checked = option.key in state.form.permissions,
                                        modifier = Modifier.weight(1f),
                                        onCheckedChange = { checked ->
                                            viewModel.updateForm { form ->
                                                val updated = if (checked) form.permissions + option.key else form.permissions - option.key
                                                form.copy(permissions = updated)
                                            }
                                        },
                                    )
                                }
                                if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                ExpandableHeader(
                    icon = { Icon(Icons.Outlined.Security, null) },
                    title = "Protection",
                    subtitle = "Obfuscation options",
                    expanded = protectionOpen,
                    onClick = { protectionOpen = !protectionOpen },
                )
                AnimatedVisibility(protectionOpen) {
                    Column(Modifier.padding(bottom = 8.dp)) {
                        SettingToggle(
                            title = "Dex2C protection",
                            description = "Native code hardening",
                            checked = state.form.dex2cProtection,
                            onCheckedChange = { checked -> viewModel.updateForm { it.copy(dex2cProtection = checked) } },
                        )
                        SettingToggle(
                            title = "String protection",
                            description = "Encrypt app strings",
                            checked = state.form.stringProtection,
                            onCheckedChange = { checked -> viewModel.updateForm { it.copy(stringProtection = checked) } },
                        )
                    }
                }

                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                SettingToggle(
                    title = "Cache pages offline",
                    description = "Keep pages available without a connection",
                    checked = state.form.cachePagesOffline,
                    icon = { Icon(Icons.Outlined.Cached, null) },
                    onCheckedChange = { checked -> viewModel.updateForm { it.copy(cachePagesOffline = checked) } },
                )

                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                Text("STARTUP LOADING", color = MutedText, fontSize = 9.sp, fontFamily = Mono, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(9.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SplashMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.form.splashMode == mode,
                            onClick = { viewModel.updateForm { it.copy(splashMode = mode) } },
                            label = { Text(mode.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2B3520),
                                selectedLabelColor = Color(0xFFD1E7AD),
                                labelColor = MutedText,
                            ),
                        )
                    }
                }
                if (state.form.splashMode == SplashMode.Custom) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { splashPicker.launch(arrayOf("image/*", "video/mp4")) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BorderColor),
                    ) {
                        Icon(Icons.Outlined.FileUpload, null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Text(state.splashFileName ?: "Upload MP4 or image", maxLines = 1)
                    }
                }

                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                SettingToggle(
                    title = "Sign as release build",
                    description = "Prepare a release-signed APK",
                    checked = state.form.signAsRelease,
                    icon = { Icon(Icons.Outlined.Security, null) },
                    onCheckedChange = { checked -> viewModel.updateForm { it.copy(signAsRelease = checked) } },
                )
            }

            Spacer(Modifier.height(14.dp))
            SectionCard(number = "03", title = "GitHub Actions", subtitle = "Connect a workflow to compile your APK.") {
                ExpandableHeader(
                    icon = { Text("GH", color = Color(0xFFC9D4C9), fontSize = 9.sp, fontFamily = Mono) },
                    title = "Repository credentials",
                    subtitle = if (state.github.isComplete) "Settings saved on this device" else "Repository, workflow, branch and token",
                    expanded = backendOpen,
                    onClick = { backendOpen = !backendOpen },
                )
                AnimatedVisibility(backendOpen) {
                    GitHubSettingsForm(
                        settings = state.github,
                        onSettingsChange = viewModel::updateGitHub,
                        onSave = viewModel::saveGitHubSettings,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF20251D))
                        .padding(11.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Info, null, tint = Color(0xFFB9D77E), modifier = Modifier.size(16.dp))
                    Text(
                        "The workflow must accept build_id and config_path inputs and upload an artifact named apexbuilder-{build_id}.",
                        color = Color(0xFFA9B5A2),
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Use a fine-grained token with Contents read/write and Actions read/write access. Release signing keys belong in GitHub Actions secrets.",
                    color = MutedText,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.height(22.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF141815))
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            Button(
                onClick = viewModel::startBuild,
                enabled = !state.isRunning,
                modifier = Modifier.fillMaxWidth().height(49.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color(0xFF1A2214),
                    disabledContainerColor = Color(0xFF333A31),
                    disabledContentColor = Color(0xFF7E887D),
                ),
            ) {
                Icon(Icons.Outlined.CloudUpload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text("Build APK", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Source, config, workflow, artifact", Modifier.align(Alignment.CenterHorizontally).padding(top = 7.dp), color = MutedText, fontSize = 9.sp)
        }
    }
}

@Composable
private fun BuildProgressScreen(state: BuilderUiState, viewModel: BuilderViewModel) {
    val logScrollState = rememberScrollState()
    val fileName = "${state.form.appName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "ApexBuilder" }}.apk"
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")) { uri ->
        if (uri != null) viewModel.copyApkTo(uri)
    }

    LaunchedEffect(state.logLines.size) { logScrollState.animateScrollTo(logScrollState.maxValue) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF141815))
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::returnToEditor, enabled = !state.isRunning) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to builder", tint = SoftText)
            }
            Column(Modifier.weight(1f)) {
                Text("BUILD SESSION", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text("ApexBuilder / cloud", color = MutedText, fontSize = 9.sp)
            }
            PhaseBadge(state.phase)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("BUILD / ${state.buildId?.take(8)?.uppercase() ?: "SESSION"}", color = Color(0xFF9BAA8A), fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(9.dp))
            Text(
                when (state.phase) {
                    BuildPhase.Ready -> "Your APK is ready."
                    BuildPhase.Failed -> "The build stopped."
                    else -> "Putting it together."
                },
                color = Color(0xFFF0F3EE),
                fontSize = 26.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text("${state.form.appName.ifBlank { "Your app" }}  /  com.${state.form.packageSuffix}", color = MutedText, fontSize = 11.sp, fontFamily = Mono)
            Spacer(Modifier.height(22.dp))

            SectionCard(title = "Build pipeline", subtitle = "Upload, prepare, build, then download.") {
                val stepNames = listOf("Upload", "Prepare", "Build", "Ready")
                val descriptions = listOf("Project files and assets", "App configuration", "GitHub Actions compile", "Signed APK artifact")
                stepNames.forEachIndexed { index, title ->
                    val complete = state.phase == BuildPhase.Ready || index < state.currentStep
                    val active = index == state.currentStep && state.phase != BuildPhase.Ready && state.phase != BuildPhase.Failed
                    BuildStepRow(
                        title = title,
                        description = descriptions[index],
                        complete = complete,
                        active = active,
                        failed = state.phase == BuildPhase.Failed && index == state.currentStep,
                    )
                }
                if (state.errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151915)),
                border = BorderStroke(1.dp, BorderColor),
            ) {
                Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Code, null, tint = Accent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Live output", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(if (state.isRunning) "STREAMING" else "SESSION LOG", color = MutedText, fontSize = 8.sp, fontFamily = Mono, letterSpacing = 0.8.sp)
                    }
                    Divider(color = BorderColor, modifier = Modifier.padding(vertical = 11.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 250.dp)
                            .verticalScroll(logScrollState),
                    ) {
                        state.logLines.forEach { line ->
                            Text(line, color = Color(0xFFADB9AC), fontSize = 9.sp, lineHeight = 16.sp, fontFamily = Mono)
                        }
                        if (state.isRunning) {
                            Text("Working...", color = Color(0xFFBFD87B), fontSize = 9.sp, fontFamily = Mono)
                        }
                    }
                }
            }

            if (state.phase == BuildPhase.Ready) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF20271C)),
                    border = BorderStroke(1.dp, Color(0xFF455638)),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Outlined.Info, null, tint = Accent, modifier = Modifier.size(16.dp))
                        Text("APK downloads are saved to the public Downloads folder. Android does not need broad storage permission.", color = Color(0xFFB8C5B0), fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
            }
            state.downloadMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = if (it.startsWith("Saved") || it.startsWith("APK saved")) Accent else MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
            Spacer(Modifier.height(20.dp))
        }

        if (state.phase == BuildPhase.Ready) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141815))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) viewModel.saveApkToDownloads()
                        else createDocument.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth().height(49.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF1A2214)),
                ) {
                    Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Download APK", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("${fileName}  /  GitHub Actions artifact", Modifier.align(Alignment.CenterHorizontally).padding(top = 7.dp), color = MutedText, fontSize = 9.sp)
            }
        } else if (state.phase == BuildPhase.Failed) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141815))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = viewModel::returnToEditor, modifier = Modifier.weight(1f)) { Text("Edit settings") }
                Button(onClick = viewModel::startBuild, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF1A2214))) {
                    Text("Retry build")
                }
            }
        }
    }
}

@Composable
private fun AppTopBar(subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF141815))
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .height(57.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF29331F)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.CloudUpload, null, tint = Accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("APEX", color = Color(0xFFF0F3ED), fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.1.sp)
                Spacer(Modifier.width(5.dp))
                Text("BUILDER", color = MutedText, fontSize = 11.sp, letterSpacing = 0.7.sp)
            }
            Text(subtitle, color = Color(0xFF69756B), fontFamily = Mono, fontSize = 7.sp, letterSpacing = 0.8.sp)
        }
        Text("ANDROID  /  1.0", color = Color(0xFF7F8B7D), fontSize = 8.sp, fontFamily = Mono)
    }
}

@Composable
private fun SectionCard(
    number: String? = null,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (number != null) {
                    Box(
                        Modifier
                            .size(25.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF2B3423)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(number, color = Color(0xFFD0E6A5), fontSize = 9.sp, fontFamily = Mono)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color(0xFFE5EAE3), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
            Spacer(Modifier.height(15.dp))
            content()
        }
    }
}

@Composable
private fun SourceModePicker(sourceMode: SourceMode, onSelect: (SourceMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111511))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SourceMode.entries.forEach { mode ->
            val selected = sourceMode == mode
            OutlinedButton(
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (selected) Color(0xFF3C4838) else Color.Transparent),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) Color(0xFF2A3128) else Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 7.dp),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (mode == SourceMode.Upload) Icons.Outlined.CloudUpload else Icons.Outlined.Language, null, tint = if (selected) Accent else MutedText, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (mode == SourceMode.Upload) "Upload" else "Website", color = if (selected) Color(0xFFE0E8DD) else MutedText, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun UploadTypeButton(
    modifier: Modifier,
    title: String,
    description: String,
    active: Boolean,
    marker: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 65.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (active) Color(0xFF586843) else BorderColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (active) Color(0xFF20271C) else Color(0xFF141814)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(marker, color = if (active) Accent else Color(0xFF9AA495), fontSize = 8.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(title, color = Color(0xFFD4DBD2), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = MutedText, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 9.sp, fontFamily = Mono, letterSpacing = 0.7.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF667166), fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
        shape = RoundedCornerShape(9.dp),
        colors = textFieldColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFFE5EBE3),
    unfocusedTextColor = Color(0xFFD8DED7),
    focusedBorderColor = Color(0xFF8BA35B),
    unfocusedBorderColor = BorderColor,
    focusedLabelColor = Color(0xFFB7D47B),
    unfocusedLabelColor = MutedText,
    cursorColor = Accent,
    focusedContainerColor = Color(0xFF131714),
    unfocusedContainerColor = Color(0xFF131714),
)

@Composable
private fun ExpandableHeader(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
    ) {
        Row(
            Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222822)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides Color(0xFFAFC195),
                    content = icon,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MutedText, fontSize = 9.sp)
            }
            Icon(Icons.Outlined.ExpandMore, null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PermissionToggle(
    option: PermissionOption,
    checked: Boolean,
    modifier: Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF141815))
            .padding(start = 9.dp, end = 5.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(option.title, color = SoftText, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(option.description, color = MutedText, fontSize = 8.sp, maxLines = 1)
        }
        ApexSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF222822)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides Color(0xFFAFC195),
                    content = icon,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = SoftText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = MutedText, fontSize = 9.sp)
        }
        ApexSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ApexSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF1D2817),
            checkedTrackColor = Accent,
            checkedBorderColor = Accent,
            uncheckedThumbColor = Color(0xFF9AA49A),
            uncheckedTrackColor = Color(0xFF363D36),
            uncheckedBorderColor = Color(0xFF4A534A),
        ),
    )
}

@Composable
private fun FileOptionRow(title: String, detail: String, buttonText: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = SoftText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = MutedText, fontSize = 9.sp, maxLines = 1)
        }
        OutlinedButton(
            onClick = onClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, BorderColor),
        ) { Text(buttonText, fontSize = 9.sp) }
    }
}

@Composable
private fun GitHubSettingsForm(
    settings: GitHubSettings,
    onSettingsChange: ((GitHubSettings) -> GitHubSettings) -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 11.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        ApexTextField(settings.owner, { value -> onSettingsChange { it.copy(owner = value) } }, "OWNER", "your-username")
        ApexTextField(settings.repository, { value -> onSettingsChange { it.copy(repository = value) } }, "REPOSITORY", "web-app-builder")
        ApexTextField(settings.branch, { value -> onSettingsChange { it.copy(branch = value) } }, "BRANCH", "main")
        ApexTextField(settings.workflowFile, { value -> onSettingsChange { it.copy(workflowFile = value) } }, "WORKFLOW FILE", "build-apk.yml")
        ApexTextField(
            value = settings.token,
            onValueChange = { value -> onSettingsChange { it.copy(token = value) } },
            label = "PERSONAL ACCESS TOKEN",
            placeholder = "GitHub token",
            visualTransformation = PasswordVisualTransformation(),
        )
        Text(
            "Token is encrypted with Android Keystore and never written to build logs.",
            color = MutedText,
            fontSize = 9.sp,
            lineHeight = 14.sp,
        )
        OutlinedButton(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF566841)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0E5A5)),
        ) { Text("Save GitHub settings", fontSize = 11.sp) }
    }
}

@Composable
private fun BuildStepRow(
    title: String,
    description: String,
    complete: Boolean,
    active: Boolean,
    failed: Boolean,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(when {
                    complete -> Color(0xFF2B3622)
                    failed -> Color(0xFF472A25)
                    active -> Color(0xFF272F21)
                    else -> Color(0xFF222722)
                }),
            contentAlignment = Alignment.Center,
        ) {
            if (complete) {
                Icon(Icons.Outlined.Check, null, tint = Accent, modifier = Modifier.size(14.dp))
            } else if (active) {
                CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 1.7.dp, color = Accent)
            } else if (failed) {
                Text("!", color = Color(0xFFFF9786), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(title.take(1), color = MutedText, fontSize = 9.sp, fontFamily = Mono)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = if (complete || active) SoftText else MutedText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = MutedText, fontSize = 9.sp)
        }
        if (complete) Text("DONE", color = Color(0xFF9DBF6A), fontSize = 8.sp, fontFamily = Mono)
        if (active) Text("RUNNING", color = Color(0xFFBFD97A), fontSize = 8.sp, fontFamily = Mono)
    }
}

@Composable
private fun PhaseBadge(phase: BuildPhase?) {
    val label = when (phase) {
        BuildPhase.Ready -> "READY"
        BuildPhase.Failed -> "FAILED"
        BuildPhase.Upload, BuildPhase.Prepare, BuildPhase.Build -> "RUNNING"
        null -> "IDLE"
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (phase == BuildPhase.Ready) Color(0xFF29351F) else Color(0xFF252B25))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (phase == BuildPhase.Ready) Accent else Color(0xFF8F9B8D)))
        Text(label, color = if (phase == BuildPhase.Ready) Color(0xFFD3E5B5) else MutedText, fontSize = 8.sp, fontFamily = Mono, letterSpacing = 0.5.sp)
    }
}

private fun holdReadPermission(context: android.content.Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex >= 0) cursor.getString(columnIndex)?.let { return it }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "selected-file"
}