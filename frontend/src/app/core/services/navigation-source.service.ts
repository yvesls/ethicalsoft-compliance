import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NavigationSourceService {
  private _isInternalNavigation = false;

  setInternalNavigation(isInternal: boolean): void {
    this._isInternalNavigation = isInternal;
  }

  isInternalNavigation(): boolean {
    const result = this._isInternalNavigation;
    this._isInternalNavigation = false;
    return result;
  }
}
