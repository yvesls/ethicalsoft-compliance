import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LayoutStateService {
  private isSidebarCollapsedSubject = new BehaviorSubject<boolean>(false);
  isSidebarCollapsed$ = this.isSidebarCollapsedSubject.asObservable();

  toggleSidebar(): void {
    const newState = !this.isSidebarCollapsedSubject.value;
    this.isSidebarCollapsedSubject.next(newState);
  }

  setSidebarState(collapsed: boolean): void {
    this.isSidebarCollapsedSubject.next(collapsed);
  }
}
