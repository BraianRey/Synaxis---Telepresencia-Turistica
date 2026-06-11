package com.sismptm.partner.core.events

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Simple event bus for profile-related events.
 */
object ProfileEvents {
    val profilePictureUpdated: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)
}
