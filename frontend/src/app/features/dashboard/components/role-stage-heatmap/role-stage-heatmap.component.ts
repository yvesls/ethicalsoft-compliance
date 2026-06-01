import { Component, inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import type { EChartsOption } from 'echarts';
import { RoleStageComplianceDTO } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-role-stage-heatmap',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './role-stage-heatmap.component.html',
  styleUrl: './role-stage-heatmap.component.scss',
})
export class RoleStageHeatmapComponent implements OnInit, OnChanges, OnDestroy {
  @Input({ required: true }) roleStageData: RoleStageComplianceDTO[] = [];

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
    if (!this.roleStageData.length) return;

    const roles = this.roleStageData.map((r) => r.roleName);

    const stageMap = new Map<string, string>();
    this.roleStageData.forEach((r) =>
      Object.values(r.iemByStage).forEach((s) => stageMap.set(String(s.stageId), s.stageName))
    );
    const stages = Array.from(stageMap.values());

    const data: [number, number, number][] = [];
    this.roleStageData.forEach((role, rowIdx) => {
      stages.forEach((stageName, colIdx) => {
        const stageEntry = Object.values(role.iemByStage).find(
          (s) => s.stageName === stageName
        );
        data.push([colIdx, rowIdx, stageEntry ? stageEntry.iemPercent : 0]);
      });
    });

    this.chartOptions = {
      title: {
        text: this.translate.instant('dashboard.chart.heatmap_title'),
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        formatter: (params: unknown) => {
          const p = params as { value: [number, number, number] };
          const roleName = roles[p.value[1]];
          const stageName = stages[p.value[0]];
          const iem = p.value[2];
          return `<b>${roleName}</b><br/>${stageName}: <b>${iem.toFixed(2)}%</b>`;
        },
      },
      grid: { containLabel: true, left: '15%', right: '10%', bottom: '15%' },
      xAxis: {
        type: 'category',
        data: stages,
        axisLabel: { rotate: stages.length > 4 ? 30 : 0 },
      },
      yAxis: {
        type: 'category',
        data: roles,
      },
      visualMap: {
        min: 0,
        max: 100,
        calculable: true,
        orient: 'horizontal',
        left: 'center',
        bottom: '0%',
        inRange: {
          color: ['#c62828', '#e65100', '#f57f17', '#1565c0', '#2e7d32'],
        },
        text: ['100%', '0%'],
      },
      series: [
        {
          type: 'heatmap',
          data,
          label: {
            show: true,
            formatter: (params: unknown) => {
              const v = (params as { value: [number, number, number] }).value[2];
              return `${v.toFixed(0)}%`;
            },
          },
          emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } },
        },
      ],
    };
  }
}
