package name.alexwayfer.customtv.ui

/**
 * A tap on the navigation item of the section that is already open. That section takes it as a request to go back
 * to its start, such as the top of Home's list; a tap on another item only opens that section.
 */
internal fun navigationTapReselects(open: AppSection, tapped: AppSection): Boolean = open == tapped
