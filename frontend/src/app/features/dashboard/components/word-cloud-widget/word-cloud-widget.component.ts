import { Component, Input, OnChanges, inject } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { TranslateService } from '@ngx-translate/core';
import { WordEntry } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-word-cloud-widget',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './word-cloud-widget.component.html',
  styleUrl: './word-cloud-widget.component.scss',
})
export class WordCloudWidgetComponent implements OnChanges {
  private readonly translate = inject(TranslateService);

  @Input({ required: true }) words: WordEntry[] = [];
  @Input() totalJustifications = 0;

  chartOptions: EChartsOption = {};

  ngOnChanges(): void {
    this.buildChart();
  }

  private buildChart(): void {
    const data = this.words.map((w) => ({ name: w.word, value: w.frequency }));

    (this.chartOptions as unknown) = {
      title: {
        text: this.translate.instant('dashboard.chart.word_cloud_title'),
        subtext: this.translate.instant('dashboard.chart.word_cloud_subtext', { count: this.totalJustifications }),
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        formatter: (params: { name: string; value: number }) =>
          this.translate.instant('dashboard.chart.word_cloud_tooltip', { name: params.name, value: params.value }),
      },
      series: [
        {
          type: 'wordCloud',
          shape: 'circle',
          left: 'center',
          top: 'center',
          width: '90%',
          height: '85%',
          sizeRange: [14, 60],
          rotationRange: [-45, 45],
          rotationStep: 15,
          gridSize: 8,
          drawOutOfBound: false,
          textStyle: {
            fontFamily: 'sans-serif',
            fontWeight: 'bold',
            color: () => {
              const colors = ['#1565c0', '#2e7d32', '#6a1b9a', '#00838f', '#c62828', '#f57f17'];
              return colors[Math.floor(Math.random() * colors.length)];
            },
          },
          emphasis: {
            focus: 'self',
            textStyle: { textShadowBlur: 10, textShadowColor: '#333' },
          },
          data,
        },
      ],
    };
  }
}
