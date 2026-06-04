import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexFill,
  ApexGrid,
  ApexMarkers,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  ApexAnnotations,
  ChartComponent,
} from 'ng-apexcharts';
import { GlucosePoint, PatientReading, Trend } from '../models/glucose.model';
import { GlucoseService } from '../services/glucose.service';
import { PreferencesService } from '../services/preferences.service';
import { SearchService } from '../services/search.service';
import { InventoryService } from '../services/inventory.service';
import { AppointmentService } from '../services/appointment.service';
import { UserService } from '../services/user.service';
import { Unidades } from '../models/preferences.model';
import {
  InventoryItem,
  InventoryItemType,
  InventoryStatus,
  inventoryLabel,
} from '../models/inventory.model';
import { Appointment, SaveAppointmentRequest } from '../models/appointment.model';
import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetails } from '../models/auth.model';

export type ChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  stroke: ApexStroke;
  fill: ApexFill;
  colors: string[];
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  grid: ApexGrid;
  dataLabels: ApexDataLabels;
  markers: ApexMarkers;
  tooltip: ApexTooltip;
  annotations: ApexAnnotations;
};

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: false,
})
export class HomePage implements OnInit, OnDestroy {

  @ViewChild('chart') chartRef?: ChartComponent;

  private yMinActual = 40;
  private yMaxActual = 280;
  private yTicksActual = 6;

  private readonly intervaloRefrescoMs = 90_000;
  private refrescoHandle: ReturnType<typeof setInterval> | null = null;

  pacientes: PatientReading[] = [];
  filtro: 'all' | 'danger' | 'warn' = 'all';
  busqueda = '';

  seleccionadoId = '';
  historico: GlucosePoint[] = [];

  // 2160 h = 3 meses, alineado con el rango clinico de la HbA1c.
  readonly periodos: { label: string; horas: number }[] = [
    { label: '12h', horas: 12 },
    { label: '1d', horas: 24 },
    { label: '7d', horas: 168 },
    { label: '14d', horas: 336 },
    { label: '30d', horas: 720 },
    { label: '3m', horas: 2160 },
  ];
  horasSeleccionadas = 12;

  umbralBajo = 70;
  umbralAlto = 180;
  unidades: Unidades = 'mgdl';

  promedio = 0;
  minimo = 0;
  maximo = 0;
  tirPct = 0;
  tirBajoPct = 0;
  tirOkPct = 0;
  tirAltoPct = 0;

  // --- Vista medico (rol DOCTOR) ---
  // El rol llega del perfil; si es DOCTOR se ocultan inventario/citas y se
  // muestran las graficas clinicas (variabilidad, TIR-5, perfil diario, heatmap).
  esMedico = false;

  // Variabilidad glucemica (umbrales clinicos en mg/dL, independientes de la unidad mostrada).
  desviacion = 0;       // desviacion estandar
  coefVariacion = 0;    // CV% = DE / promedio * 100
  gmi = 0;              // Glucose Management Indicator = 3.31 + 0.02392 * promedio_mgdl

  // TIR de 5 zonas (porcentajes): muy bajo <54, bajo 54-70, rango 70-180, alto 180-250, muy alto >250.
  tir5MuyBajo = 0;
  tir5Bajo = 0;
  tir5Rango = 0;
  tir5Alto = 0;
  tir5MuyAlto = 0;

  // Perfil diario: glucosa media por hora del dia (0-23) agregando todos los dias.
  perfilDiarioChart: any = this.crearOpcionesPerfilDiario();
  // Mapa de calor dia x hora: filas=ultimos dias, columnas=horas, color=glucosa media.
  heatmapChart: any = this.crearOpcionesHeatmap();

  private readonly colorMarca = '#2b86b3';

