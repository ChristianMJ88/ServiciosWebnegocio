import {Component,inject,signal} from '@angular/core';
import {FormBuilder,ReactiveFormsModule,Validators} from '@angular/forms';
import {ActivatedRoute,RouterLink} from '@angular/router';
import {finalize} from 'rxjs/operators';
import {PasswordRecoveryService} from '../../core/auth/password-recovery.service';
@Component({selector:'app-password-recovery',standalone:true,imports:[ReactiveFormsModule,RouterLink],templateUrl:'./password-recovery.component.html',styleUrl:'./password-recovery.component.css'})
export class PasswordRecoveryComponent {
  private readonly fb=inject(FormBuilder); private readonly route=inject(ActivatedRoute); private readonly service=inject(PasswordRecoveryService);
  readonly loading=signal(false); readonly done=signal(false); error=''; readonly token=this.route.snapshot.queryParamMap.get('token');
  readonly requestForm=this.fb.group({correo:['',[Validators.required,Validators.email]]});
  readonly confirmForm=this.fb.group({password:['',[Validators.required,Validators.minLength(10),Validators.maxLength(100),Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{10,100}$/)]],confirmation:['',Validators.required]});
  request(){if(this.requestForm.invalid){this.requestForm.markAllAsTouched();return;}this.loading.set(true);this.error='';this.service.request(this.requestForm.value.correo!).pipe(finalize(()=>this.loading.set(false))).subscribe({next:()=>this.done.set(true),error:()=>this.done.set(true)});}
  confirm(){if(this.confirmForm.invalid){this.confirmForm.markAllAsTouched();return;}const v=this.confirmForm.getRawValue();if(v.password!==v.confirmation){this.error='Las contraseñas no coinciden.';return;}this.loading.set(true);this.error='';this.service.confirm(this.token!,v.password!).pipe(finalize(()=>this.loading.set(false))).subscribe({next:()=>this.done.set(true),error:e=>this.error=e?.error?.message||e?.error?.mensaje||'El enlace expiró o ya fue utilizado.'});}
}
