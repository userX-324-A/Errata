package com.errata.app.domain.sync

import java.util.UUID

object StableIds {
    fun new(): String = UUID.randomUUID().toString()

    fun orNew(uuid: String): String = uuid.ifBlank { new() }
}
