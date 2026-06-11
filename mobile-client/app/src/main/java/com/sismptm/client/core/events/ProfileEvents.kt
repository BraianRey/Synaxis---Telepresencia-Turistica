package com.sismptm.client.core.events

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Simple event bus for profile-related events.
 */
object ProfileEvents {
    // Emitted when profile picture was updated on server and clients should refresh
    val profilePictureUpdated: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)
}
