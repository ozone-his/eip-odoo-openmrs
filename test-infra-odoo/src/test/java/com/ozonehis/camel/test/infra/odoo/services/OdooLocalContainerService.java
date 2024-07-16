/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.camel.test.infra.odoo.services;

import com.ozonehis.camel.test.infra.odoo.common.OdooProperties;
import java.io.File;
import java.net.URL;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@Slf4j
public class OdooLocalContainerService implements OdooService {

    private final ComposeContainer container;

    private final String SERVICE_NAME = "odoo";

    public OdooLocalContainerService() {
        this.container = initContainer();
    }

    @Override
    public int getPort() {
        return container.getServicePort(SERVICE_NAME, OdooProperties.DEFAULT_SERVICE_PORT);
    }

    @Override
    public String getHost() {
        return container.getServiceHost(SERVICE_NAME, OdooProperties.DEFAULT_SERVICE_PORT);
    }

    @Override
    public void registerProperties() {
        System.setProperty(OdooProperties.ODOO_HOST, getHost());
        System.setProperty(OdooProperties.ODOO_PORT, String.valueOf(getPort()));
    }

    @Override
    public void initialize() {
        log.info("Starting Odoo container...");
        container.start();

        registerProperties();
        log.info("Odoo container started");
    }

    @Override
    public void shutdown() {
        log.info("Stopping the Odoo container.");
        container.stop();
        log.info("Odoo container stopped.");
    }

    protected ComposeContainer initContainer() {
        try (var container = new ComposeContainer(getFile("docker-compose/docker-compose-odoo.yml"))
                .withLocalCompose(true)
                .withStartupTimeout(java.time.Duration.ofMinutes(5))
                .withExposedService(SERVICE_NAME, OdooProperties.DEFAULT_SERVICE_PORT, Wait.forListeningPort())) {

            return container;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected File getFile(String fileName) {
        URL url = getClass().getClassLoader().getResource(fileName);
        return new File(Objects.requireNonNull(url).getPath());
    }
}
