import { CommonModule } from '@angular/common'
import { Component } from '@angular/core'
import { NgxSpinnerModule } from 'ngx-spinner'

@Component({
	selector: 'app-home',
	imports: [CommonModule, NgxSpinnerModule],
	templateUrl: './home.component.html',
	styleUrls: ['./home.component.css'],
})
export class HomeComponent {}
