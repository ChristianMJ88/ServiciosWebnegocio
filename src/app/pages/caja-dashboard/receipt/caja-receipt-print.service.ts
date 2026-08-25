import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CajaReceiptPrintService {
  private readonly document = inject(DOCUMENT);

  async printAfterRender(): Promise<void> {
    const body = this.document.body;
    const windowRef = this.document.defaultView;
    if (!windowRef) {
      throw new Error('La impresión no está disponible en este entorno.');
    }

    await new Promise<void>(resolve => windowRef.requestAnimationFrame(() => resolve()));

    const limpiar = () => body.classList.remove('printing-caja-receipt');
    body.classList.add('printing-caja-receipt');
    windowRef.addEventListener('afterprint', limpiar, { once: true });
    windowRef.print();
    windowRef.setTimeout(limpiar, 1000);
  }
}
