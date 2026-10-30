package com.apexbuilder.app.data

import android.content.Context

class AppContainer(context: Context) {
    val githubSettingsStore = GitHubSettingsStore(context)
    val buildRepository = GitHubBuildRepository(
        context = context.applicationContext,
        api = GitHubApiFactory.create(),
    )
}