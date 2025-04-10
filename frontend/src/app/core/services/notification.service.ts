import { Injectable } from '@angular/core';
import { getErrorMessage } from '../../shared/enums/error-messages.enum';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor() {}

  showWarning(error: any) {
    if(typeof error === 'string') {
      this.showModal('warning', 'Atenção', error);
    } else {
      this.showModal('warning', 'Atenção', this.formatErrorMessage(error));
    }
  }

  showError(error: any) {
    this.showModal('error', 'Erro', this.formatErrorMessage(error));
  }

  showSuccess(message: string) {
    this.showModal('success', 'Sucesso', message);
  }

  showConfirm(message: string, callbackConfirm: () => void, callbackCancel?: () => void) {
    this.showModal('confirm', 'Atenção', message, callbackConfirm, callbackCancel);
  }

  private formatErrorMessage(error: any): string {
    const errorMessage = error.message?.trim();
    return `**Erro ${error.status}** - ${error.errorType}: ${errorMessage || getErrorMessage(error.status)}`;
  }

  private showModal(
    type: 'success' | 'warning' | 'error' | 'confirm',
    title: string,
    message: string,
    callbackConfirm?: () => void,
    callbackCancel?: () => void
  ) {
    this.closeModal();

    const modal = document.createElement('div');
    modal.classList.add('modal', type);
    modal.innerHTML = `
      <div class="modal-content" @modalAnimation>
        <div class="close text-end">x</div>
        <img class="modal-icon" src="assets/icons/${type}.svg" alt="Ícone ${title}">
        <h2 class="modal-title">${title}</h2>
        <p class="modal-message">${message}</p>
        ${type === 'confirm' ? `
          <div class="modal-buttons">
            <button class="btn-cancel">Cancelar</button>
            <button class="btn-confirm">Confirmar</button>
          </div>` : ''}
      </div>
    `;

    document.body.appendChild(modal);

    modal.querySelector('.close')?.addEventListener('click', () => this.closeModal());

    if (type === 'confirm') {
      modal.querySelector('.btn-cancel')?.addEventListener('click', () => {
        this.closeModal();
        callbackCancel?.();
      });

      modal.querySelector('.btn-confirm')?.addEventListener('click', () => {
        this.closeModal();
        callbackConfirm?.();
      });
    }

    modal.addEventListener('click', (event) => {
      if (event.target === modal) {
        this.closeModal();
      }
    });
  }

  private closeModal() {
    document.querySelectorAll('.modal').forEach(modal => {
      modal.classList.add('closing');
      setTimeout(() => modal.remove(), 50);
    });
  }
}
