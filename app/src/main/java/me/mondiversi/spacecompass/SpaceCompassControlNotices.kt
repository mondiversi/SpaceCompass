package me.mondiversi.spacecompass

/** Internal edits and reopening a control do not announce an unchanged activation state. */
internal fun spaceCompassActivationNotice(wasActive: Boolean, active: Boolean): Boolean? =
    active.takeIf { wasActive != active }

/** Toasts omit a closing full stop in every language; progress ellipses remain intact. */
internal fun spaceCompassBottomMessageText(text: CharSequence): String {
    val message = text.toString().trimEnd()
    return if (message.endsWith("...")) message else message.trimEnd('.', '。', '।', '۔')
}
