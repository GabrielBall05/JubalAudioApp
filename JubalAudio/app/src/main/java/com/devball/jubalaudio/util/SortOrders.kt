package com.devball.jubalaudio.util

interface SortOption {
    val label: String
}

enum class MediaSortOrder(override val label: String) : SortOption {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    CREATOR_ASC ("Creator (A-Z)"),
    CREATOR_DESC("Creator (Z-A)"),
    DURATION_ASC("Duration (Low-High)"),
    DURATION_DESC("Duration (High-Low)"),
    DATE_ADDED_MOST_RECENT("Date Added (Most Recent)"),
    DATE_ADDED_LEAST_RECENT("Date Added (Least Recent)");
}

enum class PlaylistsSortOrder(override val label: String): SortOption {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DATE_CREATED_MOST_RECENT("Date Created (Most Recent)"),
    DATE_CREATED_LEAST_RECENT("Date Created (Least Recent)");
}