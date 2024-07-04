/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.mapper;

import com.ozonehis.eip.odoo.openmrs.model.OdooResource;

/**
 * An Interface for mapping from FHIR Resources to Odoo Resources
 *
 * @param <F> FHIR Resource
 * @param <E> Odoo Resource
 */
public interface ToOdooMapping<F, E extends OdooResource> {

    /**
     * Maps a FHIR Resource to an Odoo Resource
     *
     * @param fhirResource FHIR Resource
     * @return Odoo Resource
     */
    E toOdoo(F fhirResource);
}
