package com.rexaps.rexpanel

fun RexPanelViewModel.__split_resize_5(
        newCols: Int,
        newRows: Int
    ) {
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
