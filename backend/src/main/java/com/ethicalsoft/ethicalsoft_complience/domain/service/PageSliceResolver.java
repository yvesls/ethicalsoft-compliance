package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class PageSliceResolver {

    public PageSlice resolve(int page, int size, int totalElements) {
        if (size <= 0) {
            throw new BusinessException("Tamanho de página inválido");
        }
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (page < 0 || (totalPages > 0 && page >= totalPages)) {
            throw new BusinessException("Página solicitada está fora do intervalo disponível");
        }
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        return new PageSlice(fromIndex, toIndex, totalPages == 0 ? 1 : totalPages);
    }

    public record PageSlice(int fromIndex, int toIndex, int totalPages) {}
}

