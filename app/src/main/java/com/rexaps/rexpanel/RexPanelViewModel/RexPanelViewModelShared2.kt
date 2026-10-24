package com.rexaps.rexpanel

/**
     * Panah, Home, End.
     *
     * final:
     * A = atas
     * B = bawah
     * C = kanan
     * D = kiri
     * H = home
     * F = end
     */
    fun RexPanelViewModel.__split_sendCursor_7(final: Char) {

        val mod =
            1 +
                (if (shift) 1 else 0) +
                (if (alt) 2 else 0) +
                (if (ctrl) 4 else 0)

        sendRaw(
            if (mod == 1) {
                "\u001B[$final"
            } else {
                "\u001B[1;$mod$final"
            }
        )
    }
