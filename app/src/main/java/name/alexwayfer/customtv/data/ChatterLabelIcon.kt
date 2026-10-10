package name.alexwayfer.customtv.data

/** A Chatter Labels label; [assignedAtMillis] is when it was given to the chatter, if this is an assignment. */
class ChatterLabelIcon(
    val id: String,
    val name: String,
    val image: ByteArray,
    val assignedAtMillis: Long? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatterLabelIcon) return false
        return id == other.id && name == other.name && image.contentEquals(other.image) &&
            assignedAtMillis == other.assignedAtMillis
    }

    override fun hashCode(): Int =
        ((31 * id.hashCode() + name.hashCode()) * 31 + image.contentHashCode()) * 31 + assignedAtMillis.hashCode()
}
