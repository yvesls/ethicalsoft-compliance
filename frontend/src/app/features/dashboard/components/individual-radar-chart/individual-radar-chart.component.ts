import { Component, Input, OnChanges, inject } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { TranslateService } from '@ngx-translate/core';
import type { EChartsOption } from 'echarts';

@Component({
  selector: 'app-individual-radar-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './individual-radar-chart.component.html',
  styleUrl: './individual-radar-chart.component.scss',
})
export class IndividualRadarChartComponent implements OnChanges {
  @Input({ required: true }) personalIcpPercent!: number;
  @Input() teamAveragePercent: number | null = null;

  chartOptions: EChartsOption = {};

  private readonly translate = inject(TranslateService);

  ngOnChanges(): void {
    this.buildChart();
  }

  private buildChart(): void {
    const personalLabel = this.translate.instant('dashboard.chart.personal_conformity');
    const teamLabel = this.translate.instant('dashboard.chart.team_average');
    const myIndexLabel = this.translate.instant('dashboard.chart.my_index');

    const indicators = [
      { name: personalLabel, max: 100 },
      { name: teamLabel, max: 100 },
    ];

    const myValue = this.personalIcpPercent ?? 0;
    const teamValue = this.teamAveragePercent ?? 0;

    this.chartOptions = {
      title: {
        text: this.translate.instant('dashboard.chart.radar_title'),
        subtext: this.teamAveragePercent === null ? this.translate.instant('dashboard.chart.radar_subtitle_pending') : undefined,
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {},
      legend: { bottom: 0, data: [myIndexLabel, teamLabel] },
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
