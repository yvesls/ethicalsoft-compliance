import { Injectable } from '@angular/core'

@Injectable({
  providedIn: 'root',
})
export class FormStateService {
  private _isFormDirty = false

  setFormDirty(isDirty: boolean): void {
    this._isFormDirty = isDirty
  }

  getFormDirty(): boolean {
    return this._isFormDirty
  }
}
