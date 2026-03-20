import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class BookingDataService {
  private readonly SERVICES = [
    'Manicura',
    'Pedicura',
    'Uñas Acrílicas',
    'Uñas de Gel',
    'Nail Art',
    'Retiro de Gel/Acrílico',
    'Diseño Especial'
  ];

  private readonly WORKING_HOURS = [
    '09:00', '10:00', '11:00', '12:00', '13:00', '14:00',
    '15:00', '16:00', '17:00', '18:00', '19:00', '20:00'
  ];

  getServices(): string[] {
    return [...this.SERVICES];
  }

  getWorkingHours(): string[] {
    return [...this.WORKING_HOURS];
  }


}
