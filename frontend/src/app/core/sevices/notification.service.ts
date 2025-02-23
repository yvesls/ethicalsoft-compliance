import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor() {}

  private showModal(type: 'success' | 'warning' | 'error' | 'confirm', title: string, message: string, callbackConfirm?: () => void, callbackCancel?: () => void) {

    this.closeModal();

    const modal = document.createElement('div');
    modal.classList.add('modal', type);
    const iconPath = new URL(`../../../assets/icons/${type}.svg`, import.meta.url).href;

    modal.innerHTML = `
      <div class="modal-content">
        <div class="close text-end">x</div>
        <img class="modal-icon" src="${iconPath}" alt="Ícone ${title}">
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
    const iconPath2 = `/assets/icons/${type}.svg`;
    console.log('URL da Imagem:', iconPath2);
    if (type === 'confirm') {
      modal.querySelector('.btn-cancel')?.addEventListener('click', () => {
        this.closeModal();
        if (callbackCancel) callbackCancel();
      });

      modal.querySelector('.btn-confirm')?.addEventListener('click', () => {
        this.closeModal();
        if (callbackConfirm) callbackConfirm();
      });
    }

    modal.addEventListener('click', (event) => {
      if (event.target === modal) {
        this.closeModal();
      }
    });
  }

  private closeModal() {
    document.querySelectorAll('.modal').forEach(modal => modal.remove());
  }

  showSuccess(message: string) {
    this.showModal('success', 'Sucesso', message);
  }

  showWarning(message: string) {
    this.showModal('warning', 'Atenção', message);
  }

  showError(message: string) {
    this.showModal('error', 'Erro', message);
  }

  showConfirm(message: string, callbackConfirm: () => void, callbackCancel?: () => void) {
    this.showModal('confirm', 'Atenção', message, callbackConfirm, callbackCancel);
  }
}
