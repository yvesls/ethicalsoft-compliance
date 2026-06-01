import { Component, inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import type { EChartsOption } from 'echarts';
import { Band, BAND_META } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-band-distribution-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './band-distribution-chart.component.html',
  styleUrl: './band-distribution-chart.component.scss',
})
export class BandDistributionChartComponent implements OnInit, OnChanges, OnDestroy {
  @Input({ required: true }) distribution: Record<Band, number> = { A: 0, B: 0, C: 0, D: 0, E: 0 };

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
    const bands: Band[] = ['A', 'B', 'C', 'D', 'E'];
    const data = bands.map((b) => ({
      value: this.distribution[b] ?? 0,
      itemStyle: { color: BAND_META[b].cssColor },
    }));

    const t = (key: string) => this.translate.instant(key);

    this.chartOptions = {
      title: {
        text: t('dashboard.chart.band_distribution_title'),
        subtext: t('dashboard.chart.band_distribution_subtitle'),
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: unknown) => {
          const p = (params as { name: string; value: number }[])[0];
          return `${t('dashboard.chart.band_x_axis')} ${p.name}: <b>${p.value} ${t('dashboard.chart.member_count')}</b>`;
        },
      },
      xAxis: { type: 'category', data: bands, name: t('dashboard.chart.band_x_axis') },
      yAxis: { type: 'value', name: t('dashboard.chart.members_y_axis'), minInterval: 1 },
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
