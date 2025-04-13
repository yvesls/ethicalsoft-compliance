import { Type } from '@angular/core'

export interface ModalConfig {
	component?: Type<any>
	size?: 'very-small' | 'small' | 'medium' | 'large'
	title?: string
	data?: any
	hasForm?: boolean
	showCloseButton?: boolean
	backdropClose?: boolean
	customClass?: string
}