  chartOptions: ChartOptions = {
    series: [],
    chart: {
      type: 'area',
      height: 280,
      fontFamily: 'var(--font-sans)',
      toolbar: { show: false },
      zoom: { enabled: false },
    },
    stroke: { curve: 'smooth', width: 2.5 },
    fill: {
      type: 'gradient',
      gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 100] },
    },
    colors: [this.colorMarca],
    xaxis: {
      type: 'datetime',
      // datetimeUTC=false: los datos llegan en UTC y se pintan en zona local del navegador.
      labels: { datetimeUTC: false, style: { colors: '#9aa4b2', fontSize: '11px' } },
      axisBorder: { show: false },
      axisTicks: { show: false },
      tooltip: { enabled: false },
    },
    yaxis: {
      min: 40,
      max: 280,
      tickAmount: 6,
      labels: {
        style: { colors: '#9aa4b2', fontSize: '11px' },
        formatter: (valor: number) => `${Math.round(valor)}`,
      },
    },
    grid: {
      borderColor: '#eef0f3',
      strokeDashArray: 4,
      xaxis: { lines: { show: false } },
      padding: { top: 0, right: 8, bottom: 0, left: 8 },
    },
    dataLabels: { enabled: false },
    markers: { size: 0, hover: { size: 5 } },
    tooltip: {
      theme: 'light',
      x: { format: 'dd MMM HH:mm' },
      y: { formatter: (valor: number) => `${valor} mg/dL` },
    },
    annotations: {
      yaxis: [
        {
          y: this.umbralBajo,
          y2: this.umbralAlto,
          fillColor: '#3fae6e',
          opacity: 0.1,
          borderColor: 'transparent',
        },
      ],
    },
  };

  constructor(
    private glucoseService: GlucoseService,
    private preferencesService: PreferencesService,
    private searchService: SearchService,
    private inventoryService: InventoryService,
    private appointmentService: AppointmentService,
    private userService: UserService,
  ) {}

  ngOnInit(): void {
    this.glucoseService.getPatients().subscribe((lista) => {
      this.pacientes = lista;
      if (lista.length > 0) {
        this.seleccionarPaciente(lista[0].patientId);
      }
    });

    this.preferencesService.preferences$.subscribe((prefs) => {
      this.umbralBajo = prefs.umbralBajo;
      this.umbralAlto = prefs.umbralAlto;
      this.unidades = prefs.unidades;
      if (this.historico.length > 0) {
        this.recalcularEstadisticas();
        this.recalcularGrafica();
        if (this.esMedico) {
          this.recalcularMedico();
        }
      }
    });

    this.searchService.term$.subscribe((texto) => {
      this.busqueda = texto;
    });

    // El rol vive en el perfil compartido (UserService). Si cambia en Settings,
    // el dashboard alterna entre vista cuidador y vista medico en vivo.
    this.userService.profile$.subscribe((perfil) => {
      const eraMedico = this.esMedico;
      this.esMedico = perfil?.role === 'DOCTOR';
      if (this.esMedico !== eraMedico && this.seleccionadoId) {
        // Al pasar a cuidador hay que cargar inventario/citas que antes se omitieron.
        this.seleccionarPaciente(this.seleccionadoId);
      } else if (this.esMedico && this.historico.length > 0) {
        this.recalcularMedico();
      }
    });

    this.refrescoHandle = setInterval(() => {
      this.refrescarDatos();
    }, this.intervaloRefrescoMs);
  }

  ngOnDestroy(): void {
    if (this.refrescoHandle !== null) {
      clearInterval(this.refrescoHandle);
      this.refrescoHandle = null;
    }
  }

  private refrescarDatos(): void {
    this.glucoseService.getPatients().subscribe({
      next: (lista) => (this.pacientes = lista),
      error: () => {},
    });
    if (this.seleccionadoId) {
      this.cargarHistorico(true);
    }
  }

  // Datos internos en mg/dL; conversion a mmol/L solo al pintar (factor 18).
  mostrarLectura(mgDl: number): string {
    if (this.unidades === 'mmol') {
      return (mgDl / 18).toFixed(1);
    }
    return String(Math.round(mgDl));
  }

  unidadLabel(): string {
    return this.unidades === 'mmol' ? 'mmol/L' : 'mg/dL';
  }

  seleccionarPaciente(id: string): void {
    const existe = this.pacientes.find((p) => p.patientId === id);
    if (!existe) {
      return;
    }
    this.seleccionadoId = id;
    this.cargarHistorico();
    // El medico no gestiona inventario ni citas (el backend devolveria 403), no se piden.
    if (!this.esMedico) {
      this.cargarInventario();
      this.cargarCitas();
    }
  }

  cambiarPeriodo(horas: number): void {
    this.horasSeleccionadas = horas;
    this.cargarHistorico();
  }

  cambiarFiltro(f: 'all' | 'danger' | 'warn'): void {
    this.filtro = f;
  }

  get pacientesVisibles(): PatientReading[] {
    const query = this.busqueda.trim().toLowerCase();
    return this.pacientes.filter((p) => {
      const pasaEstado =
        this.filtro === 'all' || this.estadoDe(p.mgDl) === this.filtro;
      if (!pasaEstado) {
        return false;
      }
      if (query.length === 0) {
        return true;
      }
      const nombre = (p.firstName ?? '').toLowerCase();
      const apellido = (p.lastName ?? '').toLowerCase();
      return nombre.includes(query) || apellido.includes(query);
    });
  }

  get pacienteSeleccionado(): PatientReading | null {
    return (
      this.pacientes.find((p) => p.patientId === this.seleccionadoId) || null
    );
  }

  get numAlertas(): number {
    return this.pacientes.filter(
      (p) => p.mgDl < this.umbralBajo || p.mgDl > this.umbralAlto,
    ).length;
  }

  // Formula ADAG: A1c (%) = (promedio_mgdl + 46.7) / 28.7.
  get glicosilada(): string {
    if (!this.promedio || this.promedio <= 0) {
      return '—';
    }
    const a1c = (this.promedio + 46.7) / 28.7;
    return a1c.toFixed(1);
  }

  citas: Appointment[] = [];

  modalCitaAbierto = false;
  citaEditandoId: number | null = null;
  citaFecha = '';
  citaProfesional = '';
  citaMotivo = '';
  citaError = '';
  guardandoCita = false;

  inventario: InventoryItem[] = [];

  readonly estadosInventario: { value: InventoryStatus; label: string }[] = [
    { value: 'OK',     label: 'OK' },
    { value: 'WARN',   label: 'Bajo' },
    { value: 'DANGER', label: 'Reponer' },
  ];

  get periodoLabel(): string {
    const p = this.periodos.find((x) => x.horas === this.horasSeleccionadas);
    return p ? p.label : '';
  }

  estadoDe(mgDl: number): 'ok' | 'warn' | 'danger' {
    if (mgDl < this.umbralBajo) {
      return 'warn';
    }
    if (mgDl > this.umbralAlto) {
      return 'danger';
    }
    return 'ok';
  }

  etiquetaEstado(mgDl: number): string {
    const e = this.estadoDe(mgDl);
    if (e === 'warn') {
      return 'Bajo';
    }
    if (e === 'danger') {
      return 'Alto';
    }
    return 'En rango';
  }

  flechaTendencia(t: Trend): string {
    if (t === 'rising_fast') return '↑↑';
    if (t === 'rising') return '↑';
    if (t === 'flat') return '→';
    if (t === 'falling') return '↓';
    if (t === 'falling_fast') return '↓↓';
    return '?';
  }

  textoTendencia(t: Trend): string {
    if (t === 'rising_fast') return 'Subiendo rápido';
    if (t === 'rising') return 'Subiendo';
    if (t === 'flat') return 'Estable';
    if (t === 'falling') return 'Bajando';
    if (t === 'falling_fast') return 'Bajando rápido';
    return 'Sin datos';
  }

  iniciales(p: PatientReading): string {
    const a = p.firstName?.[0] ?? '?';
    const b = p.lastName?.[0] ?? '?';
    return (a + b).toUpperCase();
  }

  hace(readAt: string): string {
    const ms = Date.now() - new Date(readAt).getTime();
    const min = Math.max(0, Math.round(ms / 60000));
    if (min < 1) {
      return 'Ahora';
    }
    if (min === 1) {
      return 'Hace 1 min';
    }
    return `Hace ${min} min`;
  }

  // suave=true en el polling de 90s: actualiza datos en sitio para evitar el rebuild que destruye la banda verde.
  private cargarHistorico(suave: boolean = false): void {
    this.glucoseService
      .getHistory(this.seleccionadoId, this.horasSeleccionadas)
      .subscribe((puntos) => {
        this.historico = puntos;
        this.recalcularEstadisticas();
        if (suave) {
          this.actualizarGraficaSuave();
        } else {
          this.recalcularGrafica();
        }
        if (this.esMedico) {
          this.recalcularMedico();
        }
      });
  }

  // No mutar chartOptions aqui: cambiar la referencia del binding dispara rebuild completo en ng-apexcharts.
  // Solo API imperativa con animate=false / redrawPaths=false.
  private actualizarGraficaSuave(): void {
    if (!this.chartRef) {
      this.recalcularGrafica();
      return;
    }

    const factor = this.unidades === 'mmol' ? 1 / 18 : 1;
    let datos: { x: number; y: number }[];
    if (this.horasSeleccionadas <= 24) {
      datos = this.historico.map((p) => ({
        x: new Date(p.readAt).getTime(),
        y: this.unidades === 'mmol'
          ? Number((p.mgDl / 18).toFixed(1))
          : p.mgDl,
      }));
    } else {
      datos = this.agruparPorHora(this.historico, factor);
    }

    this.chartRef.updateSeries([{ name: 'Glucosa', data: datos }], false);

    const limitesNuevos = this.calcularLimitesY();
    if (
      limitesNuevos.min !== this.yMinActual ||
      limitesNuevos.max !== this.yMaxActual ||
      limitesNuevos.ticks !== this.yTicksActual
    ) {
      this.yMinActual = limitesNuevos.min;
      this.yMaxActual = limitesNuevos.max;
      this.yTicksActual = limitesNuevos.ticks;
      this.chartRef.updateOptions(
        {
          yaxis: {
            min: limitesNuevos.min,
            max: limitesNuevos.max,
            tickAmount: limitesNuevos.ticks,
          },
        },
        false,
        false,
        false,
      );
    }

    const ahora = Date.now();
    const desde = ahora - this.horasSeleccionadas * 60 * 60 * 1000;
    this.chartRef.updateOptions(
      { xaxis: { min: desde, max: ahora } },
      false,
      false,
      false,
    );
  }

  private cargarInventario(): void {
    this.inventario = [];
    this.inventoryService.list(this.seleccionadoId).subscribe({
      next: (items) => (this.inventario = items),
      error: () => (this.inventario = []),
    });
  }

  etiquetaInventario(t: InventoryItemType): string {
    return inventoryLabel(t);
  }

  guardarCantidad(item: InventoryItem, nuevaCantidad: string): void {
    const normalizada = (nuevaCantidad ?? '').trim();
    const previa = (item.quantity ?? '').trim();
    if (normalizada === previa) {
      return;
    }
    this.inventoryService
      .update(this.seleccionadoId, item.type, {
        quantity: normalizada.length === 0 ? null : normalizada,
        status: item.status,
      })
      .subscribe({
        next: (actualizado) => this.fusionarItem(actualizado),
        error: () => {},
      });
  }

  cambiarEstado(item: InventoryItem, nuevo: InventoryStatus): void {
    if (item.status === nuevo) {
      return;
    }
    item.status = nuevo;
    this.inventoryService
      .update(this.seleccionadoId, item.type, {
        quantity: item.quantity,
        status: nuevo,
      })
      .subscribe({
        next: (actualizado) => this.fusionarItem(actualizado),
        error: () => {},
      });
  }

  private fusionarItem(actualizado: InventoryItem): void {
    const i = this.inventario.findIndex((x) => x.type === actualizado.type);
    if (i >= 0) {
      this.inventario[i] = actualizado;
    }
  }

  claseEstado(s: InventoryStatus): string {
    return s.toLowerCase();
  }

  private recalcularEstadisticas(): void {
    if (this.historico.length === 0) {
      this.promedio = this.minimo = this.maximo = 0;
      this.tirPct = this.tirBajoPct = this.tirOkPct = this.tirAltoPct = 0;
      return;
    }
    let min = Infinity;
    let max = -Infinity;
    let suma = 0;
    let bajos = 0;
    let enRango = 0;
    let altos = 0;
    for (const p of this.historico) {
      if (p.mgDl < min) min = p.mgDl;
      if (p.mgDl > max) max = p.mgDl;
      suma += p.mgDl;
      if (p.mgDl < this.umbralBajo) {
        bajos++;
      } else if (p.mgDl > this.umbralAlto) {
        altos++;
      } else {
        enRango++;
      }
    }
    const n = this.historico.length;
    this.minimo = min;
    this.maximo = max;
    this.promedio = Math.round(suma / n);
    this.tirOkPct = Math.round((enRango / n) * 100);
    this.tirBajoPct = Math.round((bajos / n) * 100);
    this.tirAltoPct = Math.round((altos / n) * 100);
    this.tirPct = this.tirOkPct;
  }

  // Ventanas cortas (<=24h): un punto por lectura. Largas: media por hora para no saturar la curva.
  private recalcularGrafica(): void {
    const factor = this.unidades === 'mmol' ? 1 / 18 : 1;

    let datos: { x: number; y: number }[];
    if (this.horasSeleccionadas <= 24) {
      datos = this.historico.map((p) => ({
        x: new Date(p.readAt).getTime(),
        y: this.unidades === 'mmol'
          ? Number((p.mgDl / 18).toFixed(1))
          : p.mgDl,
      }));
    } else {
      datos = this.agruparPorHora(this.historico, factor);
    }
    this.chartOptions.series = [{ name: 'Glucosa', data: datos }];

    const ahora = Date.now();
    const desde = ahora - this.horasSeleccionadas * 60 * 60 * 1000;
    this.chartOptions.xaxis = {
      ...this.chartOptions.xaxis,
      min: desde,
      max: ahora,
    };

    const limites = this.calcularLimitesY();
    this.yMinActual = limites.min;
    this.yMaxActual = limites.max;
    this.yTicksActual = limites.ticks;
    const unidadTxt = this.unidadLabel();
    this.chartOptions.yaxis = {
      ...this.chartOptions.yaxis,
      min: limites.min,
      max: limites.max,
      tickAmount: limites.ticks,
      labels: {
        style: { colors: '#9aa4b2', fontSize: '11px' },
        formatter: (valor: number) =>
          this.unidades === 'mmol'
            ? valor.toFixed(1)
            : `${Math.round(valor)}`,
      },
    };

    this.chartOptions.annotations = {
      yaxis: [
        {
          y: this.umbralBajo * factor,
          y2: this.umbralAlto * factor,
          fillColor: '#3fae6e',
          opacity: 0.1,
          borderColor: 'transparent',
        },
      ],
    };

    this.chartOptions.tooltip = {
      ...this.chartOptions.tooltip,
      y: { formatter: (valor: number) => `${valor} ${unidadTxt}` },
    };
  }

  private agruparPorHora(
    puntos: GlucosePoint[],
    factor: number,
  ): { x: number; y: number }[] {
    const grupos = new Map<number, { suma: number; cuenta: number }>();

    for (const p of puntos) {
      const ms = new Date(p.readAt).getTime();
      const horaMs = Math.floor(ms / 3600000) * 3600000;
      const g = grupos.get(horaMs);
      if (g) {
        g.suma += p.mgDl;
        g.cuenta += 1;
      } else {
        grupos.set(horaMs, { suma: p.mgDl, cuenta: 1 });
      }
    }

    const resultado: { x: number; y: number }[] = [];
    grupos.forEach((g, horaMs) => {
      const media = g.suma / g.cuenta;
      const valor =
        factor === 1
          ? Math.round(media)
          : Number((media * factor).toFixed(1));
      resultado.push({ x: horaMs, y: valor });
    });
    resultado.sort((a, b) => a.x - b.x);
    return resultado;
  }

  // Snapea min/max al multiplo del paso clinico (40 mg/dL o 2 mmol/L) para que los ticks salgan en numeros redondos.
  private calcularLimitesY(): { min: number; max: number; ticks: number } {
    let minMgdl = 40;
    let maxMgdl = 280;
    if (this.historico.length > 0) {
      for (const p of this.historico) {
        if (p.mgDl < minMgdl) minMgdl = p.mgDl;
        if (p.mgDl > maxMgdl) maxMgdl = p.mgDl;
      }
    }
    if (this.unidades === 'mmol') {
      const PASO = 2;
      const minMmol = Math.max(0, Math.floor((minMgdl / 18) / PASO) * PASO);
      const maxMmol = Math.ceil((maxMgdl / 18) / PASO) * PASO;
      return {
        min: minMmol,
        max: maxMmol,
        ticks: Math.max(2, (maxMmol - minMmol) / PASO),
      };
    }
    const PASO = 40;
    const minRound = Math.max(0, Math.floor(minMgdl / PASO) * PASO);
    const maxRound = Math.ceil(maxMgdl / PASO) * PASO;
    return {
      min: minRound,
      max: maxRound,
      ticks: Math.max(2, (maxRound - minRound) / PASO),
    };
  }

  private cargarCitas(): void {
    this.citas = [];
    this.appointmentService.list(this.seleccionadoId).subscribe({
      next: (lista) => (this.citas = lista),
      error: () => (this.citas = []),
    });
  }

  get citasProximas(): Appointment[] {
    const ahora = Date.now();
    return this.citas
      .filter((c) => new Date(c.appointmentAt).getTime() >= ahora)
      .slice()
      .reverse();
  }

  get citasPasadas(): Appointment[] {
    const ahora = Date.now();
    return this.citas.filter(
      (c) => new Date(c.appointmentAt).getTime() < ahora,
    );
  }

  formatFechaCita(iso: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  formatFechaDia(iso: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    }) + ',';
  }

  formatFechaHora(iso: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  abrirModalCrearCita(): void {
    this.citaEditandoId = null;
    this.citaFecha = this.fechaActualParaInput();
    this.citaProfesional = '';
    this.citaMotivo = '';
    this.citaError = '';
    this.modalCitaAbierto = true;
  }

  abrirModalEditarCita(c: Appointment): void {
    this.citaEditandoId = c.id;
    this.citaFecha = this.isoAInputLocal(c.appointmentAt);
    this.citaProfesional = c.professional ?? '';
    this.citaMotivo = c.reason ?? '';
    this.citaError = '';
    this.modalCitaAbierto = true;
  }

  cerrarModalCita(): void {
    this.modalCitaAbierto = false;
  }

  guardarCita(): void {
    if (this.guardandoCita) {
      return;
    }
    this.citaError = '';

    if (!this.citaFecha) {
      this.citaError = 'La fecha es obligatoria.';
      return;
    }
    if (!this.citaProfesional.trim()) {
      this.citaError = 'El profesional es obligatorio.';
      return;
    }

    const body: SaveAppointmentRequest = {
      appointmentAt: new Date(this.citaFecha).toISOString(),
      professional: this.citaProfesional.trim(),
      reason: this.citaMotivo.trim() === '' ? null : this.citaMotivo.trim(),
    };

    this.guardandoCita = true;
    const op$ = this.citaEditandoId == null
      ? this.appointmentService.create(this.seleccionadoId, body)
      : this.appointmentService.update(this.seleccionadoId, this.citaEditandoId, body);

    op$.subscribe({
      next: () => {
        this.guardandoCita = false;
        this.modalCitaAbierto = false;
        this.cargarCitas();
      },
      error: (err: HttpErrorResponse) => {
        this.guardandoCita = false;
        const p = err.error as ProblemDetails;
        this.citaError = p?.detail ?? 'No se pudo guardar la cita.';
      },
    });
  }

  eliminarCita(c: Appointment): void {
    const ok = confirm(
      `¿Eliminar la cita del ${this.formatFechaCita(c.appointmentAt)}?`,
    );
    if (!ok) {
      return;
    }
    this.appointmentService.remove(this.seleccionadoId, c.id).subscribe({
      next: () => this.cargarCitas(),
      error: () => {},
    });
  }

  private fechaActualParaInput(): string {
    return this.dateAInputLocal(new Date());
  }

  private isoAInputLocal(iso: string): string {
    return this.dateAInputLocal(new Date(iso));
  }

  private dateAInputLocal(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return d.getFullYear()
      + '-' + pad(d.getMonth() + 1)
      + '-' + pad(d.getDate())
      + 'T' + pad(d.getHours())
      + ':' + pad(d.getMinutes());
  }

  // ============ Vista medico (rol DOCTOR) ============

  private recalcularMedico(): void {
    this.recalcularVariabilidad();
    this.recalcularTir5();
    this.construirPerfilDiario();
    this.construirHeatmap();
  }

  // DE y CV% sobre mg/dL (la unidad del sensor); GMI usa el promedio en mg/dL.
  private recalcularVariabilidad(): void {
    const n = this.historico.length;
    if (n === 0) {
      this.desviacion = this.coefVariacion = this.gmi = 0;
      return;
    }
    const media = this.promedio;
    let sumaCuadrados = 0;
    for (const p of this.historico) {
      const d = p.mgDl - media;
      sumaCuadrados += d * d;
    }
    const de = Math.sqrt(sumaCuadrados / n);
    this.desviacion = de;
    this.coefVariacion = media > 0 ? (de / media) * 100 : 0;
    this.gmi = 3.31 + 0.02392 * media;
  }

  // Reparte las lecturas en 5 zonas clinicas (umbrales fijos en mg/dL).
  private recalcularTir5(): void {
    const n = this.historico.length;
    if (n === 0) {
      this.tir5MuyBajo = this.tir5Bajo = this.tir5Rango = this.tir5Alto = this.tir5MuyAlto = 0;
      return;
    }
    let mb = 0, b = 0, r = 0, a = 0, ma = 0;
    for (const p of this.historico) {
      const v = p.mgDl;
      if (v < 54) mb++;
      else if (v < 70) b++;
      else if (v <= 180) r++;
      else if (v <= 250) a++;
      else ma++;
    }
    this.tir5MuyBajo = Math.round((mb / n) * 100);
    this.tir5Bajo = Math.round((b / n) * 100);
    this.tir5Rango = Math.round((r / n) * 100);
    this.tir5Alto = Math.round((a / n) * 100);
    this.tir5MuyAlto = Math.round((ma / n) * 100);
  }

  // Glucosa media por hora del dia (0-23) agregando todos los dias del periodo.
  private construirPerfilDiario(): void {
    const factor = this.unidades === 'mmol' ? 1 / 18 : 1;
    const suma = new Array(24).fill(0);
    const cuenta = new Array(24).fill(0);
    for (const p of this.historico) {
      const h = new Date(p.readAt).getHours();
      suma[h] += p.mgDl;
      cuenta[h] += 1;
    }
    const data: (number | null)[] = [];
    for (let h = 0; h < 24; h++) {
      if (cuenta[h] === 0) {
        data.push(null);
      } else {
        const media = suma[h] / cuenta[h];
        data.push(factor === 1 ? Math.round(media) : Number((media * factor).toFixed(1)));
      }
    }
    this.perfilDiarioChart = {
      ...this.perfilDiarioChart,
      series: [{ name: 'Glucosa media', data }],
      // Banda verde de rango normal, igual que la gráfica principal (umbrales en la unidad mostrada).
      annotations: {
        yaxis: [
          {
            y: this.umbralBajo * factor,
            y2: this.umbralAlto * factor,
            fillColor: '#3fae6e',
            opacity: 0.1,
            borderColor: 'transparent',
          },
        ],
      },
    };
  }

  // Mapa de calor: filas = ultimos 14 dias, columnas = horas, color = glucosa media (mg/dL).
  private construirHeatmap(): void {
    const dias = new Map<string, { suma: number[]; cuenta: number[] }>();
    for (const p of this.historico) {
      const d = new Date(p.readAt);
      const clave = d.getFullYear() + '-' + this.pad2(d.getMonth() + 1) + '-' + this.pad2(d.getDate());
      let fila = dias.get(clave);
      if (!fila) {
        fila = { suma: new Array(24).fill(0), cuenta: new Array(24).fill(0) };
        dias.set(clave, fila);
      }
      const h = d.getHours();
      fila.suma[h] += p.mgDl;
      fila.cuenta[h] += 1;
    }
    // Orden ascendente + ultimos 14: ApexCharts pinta la serie[0] abajo, asi el dia mas reciente queda arriba.
    const claves = Array.from(dias.keys()).sort().slice(-14);
    const series = claves.map((clave) => {
      const fila = dias.get(clave)!;
      const data: { x: string; y: number | null }[] = [];
      for (let h = 0; h < 24; h++) {
        const y = fila.cuenta[h] === 0 ? null : Math.round(fila.suma[h] / fila.cuenta[h]);
        data.push({ x: this.pad2(h) + 'h', y });
      }
      return { name: this.etiquetaDiaCorta(clave), data };
    });
    this.heatmapChart = { ...this.heatmapChart, series };
  }

  private pad2(n: number): string {
    return n.toString().padStart(2, '0');
  }

  private etiquetaDiaCorta(clave: string): string {
    const partes = clave.split('-');
    return partes[2] + '/' + partes[1];
  }

  private crearOpcionesPerfilDiario(): any {
    const horas: string[] = [];
    for (let h = 0; h < 24; h++) {
      horas.push(this.pad2(h));
    }
    return {
      series: [],
      chart: {
        type: 'area',
        height: 220,
        fontFamily: 'var(--font-sans)',
        toolbar: { show: false },
        zoom: { enabled: false },
      },
      stroke: { curve: 'smooth', width: 2.5 },
      fill: {
        type: 'gradient',
        gradient: { shadeIntensity: 1, opacityFrom: 0.35, opacityTo: 0.05, stops: [0, 100] },
      },
      colors: ['#2b86b3'],
      dataLabels: { enabled: false },
      markers: { size: 0 },
      xaxis: {
        categories: horas,
        labels: { style: { colors: '#9aa4b2', fontSize: '11px' } },
        axisBorder: { show: false },
        axisTicks: { show: false },
        tooltip: { enabled: false },
      },
      yaxis: {
        labels: {
          style: { colors: '#9aa4b2', fontSize: '11px' },
          formatter: (valor: number) => `${Math.round(valor)}`,
        },
      },
      grid: { borderColor: '#eef0f3', strokeDashArray: 4 },
      annotations: { yaxis: [] },
      tooltip: { theme: 'light' },
    };
  }

  private crearOpcionesHeatmap(): any {
    return {
      series: [],
      chart: {
        type: 'heatmap',
        height: 320,
        fontFamily: 'var(--font-sans)',
        toolbar: { show: false },
      },
      dataLabels: { enabled: false },
      colors: ['#2b86b3'],
      xaxis: {
        type: 'category',
        labels: { style: { colors: '#9aa4b2', fontSize: '10px' } },
        axisBorder: { show: false },
        axisTicks: { show: false },
      },
      yaxis: { labels: { style: { colors: '#9aa4b2', fontSize: '11px' } } },
      grid: { padding: { right: 8 } },
      plotOptions: {
        heatmap: {
          radius: 2,
          enableShades: false,
          colorScale: {
            ranges: [
              { from: 0, to: 53, color: '#d98a2b', name: 'Muy bajo' },
              { from: 54, to: 69, color: '#e6b800', name: 'Bajo' },
              { from: 70, to: 180, color: '#3fae6e', name: 'En rango' },
              { from: 181, to: 250, color: '#e2574c', name: 'Alto' },
              { from: 251, to: 600, color: '#b03a30', name: 'Muy alto' },
            ],
          },
        },
      },
      tooltip: { theme: 'light' },
    };
  }
}
