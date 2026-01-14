package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.RoleQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListRolesUseCase {

    private final RoleQueryPort roleQueryPort;

    public List<RoleSummaryResponseDTO> execute() {
        return roleQueryPort.listRoleSummaries();
    }
}
