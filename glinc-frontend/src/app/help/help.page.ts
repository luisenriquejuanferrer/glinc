import { Component } from '@angular/core';

@Component({
  selector: 'app-help',
  templateUrl: './help.page.html',
  styleUrls: ['./help.page.scss'],
  standalone: false,
})
export class HelpPage {

  tab:
    | 'glucosa'
    | 'rango'
    | 'tir'
    | 'a1c'
    | 'tendencias'
    | 'actualizacion'
    | 'glosario' = 'glucosa';

  irATab(t: HelpPage['tab']): void {
    this.tab = t;
  }
}
