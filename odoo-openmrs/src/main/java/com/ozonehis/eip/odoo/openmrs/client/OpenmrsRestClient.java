/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
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

    protected static final BodyHandler<byte[]> BODY_HANDLER = BodyHandlers.ofByteArray();

    @Value("${openmrs.baseUrl}")
    private String baseUrl;

    @Value("${openmrs.username}")
    private String username;

    @Value("${openmrs.password}")
    private char[] password;

    private HttpClient client = HttpClient.newHttpClient();

    public byte[] get(String resource, String uuid) throws Exception {
        HttpResponse<byte[]> response =
                sendRequest(resource, uuid, null, false, Set.of(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value()));
        if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
            return null;
        }

        return response.body();
    }

    public byte[] createOrUpdate(String resource, String uuid, String body) throws Exception {
        final HttpStatus expectedStatus = (uuid == null) ? HttpStatus.CREATED : HttpStatus.OK;
        return sendRequest(resource, uuid, body, false, Set.of(expectedStatus.value()))
                .body();
    }

    public byte[] delete(String resource, String uuid) throws Exception {
        return sendRequest(resource, uuid, null, true, Set.of(HttpStatus.NO_CONTENT.value()))
                .body();
    }

    public HttpResponse<byte[]> sendRequest(
            String resource, String uuid, String body, boolean delete, Set<Integer> allowedStatuses) throws Exception {
        String uri = baseUrl + PATH + resource;
        if (uuid != null) {
            uri += ("/" + uuid);
        }

        final String userAndPass = username + ":" + new String(password);
        byte[] auth = Base64.getEncoder().encode(userAndPass.getBytes(UTF_8));
        HttpRequest.Builder reqBuilder =
                HttpRequest.newBuilder().setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(auth, UTF_8));
        reqBuilder.uri(URI.create(uri));
        if (body != null) {
            reqBuilder.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            BodyPublisher bodyPublisher = BodyPublishers.ofString(body, UTF_8);
            reqBuilder.POST(bodyPublisher);
        } else if (delete) {
            reqBuilder.DELETE();
        } else {
            reqBuilder.GET();
        }

        HttpResponse<byte[]> response;
        try {
            response = client.send(reqBuilder.build(), BODY_HANDLER);
        } catch (Exception e) {
            throw new Exception("An error occurred while submitting resource to OpenMRS: " + resource, e);
        }

        if (!allowedStatuses.contains(response.statusCode())) {
            String msg = " with status code: " + response.statusCode();
            if (response.body() != null) {
                msg += (", " + new String(response.body(), UTF_8));
            }

            throw new Exception("Request to OpenMRS failed" + msg);
        }

        return response;
    }
}
