import { Injectable } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Params, Router } from '@angular/router';
import { BehaviorSubject, filter, Observable } from 'rxjs';
import { Md5 } from 'ts-md5';
import { deepCopy } from '../utils/common-utils';
import { NotificationService } from './notification.service';
import { StorageService } from './storage.service';
import { FormStateService } from './form-state.service';
import { addDays } from '../utils/date-utils';
import { LayoutStateService } from './layout-state.service';

@Injectable({
  providedIn: 'root'
})
export class RouterService {
  private params: any;
  private currentRouteSubject = new BehaviorSubject<string>('');

  currentRoute$ = this.currentRouteSubject.asObservable();

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private storageService: StorageService,
    private notificationService: NotificationService,
    private formStateService: FormStateService,
    private layoutStateService: LayoutStateService
  ) {
    this.clearOldViewPageData();
    this.currentRouteSubject.next(this.router.url);
    this.monitorRouteChanges();
  }

  private monitorRouteChanges(): void {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      let currentRoute = this.activatedRoute;
      while (currentRoute.firstChild) {
        currentRoute = currentRoute.firstChild;
      }

      const showLayout = currentRoute.snapshot.data?.['showLayout'] !== false;

      this.layoutStateService.setShowLayout(showLayout);
    });
  }
  async navigateTo<T>(url: string, navigateParams?: NavigateParams<T>): Promise<boolean> {
    const { params = {}, queryParams = {} } = navigateParams || {};
    this._createPageData<T>(url, params, queryParams);

    if (this.formStateService.getFormDirty()) {
      return new Promise((resolve) => {
        this.notificationService.showConfirm(
          'Os dados não salvos serão perdidos. Deseja continuar?',
          async () => {
            const result = await this._redirectTo(url, queryParams);
            resolve(result);
          },
          () => resolve(false)
        );
      });
    }

    return this._redirectTo(url, queryParams);
  }

  navigateToNewTab<T>(url: string, navigateParams?: NavigateParams<T>): void {
    const { queryParams = {} } = navigateParams || {};
    this._generateVID(queryParams);
    window.open(`${url}?vid=${queryParams['vid']}`, '_blank');
  }

  rawNavigate(uri: string, queryParams?: Params | null): Promise<boolean> {
    return this.router.navigate([uri], { queryParams });
  }

  rawNavigateToNewTab(url: string): void {
    window.open(url, '_blank');
  }

  getRouteInfoParams<T>(): Observable<RouteHistoryParams<T>> {
    return new Observable((observer) => {
      this.activatedRoute.queryParams.subscribe({
        next: (routeQueryParams: Params) => {
          const routerHistoryParams: RouteHistoryParams<T> = {
            vid: routeQueryParams['vid'] || '',
            route: this.router.url.split('?')[0] || '',
            params: { ...this.params },
            queryParams: deepCopy(routeQueryParams)
          };
          observer.next(routerHistoryParams);
          observer.complete();
        }
      });
    });
  }

  get currentUrl(): string {
    return this.router.url;
  }

  getFormattedRoute(): string {
    const url = this.router.url.split('?')[0];
    const segments = url.split('/').filter(segment => segment);

    if (!segments.length) return 'Home';

    return segments.map(segment => this.capitalizeWords(segment)).join(' > ');
  }

  private capitalizeWords(str: string): string {
    return str
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join('-');
  }

  backToPrevious(inverseIndex: number = 1, removeVID: boolean = true): void {
    const currentViewPage = this.getStoredCurrentPage();
    if (!this._isStoredViewPage(currentViewPage.vid)) inverseIndex = 0;
    else if (removeVID) {
      this.storageService.remHistVID(currentViewPage.vid);
      this.storageService.remove(currentViewPage.vid);
    }

    const previousViewPage = this.getStoredViewPageByHistory(inverseIndex);
    if (previousViewPage?.obj?.route) {
      this.navigateTo(previousViewPage?.obj?.route, previousViewPage.obj);
    } else {
      this.navigateTo('');
    }
  }

  browserBack(): void {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      this.navigateTo('');
    }
  }

  private _generateVID(queryParams: Params): void {
    if (!queryParams['vid']) {
      queryParams['vid'] = Md5.hashStr(Math.random().toString());
    }
  }

  private async _redirectTo(uri: string, queryParams?: Params | null): Promise<boolean> {
    return this.router.navigate([uri], { queryParams });
  }

  private _createPageData<T>(url: string, params: RouteParams<T>, queryParams: Params): void {
    this.params = params;
    this._generateVID(queryParams);
    const vid = queryParams['vid'];
    this.setStoredCurrentPage<T>({
      vid: vid,
      route: url
    });
    this.setStoredPageViewParams(vid, {
      d: new Date(),
      obj: {
        vid: vid,
        route: url,
        params: { ...this.params },
        queryParams: queryParams
      }
    });
  }

  private _isStoredViewPage(vid: string): boolean {
    return this.storageService.getHistVID()?.some((pg) => pg === vid);
  }

  private getStoredViewPageByHistory(inverseIndex: number): RouteStorageParams | null {
    const histVID = this.storageService.getHistVID();
    const vidIndex = histVID.length - 1 - inverseIndex;
    const vid = histVID[vidIndex];
    if (vid) {
      return this.getStoredPageViewParams(vid);
    }
    return null;
  }

  private getStoredCurrentPage<T>(): NavigateInfo<T> {
    return this.storageService.getCurrentPage<T>();
  }

  private setStoredCurrentPage<T>(currentData: NavigateInfo<T>): void {
    this.storageService.setCurrentPage(currentData);
  }

  private getStoredPageViewParams(vid: string): RouteStorageParams {
    return this.storageService.getViewPageData(vid);
  }

  private setStoredPageViewParams(vid: string, storageData: RouteStorageParams): void {
    this.storageService.setViewPageData(vid, storageData);
  }

  private clearOldViewPageData(): void {
    const expiredStorageDate = addDays(-7);
    const lastViewPag = this.getStoredViewPageByHistory(0);
    if (!lastViewPag || new Date(lastViewPag.d) < expiredStorageDate) {
      this.storageService.clear();
    } else {
      const histVID = this.storageService.getHistVID();
      histVID.forEach((vidKey: any) => {
        const viewPageData = this.storageService.getViewPageData(vidKey);
        if (!viewPageData || new Date(viewPageData.d) < expiredStorageDate) {
          this.storageService.remove(vidKey);
        }
      });
    }
  }
}

export interface NavigateInfo<T> {
  vid: string;
  route: string;
}

export interface NavigateParams<T> {
  params: RouteParams<T>;
  queryParams?: Params;
}

export interface RouteHistoryParams<T> {
  vid: string;
  route: string;
  params: RouteParams<T>;
  queryParams?: Params;
}

export interface RouteParams<T> {
  objCopy?: string;
  p?: T;
  [s: string]: any;
}

export interface RouteStorageParams {
  d: Date;
  obj: RouteHistoryParams<any>;
}
