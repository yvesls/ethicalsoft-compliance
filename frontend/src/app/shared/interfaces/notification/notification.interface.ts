export type NotificationStatus = 'UNREAD' | 'READ' | 'DELETED'

export type GovernanceEvent =
  | 'QUESTIONNAIRE_OVERDUE'
  | 'QUESTIONNAIRE_ISEP_CALCULATED'
  | 'PROJECT_OVERDUE'
  | 'PROJECT_ISEP_CALCULATED'

export interface NotificationResponse {
	id: string
	title: string
	content: string
	status: NotificationStatus
	createdAt: string
	governanceEvent: GovernanceEvent | null
}
