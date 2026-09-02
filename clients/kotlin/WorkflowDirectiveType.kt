// Generated from the HashtagCMS Workflows directive manifest — DO NOT EDIT BY HAND.
// Regenerate: ./mvnw test -Dtest=ClientModelGeneratorTest -Dcodegen.write=true
package org.hashtagcms.workflows.client

/** Every directive type the server can emit, with its category and negotiation fallback. */
enum class WorkflowDirectiveType(val wire: String, val category: String, val fallback: String?) {
    TOAST("toast", "feedback", null),  // Toast message
    ALERT("alert", "feedback", "toast"),  // Alert dialog
    SNACKBAR("snackbar", "feedback", "toast"),  // Snackbar
    BANNER("banner", "feedback", "toast"),  // Inline banner
    HAPTIC("haptic", "feedback", null),  // Haptic feedback
    PLAY_SOUND("play_sound", "feedback", null),  // Play sound
    SHOW_LOADER("show_loader", "feedback", null),  // Show loader
    HIDE_LOADER("hide_loader", "feedback", null),  // Hide loader
    PROGRESS("progress", "feedback", null),  // Progress
    CONFETTI("confetti", "feedback", null),  // Confetti
    CONFIRM("confirm", "feedback", "alert"),  // Confirm dialog
    COACHMARK("coachmark", "feedback", "toast"),  // Coach mark
    SET_FIELD_ERROR("set_field_error", "feedback", "toast"),  // Set field error
    NAVIGATE("navigate", "navigation", null),  // Navigate
    GO_BACK("go_back", "navigation", null),  // Go back
    SWITCH_TAB("switch_tab", "navigation", "navigate"),  // Switch tab
    SCROLL_TO("scroll_to", "navigation", null),  // Scroll to
    OPEN_URL("open_url", "navigation", null),  // Open URL
    DEEP_LINK("deep_link", "navigation", "navigate"),  // Deep link
    OPEN_SHEET("open_sheet", "navigation", "navigate"),  // Open sheet
    DISMISS_SHEET("dismiss_sheet", "navigation", null),  // Dismiss sheet
    OPEN_MODAL("open_modal", "navigation", "open_sheet"),  // Open modal
    DISMISS_MODAL("dismiss_modal", "navigation", null),  // Dismiss modal
    REPLACE("replace", "navigation", "navigate"),  // Replace screen
    POP_TO_ROOT("pop_to_root", "navigation", "go_back"),  // Pop to root
    CALL_PHONE("call_phone", "navigation", "toast"),  // Call phone
    OPEN_EMAIL("open_email", "navigation", "open_url"),  // Open email
    OPEN_MAP("open_map", "navigation", "open_url"),  // Open map
    OPEN_SETTINGS("open_settings", "navigation", null),  // Open settings
    MUTATE_CART("mutate_cart", "cart", "toast"),  // Mutate cart
    UPDATE_BADGE("update_badge", "cart", null),  // Update badge
    SET_WISHLIST("set_wishlist", "cart", "toast"),  // Set wishlist
    START_CHECKOUT("start_checkout", "cart", "navigate"),  // Start checkout
    CLEAR_CART("clear_cart", "cart", "toast"),  // Clear cart
    UPDATE_QUANTITY("update_quantity", "cart", "mutate_cart"),  // Update quantity
    NOTIFY_BACK_IN_STOCK("notify_back_in_stock", "cart", "toast"),  // Back-in-stock alert
    TRACK_ORDER("track_order", "cart", "navigate"),  // Track order
    RENDER_PHOTOS("render_photos", "content", "toast"),  // Render photos
    RENDER_LIST("render_list", "content", "toast"),  // Render list
    SHOW_WELCOME("show_welcome", "content", "banner"),  // Show welcome banner
    RENDER_COMPONENT("render_component", "content", "render_list"),  // Render component
    UPDATE_COMPONENT("update_component", "content", null),  // Update component
    SET_THEME("set_theme", "content", null),  // Set theme
    SET_LOCALE("set_locale", "content", null),  // Set locale
    UPDATE_STATE("update_state", "state", null),  // Update state
    SET_VALUE("set_value", "state", null),  // Set value
    REFRESH("refresh", "state", null),  // Refresh
    RELOAD("reload", "state", null),  // Reload
    INVALIDATE_CACHE("invalidate_cache", "state", null),  // Invalidate cache
    SET_AUTH("set_auth", "auth", null),  // Set auth
    CLEAR_AUTH("clear_auth", "auth", null),  // Clear auth
    REQUEST_BIOMETRIC("request_biometric", "auth", null),  // Request biometric
    COPY_TO_CLIPBOARD("copy_to_clipboard", "device", null),  // Copy to clipboard
    SHARE("share", "device", "copy_to_clipboard"),  // Share
    REQUEST_REVIEW("request_review", "device", null),  // Request review
    REQUEST_PERMISSION("request_permission", "device", null),  // Request permission
    SCHEDULE_NOTIFICATION("schedule_notification", "device", null),  // Schedule notification
    ADD_TO_CALENDAR("add_to_calendar", "device", null),  // Add to calendar
    SCAN_QR("scan_qr", "device", null),  // Scan QR
    SCAN_BARCODE("scan_barcode", "device", null),  // Scan barcode
    DOWNLOAD_FILE("download_file", "device", "open_url"),  // Download file
    GET_LOCATION("get_location", "device", null),  // Get location
    TRACK_EVENT("track_event", "analytics", null),  // Track event
    OPEN_PAYMENT_SHEET("open_payment_sheet", "payments", "navigate"),  // Open payment sheet
    ADD_PAYMENT_METHOD("add_payment_method", "payments", "navigate"),  // Add payment method
    SHOW_PAYWALL("show_paywall", "payments", "navigate"),  // Show paywall
    RUN_WORKFLOW("run_workflow", "flow", null),  // Run workflow
    DELAY("delay", "flow", null),  // Delay
    EMIT_EVENT("emit_event", "flow", null),  // Emit event
    SET_FLAG("set_flag", "flow", null),  // Set flag
    REGISTER_PUSH("register_push", "growth", null),  // Register push
    SHARE_REFERRAL("share_referral", "growth", "share");  // Share referral

    companion object {
        private val byWire = entries.associateBy { it.wire }
        fun fromWire(wire: String): WorkflowDirectiveType? = byWire[wire]
    }
}
