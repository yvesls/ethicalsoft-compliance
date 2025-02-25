import { Injectable } from '@angular/core';
import { NgxSpinnerService } from 'ngx-spinner';

@Injectable({
  providedIn: 'root'
})
export class SpinnerService {
  private requestCount = 0;

  constructor(private spinner: NgxSpinnerService) {}

  show(): void {
    if (this.requestCount === 0) {
      this.spinner.show();
    }
    this.requestCount++;
  }

  hide(): void {
    this.requestCount--;
    if (this.requestCount <= 0) {
      this.requestCount = 0;
      this.spinner.hide();
    }
  }
}
