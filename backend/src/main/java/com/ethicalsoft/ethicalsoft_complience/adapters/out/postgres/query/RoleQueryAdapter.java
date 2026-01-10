package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.query;

import com.ethicalsoft.ethicalsoft_complience.application.port.RoleQueryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class RoleQueryAdapter implements RoleQueryPort {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleSummaryResponseDTO> listRoleSummaries() {
        return roleRepository.findAll().stream()
                .map(role -> new RoleSummaryResponseDTO(role.getId(), role.getName()))
                .collect(toList());
    }
}
