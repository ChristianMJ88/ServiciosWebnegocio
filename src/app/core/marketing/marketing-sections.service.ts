import { Injectable, signal } from '@angular/core';
/** Presentational navigation state. Never resolves capabilities or tenant access. */
@Injectable({providedIn:'root'})
export class MarketingSectionsService {
  private readonly current=signal('');
  readonly active=this.current.asReadonly();
  select(id:string){this.current.set(id);}
}
