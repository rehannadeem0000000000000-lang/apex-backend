package com.apexbuilder.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PutContentRequest(
    val message: String,
    val content: String,
    val branch: String,
)

@Serializable
data class GitHubContentResponse(
    val content: GitHubContent? = null,
)

@Serializable
data class GitHubContent(
    val name: String? = null,
    val path: String? = null,
    val sha: String? = null,
)

@Serializable
data class WorkflowDispatchRequest(
    val ref: String,
    val inputs: Map<String, String>,
)

@Serializable
data class WorkflowRunsResponse(
    @SerialName("workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList(),
)

@Serializable
data class WorkflowRun(
    val id: Long,
    val name: String? = null,
    val event: String = "",
    val status: String = "",
    val conclusion: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("head_branch") val headBranch: String = "",
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class WorkflowArtifactsResponse(
    val artifacts: List<WorkflowArtifact> = emptyList(),
)

@Serializable
data class WorkflowArtifact(
    val id: Long,
    val name: String,
    @SerialName("expired") val expired: Boolean = false,
)