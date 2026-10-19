import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PlatformContact, PlatformContactService } from '../../core/marketing/platform-contact.service';
import { supportDayName, supportSchedule } from '../../core/marketing/support-format';
@Component({selector:'app-support-editor', standalone:true, imports:[FormsModule],
  templateUrl:'./support-editor.component.html', styleUrl:'./support-editor.component.css', changeDetection:ChangeDetectionStrategy.OnPush})
export class SupportEditorComponent {
  private readonly api = inject(PlatformContactService);
  readonly loading = signal(true); readonly saving = signal(false);
  readonly error = signal(''); readonly message = signal('');
  readonly days = Array.from({length:7}, (_,i) => ({id:i+1, name:supportDayName(i+1)}));
  readonly schedule = supportSchedule;
  draft: PlatformContact | null = null;
  constructor() { this.load(); }
  load() {
    if(this.saving()) return;
    this.loading.set(true); this.error.set(''); this.message.set('');
    this.api.get().subscribe({next: value => {this.draft = value; this.loading.set(false);}, error: e => {
      this.loading.set(false);
      if(e.status === 404) this.draft = {version:null,correo:'',correoLegal:'',telefono:'',dias:[],horaInicio:'',horaFin:'',zonaHoraria:'',visible:false};
      else this.error.set(e.error?.mensaje || 'No fue posible cargar el contacto.');
    }});
  }
  toggleDay(id: number, checked: boolean) {
    if(this.draft) this.draft.dias = checked ? [...new Set([...this.draft.dias, id])].sort((a,b)=>a-b) : this.draft.dias.filter(d=>d!==id);
  }
  save() {
    if(!this.draft || this.saving()) return;
    this.saving.set(true); this.error.set(''); this.message.set('');
    this.api.save(this.draft).subscribe({next: value => {this.draft = value;this.saving.set(false);this.message.set('Contacto guardado. La actualización pública puede tardar hasta 60 segundos.');},error: e => {this.saving.set(false);this.error.set(e.error?.mensaje || 'No fue posible guardar el contacto.');}});
  }
}
