-- Limpieza post-TFG: se retira el DemoSeeder y todas las lecturas sinteticas.
-- A partir de aqui solo se almacenan lecturas reales del poller + backfill de LibreLink.

DELETE FROM glucose_readings WHERE source = 'SEED';

ALTER TABLE glucose_readings DROP COLUMN source;
