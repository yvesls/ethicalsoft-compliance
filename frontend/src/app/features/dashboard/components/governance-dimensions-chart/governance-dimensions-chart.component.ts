import { Component, Input, OnChanges } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';

export interface GovernanceDimensions {
  ethicsScorePercent: number | null;
  processScorePercent: number | null;
  fairnessScorePercent: number | null;
  esgScorePercent: number | null;
}

@Component({
  selector: 'app-governance-dimensions-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './governance-dimensions-chart.component.html',
  styleUrl: './governance-dimensions-chart.component.scss',
})
export class GovernanceDimensionsChartComponent implements OnChanges {
  @Input({ required: true }) dimensions!: GovernanceDimensions;

  chartOptions: EChartsOption = {};

  ngOnChanges(): void {
    this.buildChart();
  }

  private buildChart(): void {
    const categories = ['Ética', 'Processo', 'Fairness', 'ESG'];
    const rawValues: (number | null)[] = [
      this.dimensions.ethicsScorePercent,
      this.dimensions.processScorePercent,
      this.dimensions.fairnessScorePercent,
      this.dimensions.esgScorePercent,
    ];
    const colors = ['#1565c0', '#2e7d32', '#6a1b9a', '#00838f'];

    const data = rawValues.map((v, i) => ({
      value: v ?? 0,
      itemStyle: { color: colors[i] },
      label: {
        show: true,
        position: 'right' as const,
        formatter: v === null ? '--' : `${v.toFixed(1)}%`,
        color: '#333',
        fontSize: 12,
        fontWeight: 'bold' as const,
      },
    }));

    this.chartOptions = {
      title: {
        text: 'Dimensões de Governança',
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: unknown) => {
          const p = (params as { name: string; value: number }[])[0];
          const raw = rawValues[categories.indexOf(p.name)];
          return `${p.name}: <b>${raw === null ? '--' : raw.toFixed(2) + '%'}</b>`;
        },
      },
      grid: { left: '5%', right: '12%', top: '15%', bottom: '5%', containLabel: true },
      xAxis: {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}%' },
      },
      yAxis: {
        type: 'category',
        data: categories,
        axisLabel: { fontSize: 12, fontWeight: 'bold' },
      },
      series: [
        {
          type: 'bar',
          data,
          barMaxWidth: 40,
          showBackground: true,
          backgroundStyle: { color: '#f0f0f0', borderRadius: 4 },
          itemStyle: { borderRadius: 4 },
        },
      ],
    };
  }
}
