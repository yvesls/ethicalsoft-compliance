import { Component, Input, OnChanges } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { Band, BAND_META } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-band-distribution-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './band-distribution-chart.component.html',
  styleUrl: './band-distribution-chart.component.scss',
})
export class BandDistributionChartComponent implements OnChanges {
  @Input({ required: true }) distribution: Record<Band, number> = { A: 0, B: 0, C: 0, D: 0, E: 0 };

  chartOptions: EChartsOption = {};

  ngOnChanges(): void {
    this.buildChart();
  }

  private buildChart(): void {
    const bands: Band[] = ['A', 'B', 'C', 'D', 'E'];
    const data = bands.map((b) => ({
      value: this.distribution[b] ?? 0,
      itemStyle: { color: BAND_META[b].cssColor },
    }));

    this.chartOptions = {
      title: {
        text: 'Distribuição por Faixa',
        subtext: 'Nº de membros por classificação',
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: unknown) => {
          const p = (params as { name: string; value: number }[])[0];
          return `Faixa ${p.name}: <b>${p.value} membro(s)</b>`;
        },
      },
      xAxis: { type: 'category', data: bands, name: 'Faixa' },
      yAxis: { type: 'value', name: 'Membros', minInterval: 1 },
      series: [
        {
          type: 'bar',
          data,
          label: { show: true, position: 'top' },
        },
      ],
      grid: { containLabel: true, left: '5%', right: '5%' },
    };
  }
}
