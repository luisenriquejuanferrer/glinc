import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { HelpPage } from './help.page';
import { HelpPageRoutingModule } from './help-routing.module';

@NgModule({
  imports: [CommonModule, HelpPageRoutingModule],
  declarations: [HelpPage],
})
export class HelpPageModule {}
