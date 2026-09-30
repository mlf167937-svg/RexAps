package com.rexaps.rexmanager.utils

import com.rexaps.rexmanager.RexFile
import com.rexaps.rexmanager.SortMode
import com.rexaps.rexmanager.SortOrder
import com.rexaps.rexmanager.settings.RexManagerSettings

object FileSorter {
    /** Pure function; call from a background dispatcher for big folders. */
    fun filterAndSort(files: List<RexFile>, settings: RexManagerSettings): List<RexFile> {
        val visible = if (settings.showHiddenFiles) files else files.filterNot { it.isHidden }

        val nameComparator = compareBy<RexFile, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
        val base: Comparator<RexFile> = when (settings.sortMode) {
            SortMode.NAME -> nameComparator
            SortMode.SIZE -> compareBy<RexFile> { it.size }.then(nameComparator)
            SortMode.MODIFIED -> compareBy<RexFile> { it.lastModified }.then(nameComparator)
            SortMode.TYPE -> compareBy<RexFile> { it.extension ?: "" }.then(nameComparator)
        }
        val ordered: Comparator<RexFile> =
            if (settings.sortOrder == SortOrder.DESCENDING) base.reversed() else base
        val finalComparator: Comparator<RexFile> =
            if (settings.foldersFirst) compareBy<RexFile> { !it.isDirectory }.then(ordered)
            else ordered
        return visible.sortedWith(finalComparator)
    }
}
