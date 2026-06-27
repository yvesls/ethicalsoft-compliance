export interface IsepHistoryEntry {
  questionnaireId: number;
  questionnaireName: string;
  stageName: string;
  iterationName: string | null;
  iseq: number;
  iseqPercent: number;
  band: Band;
  calculatedAt: string;
  ethicsDebtPercent: number | null;
  techDebtPercent: number | null;
}

export interface ProjectIsepDashboardDTO {
  projectId: number;
  projectName: string;
  projectType: 'ITERATIVO' | 'CASCATA';
  projectIsep: number | null;
  projectIsepPercent: number | null;
  projectBand: Band | null;
  teamSimpleAveragePercent: number | null;
  teamStandardDeviationPercent: number | null;
  isepHistory: IsepHistoryEntry[];
  totalQuestionnaires: number;
  completedQuestionnaires: number;
  ethicsScorePercent: number | null;
  processScorePercent: number | null;
  fairnessScorePercent: number | null;
  esgScorePercent: number | null;
  ethicsDebtPercent: number | null;
  techDebtPercent: number | null;
}
export interface MemberResult {
  representativeId: number;
  representativeName: string;
  icp: number;
  icpPercent: number;
  band: Band;
}

export interface StageResult {
  representativeId: number;
  representativeName: string;
  stageId: number;
  stageName: string;
  iem: number;
  iemPercent: number;
  band: Band;
}

export interface QuestionnaireIsepDashboardDTO {
  questionnaireId: number;
  questionnaireName: string;
  stageName: string;
  iterationName: string | null;
  iseq: number;
  iseqPercent: number;
  band: Band;
  teamSimpleAverage: number;
  teamSimpleAveragePercent: number;
  teamStandardDeviation: number;
  teamStandardDeviationPercent: number;
  calculatedAt: string;
  bandDistribution: Record<Band, number>;
  memberResults: MemberResult[];
  stageResults: StageResult[];
  justificationTexts: string[];
  ethicsScorePercent: number | null;
  processScorePercent: number | null;
  fairnessScorePercent: number | null;
  esgScorePercent: number | null;
  ethicsDebtPercent: number | null;
  techDebtPercent: number | null;
}

export interface StageIemEntry {
  stageId: number;
  stageName: string;
  iemPercent: number;
  band: Band;
  memberCount: number;
}

export interface RoleStageComplianceDTO {
  roleId: number;
  roleName: string;
  iemByStage: Record<string, StageIemEntry>;
}

export interface WordEntry {
  word: string;
  frequency: number;
}

export interface WordCloudDTO {
  questionnaireId: number;
  wordFrequency: Record<string, number>;
  topWords: WordEntry[];
  totalJustifications: number;
  categoryWordFrequency: Record<string, Record<string, number>> | null;
  topThemeInsights: Record<string, string> | null;
}

export interface PersonalEvolutionEntry {
  questionnaireId: number;
  questionnaireName: string;
  stageName: string;
  iterationName: string | null;
  isep: number;
  isepPercent: number;
  band: Band;
  calculatedAt: string;
  ethicsDebtPercent: number | null;
  techDebtPercent: number | null;
}

export interface IndividualDashboardDTO {
  representativeId: number;
  representativeName: string;
  personalIcp: number;
  personalIcpPercent: number;
  personalBand: Band;
  personalHistoricalAverage: number;
  personalHistoricalAveragePercent: number;
  teamAnonymousAverage: number | null;
  teamAnonymousAveragePercent: number | null;
  personalEvolution: PersonalEvolutionEntry[];
}

export interface ProjectCloseResultDTO {
  projectId: number;
  isepPercent: string;
  band: Band;
  questionnaireCount: number;
  calculatedAt: string;
  closedBy: string;
}

export interface ExportMemberResult {
  representativeId: number;
  memberName: string | null;
  icpPercent: number;
  band: Band;
  stageResults: { stageId: number; stageName: string; iemPercent: number }[];
}

export interface IsepDataExportDTO {
  projectId: number;
  projectName: string;
  questionnaireId: number;
  questionnaireName: string;
  iterationOrStageName: string;
  calculatedAt: string;
  isepPercent: number;
  band: Band;
  teamAveragePercent: number;
  standardDeviationPercent: number;
  ethicsScorePercent: number | null;
  processScorePercent: number | null;
  fairnessScorePercent: number | null;
  esgScorePercent: number | null;
  ethicsDebtPercent: number | null;
  techDebtPercent: number | null;
  members: ExportMemberResult[];
}

export interface JustificationDTO {
  descricao: string;
  url: string | null;
}

export interface AnswerDTO {
  questionId: number;
  questionText: string;
  stageIds: number[];
  roleIds: number[];
  response: boolean;
  justification: JustificationDTO | null;
  evidence: JustificationDTO | null;
  attachments: string[];
}

export interface RepresentativeResponseDTO {
  representativeId: number;
  representativeName: string;
  questionnaireId: number;
  status: string;
  submissionDate: string;
  totalQuestions: number;
  answeredQuestions: number;
  yesCount: number;
  noCount: number;
  answers: AnswerDTO[];
}

export interface ConsolidatedAnswerDTO {
  representativeId: number;
  representativeName: string;
  roles: string[];
  questionnaireId: number;
  responseStatus: string;
  submissionDate: string;
  questionId: number;
  questionText: string;
  stageIds: number[];
  response: boolean;
  justification: JustificationDTO | null;
  evidence: JustificationDTO | null;
  attachments: string[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ConsolidatedAnswerFilters {
  representativeId?: number | null;
  questionId?: number | null;
  roleId?: number | null;
  response?: boolean | null;
  questionText?: string | null;
  questionnaireId?: number | null;
  page: number;
  size: number;
  sort?: string | null;
}

export type Band = 'A' | 'B' | 'C' | 'D' | 'E';

export interface BandMeta {
  label: string;
  colorClass: string;
  cssColor: string;
  range: string;
}

export const BAND_META: Record<Band, BandMeta> = {
  A: { label: 'A', colorClass: 'band-a', cssColor: '#2e7d32', range: '90–100%' },
  B: { label: 'B', colorClass: 'band-b', cssColor: '#1565c0', range: '75–89,9%' },
  C: { label: 'C', colorClass: 'band-c', cssColor: '#f57f17', range: '60–74,9%' },
  D: { label: 'D', colorClass: 'band-d', cssColor: '#e65100', range: '45–59,9%' },
  E: { label: 'E', colorClass: 'band-e', cssColor: '#c62828', range: '< 45%' },
};
