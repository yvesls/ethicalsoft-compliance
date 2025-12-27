import { Subject } from 'rxjs'

export interface ModalRef<TResult = unknown> {
	close: (result?: TResult) => void
	result: Subject<TResult>
}
