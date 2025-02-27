import { Component, ElementRef, ViewChild } from '@angular/core';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  @ViewChild('sidebar') sidebar!: ElementRef;
  @ViewChild('overlay') overlay!: ElementRef;

  collapseSidebar(): void {
    this.sidebar.nativeElement.classList.toggle('collapsed');
  }

  closeSidebar(): void {
    this.sidebar.nativeElement.classList.remove('toggled');
  }

  toggleSubMenu(event: Event): void {
    event.preventDefault();
    const target = (event.currentTarget as HTMLElement).nextElementSibling;
    if (target) {
      target.classList.toggle('open');
    }
  }
}
