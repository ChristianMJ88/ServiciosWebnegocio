import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'appMoney',
  standalone: true
})
export class MoneyDisplayPipe implements PipeTransform {
  transform(
    value: number | string | null | undefined,
    currency = 'MXN',
    minimumFractionDigits = 0,
    maximumFractionDigits = minimumFractionDigits
  ): string {
    const numericValue = typeof value === 'string' ? Number(value) : value;

    if (numericValue === null || numericValue === undefined || Number.isNaN(Number(numericValue))) {
      return this.fallback(currency, minimumFractionDigits);
    }

    return new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: (currency || 'MXN').toUpperCase(),
      currencyDisplay: (currency || 'MXN').toUpperCase() === 'MXN' ? 'narrowSymbol' : 'symbol',
      minimumFractionDigits,
      maximumFractionDigits
    }).format(Number(numericValue));
  }

  private fallback(currency: string, minimumFractionDigits: number): string {
    const digits = minimumFractionDigits > 0 ? `0.${'0'.repeat(minimumFractionDigits)}` : '0';
    return this.transform(Number(digits), currency, minimumFractionDigits, minimumFractionDigits);
  }
}
