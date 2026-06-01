import { Component, Input, OnChanges } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { WordEntry } from '../../interfaces/dashboard.interface';

const DOMAIN_LABELS: Record<string, string> = {
  ETHICS:   'Ética',
  PROCESS:  'Processo',
  QUALITY:  'Qualidade',
  SECURITY: 'Segurança',
  ESG:      'ESG',
  FAIRNESS: 'Fairness',
};

const DOMAIN_COLORS: Record<string, string[]> = {
  ETHICS:   ['#1565c0', '#0d47a1', '#1976d2', '#42a5f5', '#1e88e5'],
  PROCESS:  ['#2e7d32', '#1b5e20', '#388e3c', '#66bb6a', '#43a047'],
  QUALITY:  ['#6a1b9a', '#4a148c', '#7b1fa2', '#ab47bc', '#8e24aa'],
  SECURITY: ['#c62828', '#b71c1c', '#d32f2f', '#ef5350', '#e53935'],
  ESG:      ['#00838f', '#006064', '#0097a7', '#26c6da', '#00acc1'],
  FAIRNESS: ['#e65100', '#bf360c', '#f4511e', '#ff7043', '#ff5722'],
};

const DEFAULT_COLORS = ['#1565c0', '#2e7d32', '#6a1b9a', '#00838f', '#c62828', '#f57f17'];

@Component({
  selector: 'app-category-word-cloud-widget',
  standalone: true,
  imports: [NgxEchartsDirective, TranslateModule],
  templateUrl: './category-word-cloud-widget.component.html',
  styleUrl: './category-word-cloud-widget.component.scss',
})
export class CategoryWordCloudWidgetComponent implements OnChanges {
  @Input({ required: true }) topWords: WordEntry[] = [];
  @Input() totalJustifications = 0;
  @Input() categoryWordFrequency: Record<string, Record<string, number>> | null = {};

  activeTab = 'GERAL';
  chartOptions: EChartsOption | null = null;

  get categoryKeys(): string[] {
    if (!this.categoryWordFrequency) return [];
    return Object.keys(this.categoryWordFrequency).filter(
      (k) => Object.keys(this.categoryWordFrequency![k]).length > 0
    );
  }

  domainLabel(key: string): string {
    return DOMAIN_LABELS[key] ?? key;
  }

  ngOnChanges(): void {
    this.buildChart();
  }

  selectTab(tab: string): void {
    this.activeTab = tab;
    this.buildChart();
  }

  private buildChart(): void {
    let data: { name: string; value: number }[];
    let colorSet: string[];

    if (this.activeTab === 'GERAL') {
      data = this.topWords.map((w) => ({ name: w.word, value: w.frequency }));
      colorSet = DEFAULT_COLORS;
    } else {
      const freq = this.categoryWordFrequency?.[this.activeTab] ?? {};
      data = Object.entries(freq)
        .map(([word, frequency]) => ({ name: word, value: frequency }))
        .sort((a, b) => b.value - a.value);
      colorSet = DOMAIN_COLORS[this.activeTab] ?? DEFAULT_COLORS;
    }

    if (data.length === 0) {
      this.chartOptions = null;
      return;
    }

    (this.chartOptions as unknown) = {
      tooltip: {
        formatter: (params: { name: string; value: number }) =>
          `${params.name}: <b>${params.value} ocorrências</b>`,
      },
      series: [
        {
          type: 'wordCloud',
          shape: 'circle',
          left: 'center',
          top: 'center',
          width: '90%',
          height: '85%',
          sizeRange: [14, 56],
          rotationRange: [-45, 45],
          rotationStep: 15,
          gridSize: 8,
          drawOutOfBound: false,
          textStyle: {
            fontFamily: 'sans-serif',
            fontWeight: 'bold',
            color: () => colorSet[Math.floor(Math.random() * colorSet.length)],
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
