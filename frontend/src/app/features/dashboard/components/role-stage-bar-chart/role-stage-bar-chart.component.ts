import { Component, Input, OnChanges, inject } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { TranslateService } from '@ngx-translate/core';
import type { EChartsOption } from 'echarts';
import { RoleStageComplianceDTO } from '../../interfaces/dashboard.interface';

const ROLE_COLORS = [
  '#1565c0', '#2e7d32', '#f57f17', '#6a1b9a', '#00838f',
  '#ad1457', '#4e342e', '#37474f', '#e65100', '#263238',
];

@Component({
  selector: 'app-role-stage-bar-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './role-stage-bar-chart.component.html',
  styleUrl: './role-stage-bar-chart.component.scss',
})
export class RoleStageBarChartComponent implements OnChanges {
  @Input({ required: true }) roleStageData: RoleStageComplianceDTO[] = [];

  chartOptions: EChartsOption = {};

  private readonly translate = inject(TranslateService);

  ngOnChanges(): void {
    this.buildChart();
  }

  private buildChart(): void {
    if (!this.roleStageData.length) return;

    const stageMap = new Map<string, string>();
    this.roleStageData.forEach((r) =>
      Object.values(r.iemByStage).forEach((s) => stageMap.set(String(s.stageId), s.stageName))
    );
    const stages = Array.from(stageMap.values());

    const series = this.roleStageData.map((role, idx) => ({
      name: role.roleName,
      type: 'bar' as const,
      itemStyle: { color: ROLE_COLORS[idx % ROLE_COLORS.length] },
      data: stages.map((stageName) => {
        const entry = Object.values(role.iemByStage).find((s) => s.stageName === stageName);
        return entry ? entry.iemPercent : 0;
      }),
      label: { show: false },
    }));

    this.chartOptions = {
      title: {
        text: this.translate.instant('dashboard.chart.bar_compliance_title'),
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { bottom: 0, type: 'scroll' },
      grid: { containLabel: true, left: '5%', right: '5%', bottom: '15%' },
      xAxis: {
        type: 'category',
        data: stages,
        axisLabel: { rotate: stages.length > 4 ? 30 : 0 },
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}%' },
      },
      series,
    };
  }
}
