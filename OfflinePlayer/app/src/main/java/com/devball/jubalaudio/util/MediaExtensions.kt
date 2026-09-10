package com.devball.jubalaudio.util

import com.devball.jubalaudio.data.local.MediaEntity

 //Returns the common creator if all items in the list share the same one, otherwise returns an empty string.
fun List<MediaEntity>.getCommonCreator(): String {
    if (isEmpty()) return ""
    val firstCreator = first().creator
    return if (all { it.creator == firstCreator }) firstCreator else ""
}


//Returns the common artwork URI if all items in the list share the same one, otherwise returns null.
fun List<MediaEntity>.getCommonArtwork(): String? {
    if (isEmpty()) return null
    val firstArtwork = first().artworkUri
    return if (all { it.artworkUri == firstArtwork }) firstArtwork else null
}
