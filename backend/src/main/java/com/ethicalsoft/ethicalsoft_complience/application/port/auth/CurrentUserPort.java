package com.ethicalsoft.ethicalsoft_complience.application.port.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;

public interface CurrentUserPort {
    User getCurrentUser();
}

