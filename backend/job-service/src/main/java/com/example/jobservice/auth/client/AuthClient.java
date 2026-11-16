package com.example.jobservice.auth.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AuthClient {

    AuthHrAccount createHrAccount(String email, String fullName, UUID companyId);

    AuthHrAccount lockHrAccount(UUID userId, String reason);

    AuthHrAccount unlockHrAccount(UUID userId);

    List<AuthHrAccount> listHrAccounts(UUID companyId);

    Map<UUID, Long> countHrAccounts(Collection<UUID> companyIds);
}

