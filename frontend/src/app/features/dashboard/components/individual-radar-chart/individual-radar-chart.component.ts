import { Component, Input, OnChanges } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
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

  ngOnChanges(): void {
    this.buildChart();
  }

  private buildChart(): void {
    const indicators = [
      { name: 'Conformidade Pessoal', max: 100 },
      { name: 'Média da Equipe', max: 100 },
    ];

    const myValue = this.personalIcpPercent ?? 0;
    const teamValue = this.teamAveragePercent ?? 0;

    this.chartOptions = {
      title: {
        text: 'Desempenho Relativo',
        subtext: this.teamAveragePercent === null ? 'Equipe ainda não concluiu 100%' : undefined,
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {},
      legend: { bottom: 0, data: ['Meu Índice', 'Média da Equipe'] },
      radar: { indicator: indicators, radius: '65%' },
      series: [
        {
          type: 'radar',
          data: [
            {
              value: [myValue, teamValue],
              name: 'Meu Índice',
              lineStyle: { color: '#1565c0' },
              areaStyle: { color: 'rgba(21,101,192,0.2)' },
            },
          ],
        },
      ],
    };
  }
}
