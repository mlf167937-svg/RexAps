package com.rexaps.rexpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal fun RexPanelViewModel.__splitImpl_sendText(text: String) {

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
