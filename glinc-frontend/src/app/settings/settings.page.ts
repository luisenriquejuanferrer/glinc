import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';
import { UserService } from '../services/user.service';
import { PreferencesService } from '../services/preferences.service';
import { ProblemDetails } from '../models/auth.model';
import { Unidades } from '../models/preferences.model';
import { CaregiverRole } from '../models/user.model';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.page.html',
  styleUrls: ['./settings.page.scss'],
  standalone: false,
})
export class SettingsPage implements OnInit {

  tab: 'account' | 'measurement' | 'notif' | 'app' = 'account';

  email = '';
  firstName = '';
  lastName = '';
  birthDate = '';
  phone = '';
  role: CaregiverRole | null = null;

  cargandoPerfil = false;
  guardando = false;
  guardandoRol = false;
  mensaje: { tipo: 'ok' | 'error'; texto: string } | null = null;

  pacientesVinculados = 0;
  sesionExpiraEn = '';
  tokenSufijo = '';

  unidades: Unidades = 'mgdl';
  umbralBajo = 70;
  umbralAlto = 180;

  notifs = true;
  sonido = false;
  emailNotif = true;

  modoOscuro = false;

  modalApp: 'about' | 'docs' | 'support' | null = null;

  readonly appNombre = 'Glinc';
  readonly appVersion = 'v0.4.1';
  readonly appAutor = 'Luis Enrique Juan Ferrer';
  readonly appAnio = 2026;
  readonly soporteEmail = 'luisenriquejuanf@gmail.com';

  constructor(
    private router: Router,
    private authService: AuthService,
    private themeService: ThemeService,
    private userService: UserService,
    private preferencesService: PreferencesService,
  ) {
    this.modoOscuro = this.themeService.esOscuro();
    this.email = this.authService.getEmail() ?? '';
    this.tokenSufijo = this.calcularSufijoToken();

    const prefs = this.preferencesService.snapshot();
    this.unidades = prefs.unidades;
    this.umbralBajo = prefs.umbralBajo;
    this.umbralAlto = prefs.umbralAlto;
    this.notifs = prefs.notifs;
    this.sonido = prefs.sonido;
    this.emailNotif = prefs.emailNotif;
  }

  ngOnInit(): void {
    this.cargarPerfil();
    this.cargarSesion();
  }

  irATab(t: 'account' | 'measurement' | 'notif' | 'app'): void {
    this.tab = t;
  }

  cargarPerfil(): void {
    const cache = this.userService.snapshot();
    if (cache) {
      this.aplicarPerfil(cache);
    }

    this.cargandoPerfil = true;
    this.userService.refresh().subscribe({
      next: (perfil) => {
        this.aplicarPerfil(perfil);
        this.cargandoPerfil = false;
      },
      error: () => {
        this.cargandoPerfil = false;
        this.mensaje = {
          tipo: 'error',
          texto: 'No se pudo cargar tu perfil.',
        };
      },
    });
  }

  private aplicarPerfil(perfil: { firstName: string | null; lastName: string | null; birthDate: string | null; phone: string | null; role?: CaregiverRole | null }): void {
    this.firstName = perfil.firstName ?? '';
    this.lastName = perfil.lastName ?? '';
    this.birthDate = perfil.birthDate ?? '';
    this.phone = perfil.phone ?? '';
    this.role = perfil.role ?? null;
  }

  // Cambia el rol al instante (re-emite profile$ → el dashboard alterna vista cuidador/médico).
  cambiarRol(nuevo: CaregiverRole): void {
    if (this.guardandoRol || this.role === nuevo) {
      return;
    }
    this.guardandoRol = true;
    this.mensaje = null;
    this.userService.updateRole(nuevo).subscribe({
      next: (perfil) => {
        this.guardandoRol = false;
        this.role = perfil.role;
        this.mensaje = { tipo: 'ok', texto: 'Rol actualizado.' };
      },
      error: (err: HttpErrorResponse) => {
        this.guardandoRol = false;
        const problem = err.error as ProblemDetails;
        this.mensaje = {
          tipo: 'error',
          texto: problem?.detail ?? 'No se pudo cambiar el rol.',
        };
      },
    });
  }

  cargarSesion(): void {
    this.authService.me().subscribe({
      next: (me) => {
        this.pacientesVinculados = me.patients?.length ?? 0;
        this.sesionExpiraEn = this.formatearFecha(me.expiresAt);
      },
      error: () => {},
    });
  }

  guardarPerfil(): void {
    if (this.guardando) {
      return;
    }
    this.guardando = true;
    this.mensaje = null;

    this.userService
      .update({
        firstName: this.normalizar(this.firstName),
        lastName: this.normalizar(this.lastName),
        birthDate: this.normalizar(this.birthDate),
        phone: this.normalizar(this.phone),
      })
      .subscribe({
        next: (perfil) => {
          this.guardando = false;
          this.aplicarPerfil(perfil);
          this.mensaje = { tipo: 'ok', texto: 'Perfil guardado.' };
        },
        error: (err: HttpErrorResponse) => {
          this.guardando = false;
          const problem = err.error as ProblemDetails;
          this.mensaje = {
            tipo: 'error',
            texto:
              problem?.detail ?? 'No se pudo guardar el perfil.',
          };
        },
      });
  }

  iniciales(): string {
    const ini = (s: string) => (s.trim().length > 0 ? s.trim()[0].toUpperCase() : '');
    const a = ini(this.firstName);
    const b = ini(this.lastName);
    if (a || b) {
      return (a + b) || a || b;
    }
    return this.email ? this.email[0].toUpperCase() : '?';
  }

  nombreCompleto(): string {
    const completo = (this.firstName + ' ' + this.lastName).trim();
    return completo.length > 0 ? completo : 'Tu perfil';
  }

  cambiarTema(): void {
    this.modoOscuro = !this.modoOscuro;
    this.themeService.cambiar(this.modoOscuro);
  }

  abrirModalApp(tipo: 'about' | 'docs' | 'support'): void {
    this.modalApp = tipo;
  }

  cerrarModalApp(): void {
    this.modalApp = null;
  }

  cerrarSesion(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.userService.clear();
        this.router.navigateByUrl('/login');
      },
      error: () => {
        this.userService.clear();
        this.router.navigateByUrl('/login');
      },
    });
  }

  cambiarUnidades(u: Unidades): void {
    this.unidades = u;
    this.preferencesService.update({ unidades: u });
  }

  guardarUmbrales(): void {
    this.preferencesService.update({
      umbralBajo: Number(this.umbralBajo),
      umbralAlto: Number(this.umbralAlto),
    });
  }

  toggle(propiedad: 'notifs' | 'sonido' | 'emailNotif'): void {
    this[propiedad] = !this[propiedad];
    this.preferencesService.update({ [propiedad]: this[propiedad] });
  }

  private normalizar(valor: string): string | null {
    const recortado = (valor ?? '').trim();
    return recortado.length === 0 ? null : recortado;
  }

  private formatearFecha(iso: string): string {
    if (!iso) {
      return '';
    }
    const d = new Date(iso);
    return d.toLocaleString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private calcularSufijoToken(): string {
    const token = this.authService.getToken() ?? '';
    return token.length >= 4 ? token.slice(-4) : token;
  }
}
