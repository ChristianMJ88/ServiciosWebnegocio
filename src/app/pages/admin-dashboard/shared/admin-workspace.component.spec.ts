import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AdminWorkspaceComponent } from './admin-workspace.component';

@Component({ standalone:true, imports:[AdminWorkspaceComponent], template:`<app-admin-workspace [editando]="id()"><form class="form-card"><input value="Borrador"></form><div class="list-card">Registro existente</div></app-admin-workspace>` })
class Host { id=signal<number|null>(null); }

describe('AdminWorkspaceComponent',()=>{
  it('conserva el borrador al alternar y abre el formulario al editar un registro',async()=>{
    await TestBed.configureTestingModule({imports:[Host]}).compileComponents();
    const fixture=TestBed.createComponent(Host); fixture.detectChanges();
    const buttons=fixture.nativeElement.querySelectorAll('.workspace-views button');
    buttons[1].click(); fixture.detectChanges();
    const field=fixture.nativeElement.querySelector('input'); field.value='Borrador sin guardar';
    buttons[0].click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.list-card').parentElement.classList.contains('mobile-hidden')).toBe(false);
    fixture.componentInstance.id.set(42); fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.form-card').parentElement.classList.contains('mobile-hidden')).toBe(false);
    expect(field.value).toBe('Borrador sin guardar');
  });
});
