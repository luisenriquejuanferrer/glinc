import { Component, EventEmitter, Output } from '@angular/core';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-invite-modal',
  templateUrl: './invite-modal.component.html',
  styleUrls: ['./invite-modal.component.scss'],
  standalone: false,
})
export class InviteModalComponent {

  @Output() cerrado = new EventEmitter<void>();

  miCorreo: string;
  copiado = false;

  constructor(authService: AuthService) {
    this.miCorreo = authService.getEmail() ?? '';
  }

  cerrar(): void {
    this.cerrado.emit();
  }

  copiarCorreo(): void {
    navigator.clipboard?.writeText(this.miCorreo).catch(() => {});
    this.copiado = true;
    setTimeout(() => (this.copiado = false), 1500);
  }
}
