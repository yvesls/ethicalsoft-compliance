import { Component, inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import type { EChartsOption } from 'echarts';

@Component({
  selector: 'app-individual-radar-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './individual-radar-chart.component.html',
  styleUrl: './individual-radar-chart.component.scss',
})
export class IndividualRadarChartComponent implements OnInit, OnChanges, OnDestroy {
  @Input({ required: true }) personalIcpPercent!: number;
  @Input() teamAveragePercent: number | null = null;

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
    const t = (key: string) => this.translate.instant(key);

    const indicators = [
      { name: t('dashboard.chart.personal_conformity'), max: 100 },
      { name: t('dashboard.chart.team_average'), max: 100 },
    ];

    const myValue = this.personalIcpPercent ?? 0;
    const teamValue = this.teamAveragePercent ?? 0;
    const myIndexLabel = t('dashboard.chart.my_index');
    const teamAvgLabel = t('dashboard.chart.team_average');

    this.chartOptions = {
      title: {
        text: t('dashboard.chart.radar_title'),
        subtext: this.teamAveragePercent === null ? t('dashboard.chart.radar_subtitle_pending') : undefined,
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {},
      legend: { bottom: 0, data: [myIndexLabel, teamAvgLabel] },
      radar: { indicator: indicators, radius: '65%' },
      series: [
        {
          type: 'radar',
          data: [
            {
              value: [myValue, teamValue],
              name: myIndexLabel,
              lineStyle: { color: '#1565c0' },
              areaStyle: { color: 'rgba(21,101,192,0.2)' },
            },
          ],
        },
      ],
    };
  }
}
