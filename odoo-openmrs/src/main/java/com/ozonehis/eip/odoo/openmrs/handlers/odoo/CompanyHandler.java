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
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class CompanyHandler {

    @Autowired
    private OdooClient odooClient;

    public Integer getCompanyIdByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Object[] records = odooClient.searchAndRead(
                Constants.COMPANY_MODEL, List.of(asList("name", "=", name)), List.of("id", "name"));
        if (records == null) {
            throw new EIPException(
                    String.format("Got null response while fetching for res.company with name %s", name));
        } else if (records.length == 0) {
            log.warn("No res.company found with name {}", name);
            return null;
        } else if (records.length > 1) {
            log.warn("Multiple res.company found with name {}", name);
            throw new EIPException(String.format("Multiple res.company found with name %s", name));
        }
        Object id = ((Map<String, Object>) records[0]).get("id");
        if (id instanceof Integer) {
            return (Integer) id;
        }
        if (id instanceof Number) {
            return ((Number) id).intValue();
        }
        throw new EIPException(String.format("Unexpected id type returned for res.company name %s: %s", name, id));
    }
}
