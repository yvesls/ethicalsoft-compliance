import { Injectable } from '@angular/core'
import { Subject } from 'rxjs'
import { environment } from '../../enviroments/environments'

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize(config: GoogleIdConfig): void
          renderButton(element: HTMLElement, options: GoogleButtonOptions): void
          prompt(): void
          cancel(): void
        }
      }
    }
  }
}

interface GoogleIdConfig {
  client_id: string
  callback: (response: { credential: string }) => void
  auto_select?: boolean
}

interface GoogleButtonOptions {
  theme?: 'outline' | 'filled_blue' | 'filled_black'
  size?: 'large' | 'medium' | 'small'
  width?: number
  text?: 'signin_with' | 'signup_with' | 'continue_with'
  shape?: 'rectangular' | 'pill' | 'circle' | 'square'
  logo_alignment?: 'left' | 'center'
}

@Injectable({ providedIn: 'root' })
export class GoogleAuthService {
  private readonly credentialSubject = new Subject<string>()
  readonly credential$ = this.credentialSubject.asObservable()

  private initialized = false
  private scriptLoaded = false

  initialize(): Promise<void> {
    if (this.initialized) return Promise.resolve()
    return this.loadGisScript().then(() => {
      window.google?.accounts.id.initialize({
        client_id: environment.googleClientId,
        callback: (response) => this.credentialSubject.next(response.credential),
        auto_select: false,
      })
      this.initialized = true
    })
  }

  renderButton(element: HTMLElement): void {
    this.initialize().then(() => {
      window.google?.accounts.id.renderButton(element, {
        theme: 'outline',
        size: 'large',
        width: element.offsetWidth || 300,
        text: 'signin_with',
        shape: 'rectangular',
        logo_alignment: 'center',
      })
    })
  }

  private loadGisScript(): Promise<void> {
    if (this.scriptLoaded) return Promise.resolve()
    return new Promise((resolve, reject) => {
      if (document.getElementById('google-gis-script')) {
        this.scriptLoaded = true
        resolve()
        return
      }
      const script = document.createElement('script')
      script.id = 'google-gis-script'
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.defer = true
      script.onload = () => {
        this.scriptLoaded = true
        resolve()
      }
      script.onerror = () => reject(new Error('Failed to load Google Identity Services'))
      document.head.appendChild(script)
    })
  }
}
