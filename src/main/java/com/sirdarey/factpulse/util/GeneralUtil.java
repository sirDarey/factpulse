package com.sirdarey.factpulse.util;

import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@UtilityClass
public class GeneralUtil {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private final PhoneNumberToTimeZonesMapper tzMapper = PhoneNumberToTimeZonesMapper.getInstance();

    /**
     * Returns the most likely timezone for a given international phone number.
     * Returns "UTC" when timezone cannot be determined.
     */
    public String getTimezoneFromPhoneNumber(String phoneNumber) {
        try {
            // Parse phone number (null default region allows full international format)
            Phonenumber.PhoneNumber parsed = phoneNumberUtil.parse(phoneNumber, null);

            // Get potential timezones
            List<String> timezones = tzMapper.getTimeZonesForNumber(parsed);
            log.info("getTimezoneFromPhoneNumber[{}] :: {}", phoneNumber, timezones);

            if (timezones == null || timezones.isEmpty()) {
                log.warn("⚠️ Could not determine timezone for phone number: {}", phoneNumber);
                return "UTC";
            }

            // Return first (most likely) timezone
            return timezones.get(0);

        } catch (Exception e) {
            log.error("❌ Failed to parse phone number '{}': {}", phoneNumber, e.getMessage());
            return "UTC";
        }
    }
}