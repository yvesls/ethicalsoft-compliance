export type NotificationStatus = 'UNREAD' | 'READ' | 'DELETED'

export interface NotificationResponse {
	id: string
	title: string
	content: string
	status: NotificationStatus
	createdAt: string
}
