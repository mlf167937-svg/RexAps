package com.rexaps.rexmusic

import android.os.SystemClock
import kotlin.math.max
import org.json.JSONObject

/** Send a one-shot snapshot to a newly joined/requesting member. */
internal fun RexMusicSyncEngine.__splitImpl_sendState(p: RexMusicSyncEngine.Peer) {
    if (hostPreparing) return
    val track = hostTrack
    val now = SystemClock.elapsedRealtime()
    val player = RexPlayerController.state.value
    val roomElapsed = (now - roomStartedElapsedMs).coerceAtLeast(0L)
    if (track == null) {
        send(p.writer, JSONObject().put("type", "room_snapshot").put("trackId", "")
            .put("playing", false).put("position", 0L).put("roomElapsedMs", roomElapsed))
        return
    }

    val currentPosition = if (player.nowPlaying?.id == track.id && player.durationMs > 0L) {
        player.positionMs.coerceAtLeast(0L)
    } else if (hostPlaying) {
        playAnchorPositionMs + max(0L, now - playAnchorWallMs)
    } else {
        player.positionMs.coerceAtLeast(0L)
    }
    val repeat = RexPlayerController.state.value
    if (hostPlaying) {
        val startAt = if (playAnchorWallMs > now) playAnchorWallMs else now + START_LEAD_MS
        // A member who joins after playback has started must begin at the position
        // the host will reach at the scheduled deadline, not at the stale position
        // sampled before the five-second preparation window.
        val projectedPosition = if (playAnchorWallMs > now) {
            playAnchorPositionMs + max(0L, startAt - playAnchorWallMs)
        } else {
            currentPosition + max(0L, startAt - now)
        }
        val maxDuration = player.durationMs.takeIf { player.nowPlaying?.id == track.id && it > 0L }
        val startPosition = maxDuration?.let { projectedPosition.coerceAtMost(it) } ?: projectedPosition
        send(p.writer, JSONObject().put("type", "play_prepare").put("trackId", track.id)
            .put("title", track.title).put("position", startPosition).put("startAt", startAt)
            .put("hostElapsed", now).put("roomElapsedMs", roomElapsed).put("revision", roomPlaybackRevision)
            .put("repeatTotal", repeat.repeatTotal).put("repeatLeft", repeat.repeatLeft))
    } else {
        send(p.writer, JSONObject().put("type", "room_snapshot").put("trackId", track.id)
            .put("title", track.title).put("position", currentPosition).put("playing", false)
            .put("roomElapsedMs", roomElapsed).put("revision", roomPlaybackRevision)
            .put("repeatTotal", repeat.repeatTotal).put("repeatLeft", repeat.repeatLeft))
    }
}
