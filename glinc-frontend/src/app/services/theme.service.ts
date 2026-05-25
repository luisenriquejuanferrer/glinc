import { Injectable } from '@angular/core';

// El atributo data-theme="dark" en <html> dispara el bloque oscuro de theme/variables.scss.
@Injectable({ providedIn: 'root' })
export class ThemeService {

  private readonly STORAGE_KEY = 'glinc.theme';

  inicializar(): void {
    this.aplicar(this.esOscuro());
  }

  esOscuro(): boolean {
    return localStorage.getItem(this.STORAGE_KEY) === 'dark';
  }

  cambiar(oscuro: boolean): void {
    localStorage.setItem(this.STORAGE_KEY, oscuro ? 'dark' : 'light');
    this.aplicar(oscuro);
  }

  private aplicar(oscuro: boolean): void {
    const html = document.documentElement;
    if (oscuro) {
      html.setAttribute('data-theme', 'dark');
    } else {
      html.removeAttribute('data-theme');
    }
  }
}
