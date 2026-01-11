export type NotificationStatus = 'UNREAD' | 'READ' | 'DELETED'

export interface NotificationParty {
  userId: number | null
  fullName: string | null
  roles: string[]
  email?: string | null
}

export interface NotificationResponse {
  id: string
  title: string
  content: string
  status: NotificationStatus
  createdAt: string
  updatedAt: string
  templateKey?: string | null
  sender?: NotificationParty | null
  recipient?: NotificationParty | null
}
