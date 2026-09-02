// Generated from the HashtagCMS Workflows directive manifest — DO NOT EDIT BY HAND.
// Regenerate: ./mvnw test -Dtest=ClientModelGeneratorTest -Dcodegen.write=true

/** Every directive type the server can emit. */
export type WorkflowDirectiveType =
  | "toast"
  | "alert"
  | "snackbar"
  | "banner"
  | "haptic"
  | "play_sound"
  | "show_loader"
  | "hide_loader"
  | "progress"
  | "confetti"
  | "confirm"
  | "coachmark"
  | "set_field_error"
  | "navigate"
  | "go_back"
  | "switch_tab"
  | "scroll_to"
  | "open_url"
  | "deep_link"
  | "open_sheet"
  | "dismiss_sheet"
  | "open_modal"
  | "dismiss_modal"
  | "replace"
  | "pop_to_root"
  | "call_phone"
  | "open_email"
  | "open_map"
  | "open_settings"
  | "mutate_cart"
  | "update_badge"
  | "set_wishlist"
  | "start_checkout"
  | "clear_cart"
  | "update_quantity"
  | "notify_back_in_stock"
  | "track_order"
  | "render_photos"
  | "render_list"
  | "show_welcome"
  | "render_component"
  | "update_component"
  | "set_theme"
  | "set_locale"
  | "update_state"
  | "set_value"
  | "refresh"
  | "reload"
  | "invalidate_cache"
  | "set_auth"
  | "clear_auth"
  | "request_biometric"
  | "copy_to_clipboard"
  | "share"
  | "request_review"
  | "request_permission"
  | "schedule_notification"
  | "add_to_calendar"
  | "scan_qr"
  | "scan_barcode"
  | "download_file"
  | "get_location"
  | "track_event"
  | "open_payment_sheet"
  | "add_payment_method"
  | "show_paywall"
  | "run_workflow"
  | "delay"
  | "emit_event"
  | "set_flag"
  | "register_push"
  | "share_referral";

export interface DirectiveMeta {
  category: string;
  fallback: WorkflowDirectiveType | null;
}

export const DIRECTIVE_META: Record<WorkflowDirectiveType, DirectiveMeta> = {
  toast: { category: "feedback", fallback: null },
  alert: { category: "feedback", fallback: "toast" },
  snackbar: { category: "feedback", fallback: "toast" },
  banner: { category: "feedback", fallback: "toast" },
  haptic: { category: "feedback", fallback: null },
  play_sound: { category: "feedback", fallback: null },
  show_loader: { category: "feedback", fallback: null },
  hide_loader: { category: "feedback", fallback: null },
  progress: { category: "feedback", fallback: null },
  confetti: { category: "feedback", fallback: null },
  confirm: { category: "feedback", fallback: "alert" },
  coachmark: { category: "feedback", fallback: "toast" },
  set_field_error: { category: "feedback", fallback: "toast" },
  navigate: { category: "navigation", fallback: null },
  go_back: { category: "navigation", fallback: null },
  switch_tab: { category: "navigation", fallback: "navigate" },
  scroll_to: { category: "navigation", fallback: null },
  open_url: { category: "navigation", fallback: null },
  deep_link: { category: "navigation", fallback: "navigate" },
  open_sheet: { category: "navigation", fallback: "navigate" },
  dismiss_sheet: { category: "navigation", fallback: null },
  open_modal: { category: "navigation", fallback: "open_sheet" },
  dismiss_modal: { category: "navigation", fallback: null },
  replace: { category: "navigation", fallback: "navigate" },
  pop_to_root: { category: "navigation", fallback: "go_back" },
  call_phone: { category: "navigation", fallback: "toast" },
  open_email: { category: "navigation", fallback: "open_url" },
  open_map: { category: "navigation", fallback: "open_url" },
  open_settings: { category: "navigation", fallback: null },
  mutate_cart: { category: "cart", fallback: "toast" },
  update_badge: { category: "cart", fallback: null },
  set_wishlist: { category: "cart", fallback: "toast" },
  start_checkout: { category: "cart", fallback: "navigate" },
  clear_cart: { category: "cart", fallback: "toast" },
  update_quantity: { category: "cart", fallback: "mutate_cart" },
  notify_back_in_stock: { category: "cart", fallback: "toast" },
  track_order: { category: "cart", fallback: "navigate" },
  render_photos: { category: "content", fallback: "toast" },
  render_list: { category: "content", fallback: "toast" },
  show_welcome: { category: "content", fallback: "banner" },
  render_component: { category: "content", fallback: "render_list" },
  update_component: { category: "content", fallback: null },
  set_theme: { category: "content", fallback: null },
  set_locale: { category: "content", fallback: null },
  update_state: { category: "state", fallback: null },
  set_value: { category: "state", fallback: null },
  refresh: { category: "state", fallback: null },
  reload: { category: "state", fallback: null },
  invalidate_cache: { category: "state", fallback: null },
  set_auth: { category: "auth", fallback: null },
  clear_auth: { category: "auth", fallback: null },
  request_biometric: { category: "auth", fallback: null },
  copy_to_clipboard: { category: "device", fallback: null },
  share: { category: "device", fallback: "copy_to_clipboard" },
  request_review: { category: "device", fallback: null },
  request_permission: { category: "device", fallback: null },
  schedule_notification: { category: "device", fallback: null },
  add_to_calendar: { category: "device", fallback: null },
  scan_qr: { category: "device", fallback: null },
  scan_barcode: { category: "device", fallback: null },
  download_file: { category: "device", fallback: "open_url" },
  get_location: { category: "device", fallback: null },
  track_event: { category: "analytics", fallback: null },
  open_payment_sheet: { category: "payments", fallback: "navigate" },
  add_payment_method: { category: "payments", fallback: "navigate" },
  show_paywall: { category: "payments", fallback: "navigate" },
  run_workflow: { category: "flow", fallback: null },
  delay: { category: "flow", fallback: null },
  emit_event: { category: "flow", fallback: null },
  set_flag: { category: "flow", fallback: null },
  register_push: { category: "growth", fallback: null },
  share_referral: { category: "growth", fallback: "share" },
};
