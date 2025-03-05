import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { StorageService } from './storage.service';

@Injectable({
  providedIn: 'root'
})
export class LayoutStateService {
  private showLayoutSubject = new BehaviorSubject<boolean>(true);
  showLayout$ = this.showLayoutSubject.asObservable();

  private isSidebarCollapsedSubject = new BehaviorSubject<boolean>(false);
  isSidebarCollapsed$ = this.isSidebarCollapsedSubject.asObservable();

  constructor(private storageService: StorageService) {
    this.showLayoutSubject.next(this.storageService.getShowLayout());
    this.isSidebarCollapsedSubject.next(this.storageService.getSidebarState());
  }

  setShowLayout(show: boolean): void {
    this.showLayoutSubject.next(show);
    this.storageService.setShowLayout(show);
  }

  toggleSidebar(): void {
    const newState = !this.isSidebarCollapsedSubject.value;
    this.isSidebarCollapsedSubject.next(newState);
    this.storageService.setSidebarState(newState);
  }
}
