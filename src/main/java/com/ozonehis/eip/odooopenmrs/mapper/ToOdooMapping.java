/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.mapper;

import com.ozonehis.eip.odooopenmrs.model.OdooDocument;

/**
 * An Interface for mapping from FHIR Resources to Odoo Documents
 *
 * @param <F> FHIR Resource
 * @param <E> Odoo Document
 */
public interface ToOdooMapping<F, E extends OdooDocument> {

    /**
     * Maps a FHIR Resource to an Odoo Document
     *
     * @param fhirResource FHIR Resource
     * @return Odoo Document
     */
    E toOdoo(F fhirResource);
}
