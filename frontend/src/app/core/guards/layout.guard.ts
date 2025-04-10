import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { StorageService } from '../services/storage.service';

@Injectable({
  providedIn: 'root'
})
export class LayoutGuard implements CanActivate {
  constructor(private storageService: StorageService) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const showLayout = route.data?.['showLayout'] ?? true;
    this.storageService.setShowLayout(showLayout);
    return of(true);
  }
}
