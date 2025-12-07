import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { RoleSummary } from '../../shared/interfaces/role/role-summary.interface';
import { ProjectStore } from '../../shared/stores/project.store';

@Injectable({ providedIn: 'root' })
export class RoleService {
  private cachedRoles$?: Observable<RoleSummary[]>;
  private projectStore = inject(ProjectStore);

  getRoles(forceRefresh = false): Observable<RoleSummary[]> {
    if (!this.cachedRoles$ || forceRefresh) {
      this.cachedRoles$ = this.projectStore.listRoles().pipe(shareReplay(1));
    }

    return this.cachedRoles$;
  }

  clearCache(): void {
    this.cachedRoles$ = undefined;
  }
}
