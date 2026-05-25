export type Unidades = 'mgdl' | 'mmol';

export interface UserPreferences {
  unidades: Unidades;
  // Umbrales siempre en mg/dL; se convierten a mmol/L solo al pintar.
  umbralBajo: number;
  umbralAlto: number;
  notifs: boolean;
  sonido: boolean;
  emailNotif: boolean;
}

// Umbrales 70/180: rango TIR objetivo segun consensos clinicos.
export const PREFERENCES_DEFAULT: UserPreferences = {
  unidades: 'mgdl',
  umbralBajo: 70,
  umbralAlto: 180,
  notifs: true,
  sonido: false,
  emailNotif: true,
};
