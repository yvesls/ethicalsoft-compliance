import { Component, Input, OnChanges, inject } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import type { EChartsOption } from 'echarts';
import { IsepHistoryEntry, PersonalEvolutionEntry } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-debt-evolution-chart',
  standalone: true,
  imports: [NgxEchartsDirective, TranslateModule],
  templateUrl: './debt-evolution-chart.component.html',
  styleUrl: './debt-evolution-chart.component.scss',
})
export class DebtEvolutionChartComponent implements OnChanges {
  @Input({ required: true }) entries: (IsepHistoryEntry | PersonalEvolutionEntry)[] = [];
  @Input() title = '';

  chartOptions: EChartsOption = {};
  hasData = false;

  private readonly translate = inject(TranslateService);

  ngOnChanges(): void {
    this.buildChart();
  }

  private buildChart(): void {
    const validEntries = this.entries.filter(
      (e) => e.ethicsDebtPercent !== null || e.techDebtPercent !== null
    );
    this.hasData = validEntries.length > 0;
    if (!this.hasData) return;

    const labels = validEntries.map((e) => e.iterationName ?? e.stageName ?? e.questionnaireName);
    const ethicsData = validEntries.map((e) => e.ethicsDebtPercent);
    const techData = validEntries.map((e) => e.techDebtPercent);

    this.chartOptions = {
      title: { text: this.title, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: {
        trigger: 'axis',
        formatter: (params: unknown) => {
          const ps = params as { name: string; seriesName: string; value: number | null; color: string }[];
          let html = `<b>${ps[0]?.name}</b><br/>`;
          for (const p of ps) {
            const val = p.value == null ? '--' : `${p.value.toFixed(2)}%`;
            html += `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color};margin-right:6px"></span>${p.seriesName}: <b>${val}</b><br/>`;
          }
          return html;
        },
      },
      legend: {
        bottom: 8,
        data: [this.translate.instant('dashboard.chart.debt_ethics'), this.translate.instant('dashboard.chart.debt_tech')],
      },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { rotate: labels.length > 4 ? 30 : 0 },
        boundaryGap: false,
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}%' },
        splitLine: { lineStyle: { type: 'dashed', color: '#e0e0e0' } },
      },
      series: [
        {
          name: this.translate.instant('dashboard.chart.debt_ethics'),
          type: 'line',
          data: ethicsData,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { color: '#c62828', width: 2 },
          itemStyle: { color: '#c62828' },
          areaStyle: { color: 'rgba(198,40,40,0.08)' },
          connectNulls: false,
        },
        {
          name: this.translate.instant('dashboard.chart.debt_tech'),
          type: 'line',
          data: techData,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { color: '#f57f17', width: 2 },
          itemStyle: { color: '#f57f17' },
          areaStyle: { color: 'rgba(245,127,23,0.08)' },
          connectNulls: false,
        },
      ],
      grid: { containLabel: true, left: '5%', right: '5%', top: '15%', bottom: '15%' },
      markLine: {
        silent: true,
        data: [{ yAxis: 30 }],
      },
    };
  }
}
