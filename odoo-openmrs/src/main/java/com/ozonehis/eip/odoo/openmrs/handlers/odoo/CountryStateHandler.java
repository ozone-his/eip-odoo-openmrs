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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class CountryStateHandler {

    @Autowired
    private OdooClient odooClient;

    public Integer getStateId(String stateName) {
        Object[] records = odooClient.search(Constants.COUNTRY_STATE_MODEL, asList("name", "=", stateName));
        if (records.length > 1) {
            throw new EIPException(
                    String.format("Found %s states in odoo matching name: %s", records.length, stateName));
        } else if (records.length == 0) {
            log.warn("No state found in odoo matching name: {}", stateName);
            return null;
        }
        return (Integer) records[0];
    }
}
