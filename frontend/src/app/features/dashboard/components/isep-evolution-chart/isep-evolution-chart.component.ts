import { Component, Input, OnChanges } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { IsepHistoryEntry } from '../../interfaces/dashboard.interface';
import { BAND_META } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-isep-evolution-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  template: `
    <div
      echarts
      [options]="chartOptions"
      class="evolution-chart"
    ></div>
  `,
  styles: [`
    .evolution-chart {
      width: 100%;
      height: 320px;
    }
  `],
})
export class IsepEvolutionChartComponent implements OnChanges {
  @Input({ required: true }) entries: IsepHistoryEntry[] = [];
  @Input() title = 'Evolução do ISEP';

  chartOptions: EChartsOption = {};

  ngOnChanges(): void {
    this.buildChart();
  }

  private buildChart(): void {
    const labels = this.entries.map((e) => e.iterationName ?? e.stageName ?? e.questionnaireName);
    const values = this.entries.map((e) => e.isepPercent);
    const colors = this.entries.map((e) => BAND_META[e.band]?.cssColor ?? '#6b7280');

    this.chartOptions = {
      title: { text: this.title, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: {
        trigger: 'axis',
        formatter: (params: unknown) => {
          const p = (params as { name: string; value: number }[])[0];
          return `${p.name}<br/><b>${p.value.toFixed(2)}%</b>`;
        },
      },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { rotate: labels.length > 4 ? 30 : 0 },
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}%' },
      },
      series: [
        {
          type: 'bar',
          data: values.map((v, i) => ({ value: v, itemStyle: { color: colors[i] } })),
          label: { show: true, position: 'top', formatter: '{c}%' },
        },
      ],
      grid: { containLabel: true, left: '5%', right: '5%', bottom: '10%' },
    };
  }
}
