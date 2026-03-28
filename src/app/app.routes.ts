import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { ServicesComponent } from './pages/services/services.component';
import { BookingComponent } from './pages/booking/booking.component';
import { ContactComponent } from './pages/contact/contact.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { AccountComponent } from './pages/account/account.component';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { StaffDashboardComponent } from './pages/staff-dashboard/staff-dashboard.component';
import { AdminDashboardComponent } from './pages/admin-dashboard/admin-dashboard.component';
import { CajaDashboardComponent } from './pages/caja-dashboard/caja-dashboard.component';
import { RecepcionDashboardComponent } from './pages/recepcion-dashboard/recepcion-dashboard.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'servicios', component: ServicesComponent },
  { path: 'agendar', component: BookingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegisterComponent },
  { path: 'mi-cuenta', component: AccountComponent, canActivate: [authGuard], data: { layout: 'panel' } },
  { path: 'staff', component: StaffDashboardComponent, canActivate: [roleGuard], data: { roles: ['STAFF'], layout: 'panel' } },
  { path: 'admin', component: AdminDashboardComponent, canActivate: [roleGuard], data: { roles: ['ADMIN'], layout: 'panel' } },
  { path: 'recepcion', component: RecepcionDashboardComponent, canActivate: [roleGuard], data: { roles: ['ADMIN', 'RECEPCIONISTA'], layout: 'panel' } },
  { path: 'caja', component: CajaDashboardComponent, canActivate: [roleGuard], data: { roles: ['ADMIN', 'CAJERO', 'RECEPCIONISTA'], layout: 'panel' } },
  { path: 'contacto', component: ContactComponent },
  { path: '**', redirectTo: '' }
];
