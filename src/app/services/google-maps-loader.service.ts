import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class GoogleMapsLoaderService {
  private readonly document = inject(DOCUMENT);
  private loading: Promise<void> | null = null;

  load(apiKey: string): Promise<void> {
    const scope = window as unknown as { google?: { maps?: { places?: unknown } } };
    if (scope.google?.maps?.places) return Promise.resolve();
    if (this.loading) return this.loading;
    this.loading = new Promise<void>((resolve, reject) => {
      const callback = `fluoraGoogleMapsReady_${Date.now()}`;
      const callbackScope = window as unknown as Record<string, unknown>;
      callbackScope[callback] = () => {
        delete callbackScope[callback];
        resolve();
      };
      const script = this.document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&libraries=places&loading=async&callback=${callback}`;
      script.async = true;
      script.defer = true;
      script.onerror = () => {
        delete callbackScope[callback];
        this.loading = null;
        reject(new Error('No fue posible cargar Google Maps'));
      };
      this.document.head.appendChild(script);
    });
    return this.loading;
  }
}
