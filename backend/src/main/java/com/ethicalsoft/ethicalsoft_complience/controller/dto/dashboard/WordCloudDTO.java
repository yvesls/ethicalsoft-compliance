package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.util.List;
import java.util.Map;

public record WordCloudDTO(
        Integer questionnaireId,
        Map<String, Long> wordFrequency,
        List<WordEntry> topWords,
        int totalJustifications
) {
    public record WordEntry(String word, long frequency) {}
}

