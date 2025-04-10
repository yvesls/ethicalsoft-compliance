import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NavigateInfo, RouteStorageParams } from './router.service';

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  private readonly AUTH_TOKEN_KEY = 'auth_token';
  private readonly NAVIGATION_HISTORY_KEY = 'navigation_history';
  private readonly CURRENT_PAGE_KEY = 'current_page';
  private readonly SIDEBAR_STATE_KEY = 'sidebar_collapsed';
  private readonly SHOW_LAYOUT_KEY = 'show_layout';

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  setAuthToken(token: string | null): void {
    if (isPlatformBrowser(this.platformId)) {
      if (token) {
        localStorage.setItem(this.AUTH_TOKEN_KEY, token);
      } else {
        localStorage.removeItem(this.AUTH_TOKEN_KEY);
      }
    }
  }

  getAuthToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem(this.AUTH_TOKEN_KEY);
    }
    return null;
  }

  clearAuthToken(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.AUTH_TOKEN_KEY);
    }
  }

  getHistVID(): string[] {
    if (isPlatformBrowser(this.platformId)) {
      const history = localStorage.getItem(this.NAVIGATION_HISTORY_KEY);
      return history ? JSON.parse(history) : [];
    }
    return [];
  }

  remHistVID(...vids: string[]): void {
    if (isPlatformBrowser(this.platformId)) {
      let history = this.getHistVID().filter((vid) => !vids.includes(vid));
      localStorage.setItem(this.NAVIGATION_HISTORY_KEY, JSON.stringify(history));
    }
  }

  setCurrentPage<T>(currentData: NavigateInfo<T>): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.CURRENT_PAGE_KEY, JSON.stringify(currentData));
    }
  }

  getCurrentPage<T>(): NavigateInfo<T> {
    if (isPlatformBrowser(this.platformId)) {
      const data = localStorage.getItem(this.CURRENT_PAGE_KEY);
      return data ? JSON.parse(data) : { vid: '', route: '' };
    }
    return { vid: '', route: '' };
  }

  setViewPageData(vid: string, storageData: RouteStorageParams): void {
    if (isPlatformBrowser(this.platformId) && vid) {
      const key = `view_page_${vid}`;
      localStorage.setItem(key, JSON.stringify(storageData));

      const history = this.getHistVID();
      if (!history.includes(vid)) {
        history.push(vid);
        localStorage.setItem(this.NAVIGATION_HISTORY_KEY, JSON.stringify(history));
      }
    }
  }

  getViewPageData(vid: string): RouteStorageParams | null {
    if (isPlatformBrowser(this.platformId) && vid) {
      const key = `view_page_${vid}`;
      const data = localStorage.getItem(key);

      if (!data) {
        return null;
      }

      try {
        return JSON.parse(data);
      } catch (error) {
        return null;
      }
    }
    return null;
  }

  setSidebarState(collapsed: boolean): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.SIDEBAR_STATE_KEY, JSON.stringify(collapsed));
    }
  }

  getSidebarState(): boolean {
    if (isPlatformBrowser(this.platformId)) {
      return JSON.parse(localStorage.getItem(this.SIDEBAR_STATE_KEY) || 'false');
    }
    return false;
  }

  setShowLayout(show: boolean): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.SHOW_LAYOUT_KEY, JSON.stringify(show));
    }
  }

  getShowLayout(): boolean {
    if (isPlatformBrowser(this.platformId)) {
      return JSON.parse(localStorage.getItem(this.SHOW_LAYOUT_KEY) || 'true');
    }
    return true;
  }

  remove(vid: string): void {
    if (isPlatformBrowser(this.platformId) && vid) {
      const key = `view_page_${vid}`;
      localStorage.removeItem(key);
    }
  }

  clear(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.NAVIGATION_HISTORY_KEY);
      localStorage.removeItem(this.CURRENT_PAGE_KEY);
      Object.keys(localStorage).forEach((key) => {
        if (key.startsWith('view_page_')) {
          localStorage.removeItem(key);
        }
      });
    }
  }
}
