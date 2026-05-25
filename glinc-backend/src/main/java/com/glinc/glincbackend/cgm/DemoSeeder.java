package com.glinc.glincbackend.cgm;

import com.glinc.glincbackend.bridge.dto.BridgePatient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Genera lecturas sinteticas (source=SEED) para tener historico que demostrar.
// Controlado por cgm.demo.seed-enabled (debe ir a false en produccion).
@Component
public class DemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private static final int PASO_MINUTOS = 15;

    private final GlucoseReadingRepository repository;

    @Value("${cgm.demo.seed-enabled:false}")
    private boolean seedEnabled;

    @Value("${cgm.demo.seed-days:90}")
    private int seedDays;

    public DemoSeeder(GlucoseReadingRepository repository) {
        this.repository = repository;
    }

    public void seedIfNeeded(List<BridgePatient> patients) {
        if (!seedEnabled) {
            return;
        }
        if (patients == null) {
            return;
        }
        for (BridgePatient paciente : patients) {
            sembrarPaciente(paciente);
        }
    }

    private void sembrarPaciente(BridgePatient paciente) {
        boolean yaSembrado = repository.existsByPatientIdAndSource(
                paciente.getPatientId(), ReadingSource.SEED);
        if (yaSembrado) {
            log.info("DemoSeeder: el paciente {} ya tiene datos sinteticos, no se siembra",
                    paciente.getPatientId());
            return;
        }

        Random random = new Random();
        int offsetPaciente = random.nextInt(21) - 10;

        ZoneId zona = ZoneId.systemDefault();
        Instant fin = Instant.now();
        Instant inicio = fin.minus(seedDays, ChronoUnit.DAYS);

        List<GlucoseReading> lecturas = new ArrayList<>();
        int valorAnterior = -1;

        for (Instant momento = inicio;
             momento.isBefore(fin);
             momento = momento.plus(PASO_MINUTOS, ChronoUnit.MINUTES)) {

            LocalDateTime hora = LocalDateTime.ofInstant(momento, zona);
            int mgDl = valorEnInstante(hora, random, offsetPaciente);

            String trend = (valorAnterior < 0)
                    ? "flat"
                    : calcularTendencia(mgDl, valorAnterior);
            valorAnterior = mgDl;

            lecturas.add(new GlucoseReading(
                    paciente.getPatientId(),
                    paciente.getFirstName(),
                    paciente.getLastName(),
                    mgDl,
                    trend,
                    ReadingSource.SEED,
                    momento));
        }

        repository.saveAll(lecturas);
        log.info("DemoSeeder: {} lecturas sinteticas generadas para el paciente {} ({} dias)",
                lecturas.size(), paciente.getPatientId(), seedDays);
    }

    // Suma de tres componentes: base + onda diaria + pico postprandial + ruido. Recorta a [60, 260].
    private int valorEnInstante(LocalDateTime hora, Random random, int offsetPaciente) {
        int minutoDelDia = hora.getHour() * 60 + hora.getMinute();

        double base = 105 + offsetPaciente;

        double ondaDiaria = 12.0 * Math.sin(2 * Math.PI * (minutoDelDia - 360) / 1440.0);

        double picos = picoComida(minutoDelDia, 8 * 60 + 30, 70)
                + picoComida(minutoDelDia, 14 * 60, 80)
                + picoComida(minutoDelDia, 21 * 60, 65);

        double ruido = random.nextGaussian() * 6.0;

        double valor = base + ondaDiaria + picos + ruido;

        if (valor < 60) {
            valor = 60;
        }
        if (valor > 260) {
            valor = 260;
        }
        return (int) Math.round(valor);
    }

    // Curva gaussiana centrada 45 min despues de la comida.
    private double picoComida(int minutoDelDia, int minutoComida, double amplitud) {
        int minutoPico = minutoComida + 45;
        double anchura = 60.0;
        double distancia = minutoDelDia - minutoPico;
        return amplitud * Math.exp(-(distancia * distancia) / (2 * anchura * anchura));
    }

    private String calcularTendencia(int actual, int anterior) {
        int delta = actual - anterior;
        if (delta > 25) {
            return "rising_fast";
        }
        if (delta > 8) {
            return "rising";
        }
        if (delta >= -8) {
            return "flat";
        }
        if (delta >= -25) {
            return "falling";
        }
        return "falling_fast";
    }
}
