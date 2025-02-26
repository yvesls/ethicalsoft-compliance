import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgxSpinnerModule } from 'ngx-spinner';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { LayoutService } from './core/sevices/layout.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule, NgxSpinnerModule, NavbarComponent, HeaderComponent, FooterComponent],
  templateUrl: './app.component.html'
})
export class AppComponent {
  title = 'frontend';
  showLayout = false;

  constructor(
    private readonly layoutService: LayoutService
  ) {}

  ngOnInit(): void {
    this.layoutService.layoutVisible$.subscribe(visible => {
      this.showLayout = visible;
    });
  }
}
