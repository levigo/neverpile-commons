package com.neverpile.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.jadice.license.LicenseConfiguration;
import com.jadice.license.LicenseHandler;

import jakarta.annotation.PostConstruct;

/**
 * Service class responsible for license management, checking and reporting for the application.
 * If the license configuration is not provided, the service will return the product name as "basic".
 */
@Service
public final class LCServ {

  private final LicenseHandler handler;

  private static final Logger LOGGER = LoggerFactory.getLogger(LCServ.class.getName());

  public LCServ(final LicenseConfiguration configuration, final LicenseHandler handler) {
    if (!StringUtils.hasText(configuration.getLicense()) || //
        !StringUtils.hasText(configuration.getPublicKey()) && //
            !StringUtils.hasText(configuration.getPublicKeyLocation())) {
      this.handler = null;
      LOGGER.error(
          "A value for 'jadice.license-configuration.license' must be provided. The Application will run in basic mode.");
    } else {
      this.handler = handler;
    }
  }

  /**
   * Initializes the service by logging license information after bean construction.
   */
  @PostConstruct
  private void init() {
    if (LOGGER.isInfoEnabled() && handler != null) {
      final Map<String, Object> info = new LinkedHashMap<>();
      info.put("customer", handler.customer().name());
      info.put("product", handler.product().name());
      info.put("isExpired", handler.license().isExpired());
      LOGGER.info("""

          ------------------------------------
          License check: \
          Current license information:\s
          {}
          ------------------------------------""", info);
    } else {
      LOGGER.error("License configuration not provided.");
    }
  }

  /**
   * Returns the product variant based on the provided license information and the product name.
   * If the license is expired, not provided or not valid, the product variant will be "basic".
   *
   * @return the product variant as a string
   */
  public String getProductVariant() {
    if (handler != null && handler.product() != null && !handler.license().isExpired()) {
      String productName = handler.product().name();
      if (productName.toLowerCase().contains(VariantConstant.ADVANCED)) {
        return VariantConstant.ADVANCED;
      }
    }
    return VariantConstant.BASIC;
  }

}
