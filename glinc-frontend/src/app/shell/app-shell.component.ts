import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { GlucoseService } from '../services/glucose.service';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';
import { SearchService } from '../services/search.service';

@Component({
  selector: 'app-shell',
  templateUrl: './app-shell.component.html',
  styleUrls: ['./app-shell.component.scss'],
  standalone: false,
})
export class AppShellComponent implements OnInit {

  rutaActual = 'workspace';
  numPacientes = 0;
  modalInvitarAbierto = false;
  menuUsuarioAbierto = false;
  busqueda = '';

  nombreUsuario = '';
  emailUsuario = '';
  inicialesUsuario = '?';

  constructor(
    private router: Router,
    private glucoseService: GlucoseService,
    private authService: AuthService,
    private userService: UserService,
    private searchService: SearchService,
  ) {}

  ngOnInit(): void {
    this.rutaActual = this.extraerRuta(this.router.url);
    this.router.events.subscribe((evento) => {
      if (evento instanceof NavigationEnd) {
        this.rutaActual = this.extraerRuta(evento.urlAfterRedirects);
        if (this.rutaActual !== 'workspace') {
          this.busqueda = '';
          this.searchService.clear();
        }
      }
    });

    this.glucoseService.getPatients().subscribe((lista) => {
      this.numPacientes = lista.length;
    });

    this.emailUsuario = this.authService.getEmail() ?? '';
    this.nombreUsuario = this.localPartDelEmail(this.emailUsuario);
    this.inicialesUsuario = this.calcularIniciales('', '', this.emailUsuario);

    this.userService.profile$.subscribe((perfil) => {
      if (!perfil) {
        return;
      }
      const nombre = (perfil.firstName ?? '').trim();
      const apellido = (perfil.lastName ?? '').trim();
      const completo = (nombre + ' ' + apellido).trim();
      this.nombreUsuario =
        completo.length > 0
          ? completo
          : this.localPartDelEmail(perfil.email);
      this.emailUsuario = perfil.email;
      this.inicialesUsuario = this.calcularIniciales(
        nombre,
        apellido,
        perfil.email,
      );
    });

    this.userService.refresh().subscribe({
      next: () => {},
      error: () => {},
    });
  }

  private extraerRuta(url: string): string {
    const limpia = url.split('?')[0].replace(/^\//, '');
    if (limpia === '' || limpia === 'workspace') {
      return 'workspace';
    }
    return limpia;
  }

  get tituloPantalla(): string {
    if (this.rutaActual === 'settings') {
      return 'Configuración';
    }
    if (this.rutaActual === 'help') {
      return 'Ayuda';
    }
    return 'Pacientes';
  }

  abrirModalInvitar(): void {
    this.modalInvitarAbierto = true;
  }
  cerrarModalInvitar(): void {
    this.modalInvitarAbierto = false;
  }

  irA(ruta: string): void {
    this.router.navigateByUrl('/' + ruta);
  }

  onBuscar(valor: string): void {
    this.busqueda = valor;
    this.searchService.setTerm(valor);
  }

  toggleMenuUsuario(): void {
    this.menuUsuarioAbierto = !this.menuUsuarioAbierto;
  }

  irAConfiguracion(): void {
    this.menuUsuarioAbierto = false;
    this.router.navigateByUrl('/settings');
  }

  cerrarSesion(): void {
    this.menuUsuarioAbierto = false;
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

  private localPartDelEmail(email: string): string {
    if (!email) {
      return '';
    }
    const arroba = email.indexOf('@');
    return arroba > 0 ? email.substring(0, arroba) : email;
  }

  private calcularIniciales(nombre: string, apellido: string, email: string): string {
    const ini = (s: string) =>
      s && s.trim().length > 0 ? s.trim()[0].toUpperCase() : '';
    const a = ini(nombre);
    const b = ini(apellido);
    if (a || b) {
      return (a + b) || a || b;
    }
    if (email) {
      return email[0].toUpperCase();
    }
    return '?';
  }
}
