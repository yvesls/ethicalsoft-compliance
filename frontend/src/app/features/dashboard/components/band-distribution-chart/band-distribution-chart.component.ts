import { Component, Input, OnChanges, inject, ChangeDetectionStrategy } from '@angular/core'
import { NgxEchartsDirective } from 'ngx-echarts'
import { TranslateService } from '@ngx-translate/core'
import type { EChartsOption } from 'echarts'
import { Band, BAND_META } from '../../interfaces/dashboard.interface'

@Component({
	selector: 'app-band-distribution-chart',
	standalone: true,
	imports: [NgxEchartsDirective],
	templateUrl: './band-distribution-chart.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './band-distribution-chart.component.scss',
})
export class BandDistributionChartComponent implements OnChanges {
	@Input({ required: true }) distribution: Record<Band, number> = { A: 0, B: 0, C: 0, D: 0, E: 0 }

	chartOptions: EChartsOption = {}

	private readonly translate = inject(TranslateService)

	ngOnChanges(): void {
		this.buildChart()
	}

	private buildChart(): void {
		const bands: Band[] = ['A', 'B', 'C', 'D', 'E']
		const bandLabel = this.translate.instant('dashboard.chart.band_x_axis')
		const memberCount = this.translate.instant('dashboard.chart.member_count')
		const data = bands.map((b) => ({
			value: this.distribution[b] ?? 0,
			itemStyle: { color: BAND_META[b].cssColor },
		}))

		this.chartOptions = {
			title: {
				text: this.translate.instant('dashboard.chart.band_distribution_title'),
				subtext: this.translate.instant('dashboard.chart.band_distribution_subtitle'),
				left: 'center',
				textStyle: { fontSize: 14 },
			},
			tooltip: {
				trigger: 'axis',
				formatter: (params: unknown) => {
					const p = (params as { name: string; value: number }[])[0]
					return `${bandLabel} ${p.name}: <b>${p.value} ${memberCount}</b>`
				},
			},
			xAxis: { type: 'category', data: bands, name: bandLabel },
			yAxis: { type: 'value', name: this.translate.instant('dashboard.chart.members_y_axis'), minInterval: 1 },
			series: [
				{
					type: 'bar',
					data,
					label: { show: true, position: 'top' },
				},
			],
			grid: { containLabel: true, left: '5%', right: '5%' },
		}
	}
}
