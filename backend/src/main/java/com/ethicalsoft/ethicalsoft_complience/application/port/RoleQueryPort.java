package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;

import java.util.List;

public interface RoleQueryPort {
    List<RoleSummaryResponseDTO> listRoleSummaries();
}

