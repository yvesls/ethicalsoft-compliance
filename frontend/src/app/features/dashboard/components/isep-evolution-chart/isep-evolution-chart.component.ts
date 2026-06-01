import { Component, inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import type { EChartsOption } from 'echarts';
import { IsepHistoryEntry, PersonalEvolutionEntry } from '../../interfaces/dashboard.interface';
import { BAND_META } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-isep-evolution-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './isep-evolution-chart.component.html',
  styleUrl: './isep-evolution-chart.component.scss',
})
export class IsepEvolutionChartComponent implements OnInit, OnChanges, OnDestroy {
  @Input({ required: true }) entries: (IsepHistoryEntry | PersonalEvolutionEntry)[] = [];
  @Input() title = '';

  private readonly translate = inject(TranslateService);
  private langSub!: Subscription;

  chartOptions: EChartsOption = {};

  ngOnInit(): void {
    this.langSub = this.translate.onLangChange.subscribe(() => this.buildChart());
  }

  ngOnChanges(): void {
    this.buildChart();
  }

  ngOnDestroy(): void {
    this.langSub?.unsubscribe();
  }

  private buildChart(): void {
    const chartTitle = this.title || this.translate.instant('dashboard.chart.isep_evolution_default');
    const labels = this.entries.map((e) => e.iterationName ?? e.stageName ?? e.questionnaireName);
    const values = this.entries.map((e) => e.isepPercent);
    const colors = this.entries.map((e) => BAND_META[e.band]?.cssColor ?? '#6b7280');

    this.chartOptions = {
      title: { text: chartTitle, left: 'center', textStyle: { fontSize: 14 } },
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
