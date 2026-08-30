package com.errata.app.domain.sync

/**
 * Auth and unreadable Drive JSON cancel WorkManager until Link or Sync now.
 * Write debounce and process-start must not re-arm while that error is sticky.
 */
object SyncErrorPolicy {

    fun blocksBackground(lastError: String?): Boolean =
        lastError == "auth" || lastError == "corrupt"
}
