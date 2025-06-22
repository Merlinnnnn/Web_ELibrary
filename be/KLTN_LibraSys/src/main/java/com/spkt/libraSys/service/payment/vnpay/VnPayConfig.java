package com.spkt.libraSys.service.payment.vnpay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vnp")
public class VnPayConfig {
    private String vnp_PayUrl;
    private String vnp_TmnCode;
    private String secretKey;
    private String vnp_ReturnUrl;
    private String vnp_ReturnUr_Fe;
}
