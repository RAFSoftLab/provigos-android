package com.provigos.android.data.model

data class GithubRepoCommitModel(
    val url: String,
    val commit: UserCommitModel
)

data class UserCommitModel(
    val message: String,
    val author: CommitAuthorModel
)

data class CommitAuthorModel(
    val name: String,
    val date: String
)