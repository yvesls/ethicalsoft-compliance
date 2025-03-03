import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NavigateInfo, RouteStorageParams } from './router.service';

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  private authTokenKey = 'auth_token';
  private historyKey = 'navigation_history';
  private currentPageKey = 'current_page';

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  setAuthToken(token: string | null): void {
    if (isPlatformBrowser(this.platformId)) {
      if (token) {
        localStorage.setItem(this.authTokenKey, token);
      } else {
        localStorage.removeItem(this.authTokenKey);
      }
    }
  }

  getAuthToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem(this.authTokenKey);
    }
    return null;
  }

  clearAuthToken(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.authTokenKey);
    }
  }

  getHistVID(): string[] {
    if (isPlatformBrowser(this.platformId)) {
      return JSON.parse(localStorage.getItem(this.historyKey) || '[]');
    }
    return [];
  }

  remHistVID(...vids: string[]): void {
    if (isPlatformBrowser(this.platformId)) {
      let history = this.getHistVID().filter((vid) => !vids.includes(vid));
      localStorage.setItem(this.historyKey, JSON.stringify(history));
    }
  }

  setCurrentPage<T>(currentData: NavigateInfo<T>): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.currentPageKey, JSON.stringify(currentData));
    }
  }

  getCurrentPage<T>(): NavigateInfo<T> {
    if (isPlatformBrowser(this.platformId)) {
      const data = localStorage.getItem(this.currentPageKey);
      return data ? JSON.parse(data) : { vid: '', route: '' };
    }
    return { vid: '', route: '' };
  }

  setViewPageData(vid: string, storageData: RouteStorageParams): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(`view_page_${vid}`, JSON.stringify(storageData));
    }
  }

  getViewPageData(vid: string): RouteStorageParams {
    if (isPlatformBrowser(this.platformId)) {
      const data = localStorage.getItem(`view_page_${vid}`);
      return data ? JSON.parse(data) : null;
    }
    return {} as RouteStorageParams;
  }

  remove(vid: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(`view_page_${vid}`);
    }
  }

  clear(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.historyKey);
      localStorage.removeItem(this.currentPageKey);
      Object.keys(localStorage).forEach((key) => {
        if (key.startsWith('view_page_')) {
          localStorage.removeItem(key);
        }
      });
    }
  }
}
