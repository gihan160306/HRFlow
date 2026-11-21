/** Báo cho Header cập nhật lại số thông báo chưa đọc ngay, không cần reload trang. */
export const NOTIFICATIONS_CHANGED = 'hrflow:notifications-changed';

export function emitNotificationsChanged() {
  window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED));
}
