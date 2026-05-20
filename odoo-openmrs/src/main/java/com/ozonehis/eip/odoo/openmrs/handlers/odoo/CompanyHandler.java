/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers.odoo;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class CompanyHandler {

    @Autowired
    private OdooClient odooClient;

    public Integer getCompanyIdByExternalId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return null;
        }
        Object[] records = odooClient.searchAndRead(
                Constants.IR_MODEL,
                asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", externalId)),
                asList("res_id"));
        if (records == null) {
            throw new EIPException(
                    String.format("Got null response while fetching for res.company with external id %s", externalId));
        } else if (records.length == 0) {
            log.warn("No res.company found with external id {}", externalId);
            return null;
        } else if (records.length > 1) {
            log.warn("Multiple res.company external id mappings found for {}", externalId);
            throw new EIPException(String.format("Multiple res.company external id mappings found for %s", externalId));
        }
        Object resId = ((Map<String, Object>) records[0]).get("res_id");
        if (resId instanceof Integer) {
            return (Integer) resId;
        }
        if (resId instanceof Number) {
            return ((Number) resId).intValue();
        }
        throw new EIPException(
                String.format("Unexpected res_id type returned for res.company external id %s: %s", externalId, resId));
    }

    public Integer getCompanyIdByEncounterLocation(Encounter encounter) {
        String locationUuid = resolveEncounterLocationUuid(encounter);
        if (locationUuid == null) {
            log.warn("Encounter {} has no location reference", encounter == null ? null : encounter.getIdPart());
            return null;
        }
        Integer companyId = getCompanyIdByExternalId(locationUuid);
        if (companyId == null) {
            log.warn(
                    "No res.company external id matches location uuid {} for encounter {}",
                    locationUuid,
                    encounter.getIdPart());
            return null;
        }
        return companyId;
    }

    private String resolveEncounterLocationUuid(Encounter encounter) {
        if (encounter == null || !encounter.hasLocation()) {
            return null;
        }
        String uuid = encounter
                .getLocationFirstRep()
                .getLocation()
                .getReferenceElement()
                .getIdPart();
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        return uuid.trim();
    }
}
