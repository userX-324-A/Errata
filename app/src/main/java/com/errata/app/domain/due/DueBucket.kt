package com.errata.app.domain.due

enum class DueBucket {
    /** Hidden from the pending home list. */
    HIDDEN,

    OVERDUE,
    DUE_TODAY,
    SOON,

    /** Beyond soon horizon — not shown on home pending. */
    LATER,
}
