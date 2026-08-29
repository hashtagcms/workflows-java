package org.hashtagcms.workflows.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The canonical catalogue of directive types this package ships (72), consumed
 * by the seeder. Mirrors the PHP DirectiveManifest.
 */
public final class DirectiveManifest {

    private DirectiveManifest() {}

    public record DirectiveDef(String type, String label, String category, String description,
                               Map<String, String> platforms, Map<String, Object> schema, String fallback) {}

    /** platform -> min version helpers */
    private static Map<String, String> all() { return ordered("web", "1.0", "android", "1.0", "ios", "1.0"); }
    private static Map<String, String> nat() { return ordered("android", "1.0", "ios", "1.0"); }

    private static Map<String, String> ordered(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> sch(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static DirectiveDef d(String type, String label, String category, String description,
                                  Map<String, String> platforms, Map<String, Object> schema, String fallback) {
        return new DirectiveDef(type, label, category, description, platforms, schema, fallback);
    }

    public static List<DirectiveDef> core() {
        return List.of(
            // ---- Feedback ----
            d("toast", "Toast message", "feedback", "A transient message shown to the user.",
                null, sch("message", "string", "level", "enum:success,error,info,warning"), null),
            d("alert", "Alert dialog", "feedback", "A blocking alert dialog with a title and message.",
                null, sch("title", "string", "message", "string"), "toast"),
            d("snackbar", "Snackbar", "feedback", "A snackbar with an optional action button.",
                null, sch("message", "string", "actionLabel", "string?"), "toast"),
            d("banner", "Inline banner", "feedback", "A persistent inline banner.",
                null, sch("message", "string", "level", "enum:success,error,info,warning"), "toast"),
            d("haptic", "Haptic feedback", "feedback", "Physical haptic feedback on supported devices.",
                nat(), sch("intensity", "enum:success,error,warning,medium"), null),
            d("play_sound", "Play sound", "feedback", "Play a short UI sound.",
                all(), sch("sound", "string"), null),
            d("show_loader", "Show loader", "feedback", "Show a loading overlay while something runs.",
                null, sch("message", "string?"), null),
            d("hide_loader", "Hide loader", "feedback", "Dismiss the loading overlay.",
                null, sch(), null),
            d("progress", "Progress", "feedback", "Update a progress bar (0-100).",
                null, sch("value", "int", "label", "string?"), null),
            d("confetti", "Confetti", "feedback", "Play a celebratory confetti animation.",
                null, sch(), null),
            d("confirm", "Confirm dialog", "feedback", "Ask the user to confirm; can run another workflow on the answer.",
                null, sch("title", "string", "message", "string?", "confirmLabel", "string?", "cancelLabel", "string?", "onConfirm", "string?"), "alert"),
            d("coachmark", "Coach mark", "feedback", "Highlight a UI element with an onboarding hint.",
                null, sch("anchor", "string", "text", "string"), "toast"),
            d("set_field_error", "Set field error", "feedback", "Attach an inline error to a form field.",
                null, sch("field", "string", "message", "string"), "toast"),

            // ---- Navigation ----
            d("navigate", "Navigate", "navigation", "Route the client to a named destination.",
                null, sch("target", "string", "params", "object?"), null),
            d("go_back", "Go back", "navigation", "Pop the current screen / navigate back.",
                null, sch(), null),
            d("switch_tab", "Switch tab", "navigation", "Select a bottom/side navigation tab.",
                null, sch("tab", "string"), "navigate"),
            d("scroll_to", "Scroll to", "navigation", "Scroll to an anchor or element.",
                null, sch("anchor", "string"), null),
            d("open_url", "Open URL", "navigation", "Open an external URL in the browser.",
                null, sch("url", "string"), null),
            d("deep_link", "Deep link", "navigation", "Follow an in-app deep link URI.",
                nat(), sch("uri", "string"), "navigate"),
            d("open_sheet", "Open sheet", "navigation", "Present a modal bottom sheet / drawer.",
                all(), sch("sheetId", "string", "payload", "object?"), "navigate"),
            d("dismiss_sheet", "Dismiss sheet", "navigation", "Dismiss the current sheet / drawer.",
                all(), sch(), null),
            d("open_modal", "Open modal", "navigation", "Present a modal dialog.",
                null, sch("modalId", "string", "payload", "object?"), "open_sheet"),
            d("dismiss_modal", "Dismiss modal", "navigation", "Dismiss the current modal.",
                null, sch(), null),
            d("replace", "Replace screen", "navigation", "Navigate, replacing the current screen (no back).",
                null, sch("target", "string", "params", "object?"), "navigate"),
            d("pop_to_root", "Pop to root", "navigation", "Return to the root of the navigation stack.",
                null, sch(), "go_back"),
            d("call_phone", "Call phone", "navigation", "Dial a phone number.",
                nat(), sch("phone", "string"), "toast"),
            d("open_email", "Open email", "navigation", "Compose an email to an address.",
                null, sch("to", "string", "subject", "string?", "body", "string?"), "open_url"),
            d("open_map", "Open map", "navigation", "Open maps to an address or coordinates.",
                null, sch("query", "string?", "lat", "string?", "lng", "string?"), "open_url"),
            d("open_settings", "Open settings", "navigation", "Open the app/device settings screen.",
                nat(), sch("section", "string?"), null),

            // ---- Cart & commerce ----
            d("mutate_cart", "Mutate cart", "cart", "Add, remove, or apply changes to the shopping cart.",
                all(), sch("action", "string", "couponCode", "string?", "discountPercent", "int?"), "toast"),
            d("update_badge", "Update badge", "cart", "Update a numeric badge (e.g. cart count).",
                null, sch("key", "string", "count", "int"), null),
            d("set_wishlist", "Set wishlist", "cart", "Add or remove an item from the wishlist.",
                all(), sch("action", "enum:add,remove", "productId", "string"), "toast"),
            d("start_checkout", "Start checkout", "cart", "Begin the checkout flow.",
                null, sch(), "navigate"),
            d("clear_cart", "Clear cart", "cart", "Empty the shopping cart.",
                null, sch(), "toast"),
            d("update_quantity", "Update quantity", "cart", "Change the quantity of a cart line item.",
                null, sch("productId", "string", "quantity", "int"), "mutate_cart"),
            d("notify_back_in_stock", "Back-in-stock alert", "cart", "Subscribe the user to a restock alert.",
                null, sch("productId", "string"), "toast"),
            d("track_order", "Track order", "cart", "Open order tracking for an order.",
                null, sch("orderId", "string"), "navigate"),

            // ---- Content & theming ----
            d("render_photos", "Render photos", "content", "Render an image grid from a list payload.",
                all(), sch("action", "string", "items", "array"), "toast"),
            d("render_list", "Render list", "content", "Render a generic list from a data payload.",
                all(), sch("items", "array"), "toast"),
            d("show_welcome", "Show welcome banner", "content", "Display a welcome banner sourced from a target response.",
                all(), sch("message", "string"), "banner"),
            d("render_component", "Render component", "content", "Render an SDUI component tree.",
                all(), sch("component", "string", "props", "object?"), "render_list"),
            d("update_component", "Update component", "content", "Patch an already-rendered component.",
                all(), sch("id", "string", "props", "object?"), null),
            d("set_theme", "Set theme", "content", "Switch the app theme.",
                null, sch("theme", "enum:light,dark,system"), null),
            d("set_locale", "Set locale", "content", "Change the app language/locale.",
                null, sch("locale", "string"), null),

            // ---- State ----
            d("update_state", "Update state", "state", "Merge values into the client state store.",
                null, sch("path", "string", "value", "any"), null),
            d("set_value", "Set value", "state", "Set a single key in local/session storage.",
                null, sch("key", "string", "value", "any"), null),
            d("refresh", "Refresh", "state", "Refresh the current screen or a named region.",
                null, sch("region", "string?"), null),
            d("reload", "Reload", "state", "Force a full reload of the client.",
                null, sch(), null),
            d("invalidate_cache", "Invalidate cache", "state", "Invalidate a cached key on the client.",
                null, sch("key", "string"), null),

            // ---- Auth ----
            d("set_auth", "Set auth", "auth", "Store an authentication token / session.",
                null, sch("token", "string", "user", "object?"), null),
            d("clear_auth", "Clear auth", "auth", "Clear the authentication token / session.",
                null, sch(), null),
            d("request_biometric", "Request biometric", "auth", "Prompt for biometric authentication.",
                nat(), sch("reason", "string?"), null),

            // ---- Device & permissions ----
            d("copy_to_clipboard", "Copy to clipboard", "device", "Copy a value to the clipboard.",
                null, sch("value", "string"), null),
            d("share", "Share", "device", "Open the native/web share sheet.",
                null, sch("title", "string?", "text", "string?", "url", "string?"), "copy_to_clipboard"),
            d("request_review", "Request review", "device", "Prompt for an app-store review.",
                nat(), sch(), null),
            d("request_permission", "Request permission", "device", "Prompt for a device permission.",
                nat(), sch("permission", "enum:camera,location,notifications,contacts,photos"), null),
            d("schedule_notification", "Schedule notification", "device", "Schedule a local notification.",
                nat(), sch("title", "string", "body", "string?", "at", "string?"), null),
            d("add_to_calendar", "Add to calendar", "device", "Add an event to the device calendar.",
                nat(), sch("title", "string", "startsAt", "string", "endsAt", "string?", "location", "string?"), null),
            d("scan_qr", "Scan QR", "device", "Open the QR scanner.",
                nat(), sch(), null),
            d("scan_barcode", "Scan barcode", "device", "Open the barcode scanner.",
                nat(), sch(), null),
            d("download_file", "Download file", "device", "Download/save a file.",
                null, sch("url", "string", "filename", "string?"), "open_url"),
            d("get_location", "Get location", "device", "Fetch the current device location.",
                nat(), sch(), null),

            // ---- Analytics ----
            d("track_event", "Track event", "analytics", "Emit an analytics event on the client.",
                null, sch("name", "string", "properties", "object?"), null),

            // ---- Payments ----
            d("open_payment_sheet", "Open payment sheet", "payments", "Present a native/web payment sheet.",
                all(), sch("provider", "enum:apple_pay,google_pay,stripe,razorpay", "amount", "int?", "currency", "string?"), "navigate"),
            d("add_payment_method", "Add payment method", "payments", "Prompt the user to add a card/method.",
                null, sch(), "navigate"),
            d("show_paywall", "Show paywall", "payments", "Present a subscription paywall.",
                null, sch("planId", "string?"), "navigate"),

            // ---- Flow / orchestration ----
            d("run_workflow", "Run workflow", "flow", "Trigger another workflow by alias (compose workflows).",
                null, sch("workflow", "string", "payload", "object?"), null),
            d("delay", "Delay", "flow", "Wait before the next directive runs.",
                null, sch("ms", "int"), null),
            d("emit_event", "Emit event", "flow", "Fire a client-side event on the app event bus.",
                null, sch("name", "string", "data", "object?"), null),
            d("set_flag", "Set flag", "flow", "Set a feature/session flag on the client.",
                null, sch("key", "string", "value", "any"), null),

            // ---- Growth ----
            d("register_push", "Register push", "growth", "Register the device for push notifications.",
                nat(), sch(), null),
            d("share_referral", "Share referral", "growth", "Open a referral/share flow.",
                null, sch("code", "string?", "message", "string?"), "share")
        );
    }
}
