/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Submits http requests to OpenMRS.
 */
@Component
public class OpenmrsRestClient {

    protected static final String PATH = "/ws/rest/v1/";

    protected static final BodyHandler<String> BODY_HANDLER = BodyHandlers.ofString(UTF_8);

    @Value("${openmrs.baseUrl}")
    private String baseUrl;

    @Value("${openmrs.username}")
    private String username;

    @Value("${openmrs.password}")
    private char[] password;

    private HttpClient client;

    public String get(String resource, String uuid) throws Exception {
        HttpResponse<String> response = sendRequest(resource, uuid, null, Set.of(HttpStatus.OK, HttpStatus.NOT_FOUND));
        if (response.statusCode() != HttpStatus.OK.value()) {
            return null;
        }

        return response.body();
    }

    public String create(String resource, String uuid, String body) throws Exception {
        return sendRequest(resource, uuid, body, Set.of(HttpStatus.CREATED)).body();
    }

    public HttpResponse<String> sendRequest(String resource, String uuid, String body, Set<HttpStatus> allowedStatuses)
            throws Exception {
        String uri = baseUrl + PATH + resource;
        if (uuid != null) {
            uri += ("/" + uuid);
        }

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder();
        reqBuilder.uri(URI.create(uri));
        HttpRequest.BodyPublisher bodyPublisher;
        if (body != null) {
            reqBuilder.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            bodyPublisher = BodyPublishers.ofString(body, UTF_8);
            reqBuilder.POST(bodyPublisher);
        } else {
            reqBuilder.GET();
        }

        HttpResponse<String> response;
        try {
            response = getClient().send(reqBuilder.build(), BODY_HANDLER);
        } catch (Exception e) {
            throw new Exception("An error occurred while submitting resource to OpenMRS: " + resource, e);
        }

        if (allowedStatuses.contains(response.statusCode())) {
            throw new Exception("Request to OpenMRS failed with status code " + response.statusCode());
        }

        return response;
    }

    private HttpClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = HttpClient.newBuilder()
                            .authenticator(new Authenticator() {

                                @Override
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(username, password);
                                }
                            })
                            .build();
                }
            }
        }

        return client;
    }
}
