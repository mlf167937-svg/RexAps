package com.rexaps.rexwarp.tunnel

import com.rexaps.rexwarp.RexWarpError

sealed interface RexWarpTunnelState {
    data object Disconnected : RexWarpTunnelState
    data object Connecting : RexWarpTunnelState
    data object Connected : RexWarpTunnelState
    data object Disconnecting : RexWarpTunnelState
    data class Failed(val error: RexWarpError) : RexWarpTunnelState
}