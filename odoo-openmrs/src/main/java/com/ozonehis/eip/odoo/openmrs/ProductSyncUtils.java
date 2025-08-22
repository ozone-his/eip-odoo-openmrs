/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

public class ProductSyncUtils {

    private static final String SOURCE_QUERY = "SELECT s.name FROM concept_reference_source s, "
            + "fhir_concept_source f WHERE s.concept_source_id = f.concept_source_id AND f.url = ?";

    protected static String getConceptSourceName(String uri, DataSource dataSource) throws Exception {
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement(SOURCE_QUERY)) {
            s.setString(1, uri);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    return rs.getString(1);
                }
            }
        }

        return null;
    }
}
