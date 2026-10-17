package dio.compliance.infrastructure.rest.client;

import dio.compliance.infrastructure.rest.dto.SanctionResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("sanction-client")
public interface SanctionClient {

    @GetMapping("/sanctions/companies/{registrationNumber}")
    SanctionResult getCompanyRisk(@PathVariable String registrationNumber);
}
