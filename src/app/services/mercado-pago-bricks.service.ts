import { Injectable } from '@angular/core';
import { CardBrickData } from './public-checkout.service';

interface BrickController { unmount(): void; }
interface MercadoPagoWindow extends Window {
  MercadoPago?: new (publicKey: string, options: { locale: string }) => {
    bricks(): { create(type: string, target: string, settings: unknown): Promise<BrickController> };
  };
}

@Injectable({ providedIn: 'root' })
export class MercadoPagoBricksService {
  private loader: Promise<void> | null = null;

  async mountCard(target: string, publicKey: string, amount: number, email: string,
                  onSubmit: (data: CardBrickData) => Promise<void>,
                  onReady: () => void, onError: (message: string) => void): Promise<BrickController> {
    await this.load();
    const Constructor = (window as MercadoPagoWindow).MercadoPago;
    if (!Constructor) throw new Error('No fue posible iniciar el formulario seguro de Mercado Pago.');
    const builder = new Constructor(publicKey, { locale: 'es-MX' }).bricks();
    return builder.create('cardPayment', target, {
      initialization: { amount, payer: { email } },
      customization: { visual: { style: { theme: 'default' } } },
      callbacks: {
        onReady,
        onSubmit,
        onError: () => onError('Mercado Pago no pudo validar el formulario. Revisa los datos e intenta de nuevo.')
      }
    });
  }

  private load(): Promise<void> {
    if (this.loader) return this.loader;
    this.loader = new Promise((resolve, reject) => {
      if ((window as MercadoPagoWindow).MercadoPago) { resolve(); return; }
      const existing = document.querySelector<HTMLScriptElement>('script[data-fluora-mercado-pago]');
      if (existing) {
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(new Error('No fue posible cargar Mercado Pago.')), { once: true });
        return;
      }
      const script = document.createElement('script');
      script.src = 'https://sdk.mercadopago.com/js/v2';
      script.async = true;
      script.dataset['fluoraMercadoPago'] = 'true';
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('No fue posible cargar Mercado Pago.'));
      document.head.appendChild(script);
    });
    return this.loader;
  }
}
