package me.mondiversi.spacecompass

/** Latest-state transport: one presentation frame sends only the channels that changed. */
internal class SpaceCompassGlobeUpdates {
    private var content = ""
    private var frame = ""
    private var pose = ""
    private var sentContent = ""
    private var sentFrame = ""
    private var sentPose = ""

    fun update(content: String, frame: String, pose: String) {
        this.content = content; this.frame = frame; this.pose = pose
    }

    fun invalidate() { sentContent = ""; sentFrame = ""; sentPose = "" }

    fun drain(): String? {
        val fields = ArrayList<String>(3)
        if (content != sentContent) { fields += "\"content\":$content"; sentContent = content }
        if (frame != sentFrame) { fields += "\"frame\":$frame"; sentFrame = frame }
        if (pose != sentPose) { fields += "\"pose\":$pose"; sentPose = pose }
        return fields.takeIf { it.isNotEmpty() }?.joinToString(",", "{", "}")
    }
}
