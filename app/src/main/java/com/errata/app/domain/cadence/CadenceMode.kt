package com.errata.app.domain.cadence

/**
 * How [nextDue] is computed after a completion.
 * Default for new installs / settings: [FROM_COMPLETION_CATCH_UP].
 */
enum class CadenceMode {
    FROM_COMPLETION,
    FIXED_ANCHOR,
    FROM_COMPLETION_CATCH_UP,
}
