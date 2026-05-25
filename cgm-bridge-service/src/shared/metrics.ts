export class MetricsService {
  private readonly counters = new Map<string, number>();
  private readonly gauges = new Map<string, number>();

  increment(metric: string, by = 1): void {
    const current = this.counters.get(metric) ?? 0;
    this.counters.set(metric, current + by);
  }

  setGauge(metric: string, value: number): void {
    this.gauges.set(metric, value);
  }

  snapshot(): { counters: Record<string, number>; gauges: Record<string, number> } {
    return {
      counters: Object.fromEntries(this.counters.entries()),
      gauges: Object.fromEntries(this.gauges.entries())
    };
  }
}
