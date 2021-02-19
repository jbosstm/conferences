/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.developers.msa.ola;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.narayana.lra.annotation.CompensatorStatus;
import io.narayana.lra.client.LRAClient;
import io.narayana.lra.client.LRAClientAPI;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class OlaController {

    @Autowired
    private HolaService holaService;

    @Autowired
    private LRAClientAPI lraClient;

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/ola", produces = "text/plain")
    @ApiOperation("Returns the greeting in Portuguese")
    public String ola(HttpEntity<byte[]> requestEntity) {
        String lraIdUrl = requestEntity.getHeaders().getFirst(LRAClient.LRA_HTTP_HEADER);

        boolean failed = false;
        if(lraIdUrl == null || lraIdUrl.isEmpty()) {
            System.out.println("No content of header '" + LRAClient.LRA_HTTP_HEADER + "' necessary to join LRA");
            failed = true;
        }

        if(!failed) try {
            String lraId = LRAClient.getLRAId(lraIdUrl);
            System.out.println("joining lra: " + lraId + " at " + getBaseUri());
            String recoveryPath = lraClient.joinLRA(LRAClient.lraToURL(lraId), 0L, getBaseUri(), null);
            System.out.println("recovery path: " + recoveryPath);
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }

        return sayOla() + (failed ? " (failed)" : "");
    }

    private String sayOla() {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "Unknown");
        return String.format("Olá de %s", hostname);
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/ola-chaining", produces = "application/json")
    @ApiOperation("Returns the greeting plus the next service in the chain")
    public List<String> sayHelloChaining() {
        // System.out.println("OK, lra client is ready to go: " + lraClient + " with current lra: " + lraClient.getCurrent());
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        URL lraUrlId = lraClient.startLRA(null, OlaController.class.getName() + "#" + methodName, 0L, TimeUnit.SECONDS);
        String recoveryPath = lraClient.joinLRA(lraUrlId, 0L, getBaseUri(), null);
        // System.out.println("Starting LRA: " + lraUrlId + " when joining with baseUri: " + getBaseUri()
        //    + " on enlistment gets recovery path " + recoveryPath);

        List<String> greetings = new ArrayList<>();
        greetings.add(sayOla());
        greetings.addAll(holaService.hola(lraUrlId.toString()));

        if(greetings.stream().map(s -> s.toLowerCase()).anyMatch(s -> s.contains("failed") || s.contains("fallback"))) {
            lraClient.cancelLRA(lraUrlId);
        } else {
            lraClient.closeLRA(lraUrlId);
        }

        return greetings;
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/ola-secured", produces = "text/plain")
    @ApiOperation("Returns a message that is only available for authenticated users")
    public String olaSecured(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        AccessToken token = principal.getKeycloakSecurityContext().getToken();
        return "This is a Secured resource. You are logged as " + token.getName();
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/logout", produces = "text/plain")
    @ApiOperation("Logout")
    public String logout() throws ServletException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        request.logout();
        return "Logged out";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/health")
    @ApiOperation("Used to verify the health of the service")
    public String health() {
        return "I'm ok";
    }


    // === Ola LRA handling ===
    // ============================================================
    @RequestMapping(method = RequestMethod.PUT, value = "/complete", produces = MediaType.APPLICATION_JSON)
    @ApiOperation("For succesful terminating LRA action on the Ola side")
    public Response completeWork(@RequestHeader(LRAClient.LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        String txId = LRAClient.getLRAId(lraId);
        // String statusUrl = String.format("%s/%s/completed", getBaseUri(), txId);

        System.out.printf("I was happy to say 'ola' [%s completed]%n", txId);
        return Response.ok().build();
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/compensate", produces = MediaType.APPLICATION_JSON)
    @ApiOperation("For wrong terminating LRA action on the Ola side")
    public Response compensateWork(@RequestHeader(LRAClient.LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        String txId = LRAClient.getLRAId(lraId);
        // String statusUrl = String.format("%s/%s/compensated", getBaseUri(), txId);

        System.out.printf("I'm sorry, I didn't mean the greetings 'ola'. I'm taking it back [%s compensated]%n", txId);
        return Response.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/status", produces = MediaType.APPLICATION_JSON)
    public Response status(@RequestHeader(LRAClient.LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        String txId = LRAClient.getLRAId(lraId);
        System.out.println("Call status for: '" + txId + "'");
        return Response.ok(CompensatorStatus.Completed).build();
    }

    private String getBaseUri() {
        String serviceURL = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        Pattern pattern = Pattern.compile("^(.*/api).*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(serviceURL);
        String baseUri;
        if(matcher.find()) {
            baseUri = matcher.group(1);
        } else {
            baseUri = serviceURL;
        }
        return baseUri;
    }
}
