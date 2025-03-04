import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { StorageService } from './storage.service';

@Injectable({
  providedIn: 'root'
})
export class LayoutStateService {
  private isSidebarCollapsedSubject: BehaviorSubject<boolean>;

  isSidebarCollapsed$;

  constructor(private storageService: StorageService) {
    const savedState = this.storageService.getSidebarState();
    this.isSidebarCollapsedSubject = new BehaviorSubject<boolean>(savedState);
    this.isSidebarCollapsed$ = this.isSidebarCollapsedSubject.asObservable();
  }

  toggleSidebar(): void {
    const newState = !this.isSidebarCollapsedSubject.value;
    this.isSidebarCollapsedSubject.next(newState);
    this.storageService.setSidebarState(newState);
  }
}
