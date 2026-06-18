import { Component, ChangeDetectionStrategy } from '@angular/core'
import { RouterModule } from '@angular/router'
import { TranslateModule } from '@ngx-translate/core'

@Component({
	selector: 'app-settings',
	standalone: true,
	imports: [RouterModule, TranslateModule],
	templateUrl: './settings.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './settings.component.scss',
})
export class SettingsComponent {}
