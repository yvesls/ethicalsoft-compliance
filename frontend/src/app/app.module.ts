import { registerLocaleData } from '@angular/common'
import ptBr from '@angular/common/locales/pt'
import { CUSTOM_ELEMENTS_SCHEMA, EnvironmentInjector, NgModule } from '@angular/core'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { BrowserModule } from '@angular/platform-browser'
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'
import { NgSelectConfig, NgSelectModule } from '@ng-select/ng-select'
import { CalendarModule, DateAdapter } from 'angular-calendar'
import { adapterFactory } from 'angular-calendar/date-adapters/moment'
import * as moment from 'moment'
import { NgxSkeletonLoaderModule } from 'ngx-skeleton-loader'
import { NgxSpinnerModule } from 'ngx-spinner'
import { SharedModule } from './shared/shared.module'

registerLocaleData(ptBr)
export function momentAdapterFactory() {
  return adapterFactory(moment)
}

@NgModule({
  declarations: [
  ],
  imports: [
    FormsModule,
    BrowserModule,
    BrowserAnimationsModule,
    NgbModule,
    NgSelectModule,
    NgxSpinnerModule.forRoot({ type: 'ball-atom' }),
    SharedModule,
    NgxSkeletonLoaderModule,
    BrowserModule,
    ReactiveFormsModule,
    CalendarModule.forRoot({ provide: DateAdapter, useFactory: momentAdapterFactory }),
  ],

  providers: [
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class AppModule {
  constructor(private config: NgSelectConfig, public environmentInjector: EnvironmentInjector) {
    this.config.notFoundText = 'Item não encontrado!'
  }
}
