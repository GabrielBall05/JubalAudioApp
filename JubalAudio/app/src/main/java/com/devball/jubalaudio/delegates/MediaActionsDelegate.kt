package com.devball.jubalaudio.delegates

import android.net.Uri
import com.devball.jubalaudio.data.local.entity.MediaEntity
import com.devball.jubalaudio.data.repository.MediaRepository
import com.devball.jubalaudio.viewmodels.UiActionHandler
import javax.inject.Inject

interface MediaActionsDelegate {
    fun deleteMediaByIds(ids: List<Int>)
    fun updateMediaItem(item: MediaEntity)
    fun updateCreatorBulk(creator: String, ids: List<Int>)
    fun updateArtworkBulk(artworkUri: String?, ids: List<Int>)
    fun relinkMedia(mediaId: Int, newUri: Uri)
    fun bindMediaActions(handler: UiActionHandler)
}

class MediaActionsDelegateImpl @Inject constructor(
    private val mediaRepository: MediaRepository
) : MediaActionsDelegate {
    private var uiHandler: UiActionHandler? = null

    override fun bindMediaActions(handler: UiActionHandler) { this.uiHandler = handler }
    private fun handler() = uiHandler ?: throw IllegalStateException("Delegate not bound")

    override fun deleteMediaByIds(ids: List<Int>) = handler().launchWithLoading {
        mediaRepository.deleteMediaList(ids)
        handler().showToast("Removed ${ids.size} item${if (ids.size > 1) "s" else ""} from library")
    }

    override fun updateMediaItem(item: MediaEntity) = handler().launchWithoutLoading {
        mediaRepository.updateMedia(item)
        handler().showToast("Changes saved")
    }

    override fun updateCreatorBulk(creator: String, ids: List<Int>) = handler().launchWithLoading {
        mediaRepository.updateCreatorBulk(creator, ids)
        handler().showToast("${ids.size} item${if (ids.size > 1) "s" else ""} updated")
    }

    override fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) = handler().launchWithLoading {
        mediaRepository.updateArtworkBulk(artworkUri, ids)
        handler().showToast("${ids.size} item${if (ids.size > 1) "s" else ""} updated")
    }

    override fun relinkMedia(mediaId: Int, newUri: Uri) = handler().launchWithoutLoading {
        mediaRepository.relinkMedia(mediaId, newUri)
        handler().showToast("File re-linked successfully")
    }
}