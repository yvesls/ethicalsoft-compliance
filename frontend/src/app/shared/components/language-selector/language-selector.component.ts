import { CommonModule } from '@angular/common'
import { Component, ElementRef, HostListener, NgZone, OnDestroy, OnInit, inject } from '@angular/core'
import { TranslateModule } from '@ngx-translate/core'
import { LanguageCode, LanguageService, SupportedLanguage } from '../../../core/i18n/language.service'

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './language-selector.component.html',
  styleUrls: ['./language-selector.component.scss'],
})
export class LanguageSelectorComponent implements OnInit, OnDestroy {
  private readonly languageService = inject(LanguageService)
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef)
  private readonly ngZone = inject(NgZone)

  languages: SupportedLanguage[] = []
  current: LanguageCode = 'pt-BR'
  open = false

  private outsideHandler?: (event: MouseEvent) => void

  ngOnInit(): void {
    this.languageService.supportedLanguages$.subscribe((list) => (this.languages = list))
    this.languageService.currentLanguage$.subscribe((code) => (this.current = code))

    this.ngZone.runOutsideAngular(() => {
      this.outsideHandler = (event: MouseEvent) => {
        if (!this.open) return
        const target = event.target as Node | null
        if (target && !this.elementRef.nativeElement.contains(target)) {
          this.ngZone.run(() => (this.open = false))
        }
      }
      document.addEventListener('mousedown', this.outsideHandler, true)
    })
  }

  ngOnDestroy(): void {
    if (this.outsideHandler) {
      document.removeEventListener('mousedown', this.outsideHandler, true)
      this.outsideHandler = undefined
    }
  }

  toggle(event?: MouseEvent): void {
    event?.stopPropagation()
    this.open = !this.open
  }

  select(language: LanguageCode, event?: Event): void {
    event?.stopPropagation()
    this.open = false
    if (language === this.current) return
    this.languageService.changeLanguage(language).subscribe()
  }

  shortCode(code: LanguageCode): string {
    return code.split('-')[0].toUpperCase()
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) this.open = false
  }
}
