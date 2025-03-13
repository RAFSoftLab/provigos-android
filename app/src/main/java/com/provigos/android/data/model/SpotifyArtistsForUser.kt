package com.provigos.android.data.model

data class SpotifyArtistsForUser(
    val items: List<SpotifyArtist>
)

data class SpotifyArtist(
    val genres: List<String>,
    val popularity: Int
)