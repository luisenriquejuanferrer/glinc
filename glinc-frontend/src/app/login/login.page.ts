import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { ProblemDetails } from '../models/auth.model';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
  standalone: false,
})
export class LoginPage implements OnInit {

  email = '';
  password = '';
  verPassword = false;
  enviando = false;
  errorMsg = '';

  constructor(
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigateByUrl('/workspace');
    }
  }

  get puedeEnviar(): boolean {
    return (
      this.email.trim().length > 3 &&
      this.password.length >= 4 &&
      !this.enviando
    );
  }

  enviar(): void {
    if (!this.puedeEnviar) {
      return;
    }
    this.enviando = true;
    this.errorMsg = '';

    this.authService.login(this.email.trim(), this.password).subscribe({
      next: () => {
        this.enviando = false;
        this.router.navigateByUrl('/workspace');
      },
      error: (err: HttpErrorResponse) => {
        this.enviando = false;
        this.errorMsg = this.traducirError(err);
      },
    });
  }

  toggleVerPassword(): void {
    this.verPassword = !this.verPassword;
  }

  private traducirError(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No se puede conectar con el servidor. ¿Esta encendido el backend?';
    }
    const problema = err.error as ProblemDetails | null;
    if (problema && problema.detail) {
      return problema.detail;
    }
    return 'No se pudo iniciar sesion. Intentalo de nuevo.';
  }
}
