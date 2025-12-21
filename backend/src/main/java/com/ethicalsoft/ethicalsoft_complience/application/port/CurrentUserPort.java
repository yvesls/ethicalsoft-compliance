package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;

public interface CurrentUserPort {
    User getCurrentUser();
}

