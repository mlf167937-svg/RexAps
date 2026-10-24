package com.rexaps.rexpanel

internal fun RexPanelViewModel.resizeImpl(newCols: Int,
    newRows: Int) {
    if (
        newCols == cols &&
        newRows == rows
    ) {
        return
    }

    cols = newCols
    rows = newRows

    term.resize(
        newCols,
        newRows
    )

    ssh?.resize(
        newCols,
        newRows
    )
}

/* ------------------------------- input ------------------------------ */

internal fun RexPanelViewModel.clearMods() {
    ctrl = false
    alt = false
    shift = false
}

internal fun RexPanelViewModel.sendTextImpl(text: String) {

    if (text.isEmpty()) {
        return
    }

    var s = text
    val c = s[0]

    if (ctrl) {

        val lc =
            c.lowercaseChar()

        val code =
            when {
                lc in 'a'..'z' ->
                    lc.code - 96

                c == '[' ->
                    27

                c == '\\' ->
                    28

                c == ']' ->
                    29

                c == '^' ->
                    30

                c == '_' ->
                    31

                c == ' ' ||
                    c == '@' ->
                    0

                else ->
                    -1
            }

        if (code >= 0) {
            s =
                code.toChar().toString() +
                    s.drop(1)
        }

    } else if (shift) {

        s =
            c.uppercaseChar().toString() +
                s.drop(1)
    }

    if (alt) {
        s =
            "\u001B" + s
    }

    sendRaw(s)
}

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
internal fun RexPanelViewModel.sendCursorImpl(final: Char) {

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
