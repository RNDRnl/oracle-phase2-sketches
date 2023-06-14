package v05

import org.openrndr.extra.keyframer.Keyframer

class FilterSearchTimeline: Keyframer() {
    val pcscale by DoubleChannel("pc_scale")
    val pcx by DoubleChannel("pc_x")
    val pcy by DoubleChannel("pc_y")
}