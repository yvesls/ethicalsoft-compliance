import { Component, ChangeDetectionStrategy, Input, HostBinding, HostListener, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Project } from '../../interfaces/project/project.interface';
import { ProjectStatus } from '../../enums/project-status.enum';
import { ProjectType } from '../../enums/project-type.enum';
import { Router } from '@angular/router';

@Component({
  selector: 'app-list-item',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './list-item.component.html',
  styleUrls: ['./list-item.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListItemComponent {
  @Input({ required: true }) item!: Project;
  @Input() navigateTo: string[] | null = null;
  private readonly router = inject(Router);

  public ProjectType = ProjectType;

  @HostBinding('class.clickable') get clickable() {
    return !!this.navigateTo;
  }

  @HostBinding('attr.tabindex') get tabIndex(): number {
    return this.navigateTo ? 0 : -1;
  }

  @HostBinding('attr.role') get role(): string | null {
    return this.navigateTo ? 'button' : null;
  }

  statusIconMap: Record<string, string> = {
    'ABERTO': 'assets/icons/list-check.svg',
    'RASCUNHO': 'assets/icons/clock.svg',
    'CONCLUIDO': 'assets/icons/circle-check.svg',
    'ARQUIVADO': 'assets/icons/box-archive.svg',
  };

  statusDisplayMap: Record<string, string> = {
    'ABERTO': ProjectStatus.Aberto,
    'RASCUNHO': ProjectStatus.Rascunho,
    'CONCLUIDO': ProjectStatus.Concluido,
    'ARQUIVADO': ProjectStatus.Arquivado,
  };

  get formattedCode(): string {
    if (this.item && this.item.id !== undefined) {
      const paddedCode = String(this.item.id).padStart(3, '0');
      return `Cod ${paddedCode}`;
    }
    return '';
  }

  get formattedSituation(): string {
    if (!this.item) return '---';

    if (this.item.currentSituation) {
      return this.item.currentSituation;
    }

    if (this.item.type === ProjectType.Iterativo) {
      if (this.item.currentIteration && this.item.iterationCount) {
        return `Sprint ${this.item.currentIteration}/${this.item.iterationCount}`;
      }
      return this.item.currentStage || '---';
    }

    if (this.item.type === ProjectType.Cascata) {
      if (this.item.currentStage) {
        return this.item.currentStage;
      }
    }

    return '---';
  }

  @HostListener('click', ['$event'])
  onClick(event: Event): void {
    this.navigateFromEvent(event);
  }

  @HostListener('keydown.enter', ['$event'])
  @HostListener('keydown.space', ['$event'])
  onKeydown(event: Event): void {
    this.navigateFromEvent(event);
  }

  private navigateFromEvent(event: Event): void {
    if (!this.navigateTo) {
      return;
    }

    event.preventDefault();
    this.router.navigate(this.navigateTo);
  }
}
