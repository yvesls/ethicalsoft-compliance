package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ProjectCurrentIterationPolicy {

    public Integer findCurrentIterationNumber(Set<Iteration> iterations, LocalDate now) {
        if (ObjectUtils.isNullOrEmpty( iterations )) {
            return null;
        }

        List<Iteration> sorted = iterations.stream()
                .filter(it -> it.getApplicationStartDate() != null)
                .sorted(Comparator.comparing(Iteration::getApplicationStartDate))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            Iteration it = sorted.get(i);
            if (it.getApplicationEndDate() != null &&
                    !now.isBefore(it.getApplicationStartDate()) && !now.isAfter(it.getApplicationEndDate())) {
                return i + 1;
            }
        }
        return null;
    }
}
