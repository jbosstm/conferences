/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const os = require('os');
const fs = require('fs');

const express = require('express');
const router = express.Router();
const request = require('request');

// chaining
const roi = require('roi');
const circuitBreaker = require('opossum');

// authorized routes
const Keycloak = require('keycloak-connect');

// business logic
const sayBonjour = () => `Bonjour de ${os.hostname()}`;

const debug = process.env.npm_config_debug

// circuit breaker
const circuitOptions = {
  maxFailures: 5,
  timeout: 1000,
  resetTimeout: 10000
};

const nextService = 'ola';
function fallback () {
  return [`The ${nextService} service is currently unavailable.`];
}

function getAndTraceLraCall(req, res, action) {
  var lraUrl = req.headers['long-running-action']
  console.log(`Called ${action} for lra: ${lraUrl}`)
  // console.log("Headers of the request " + JSON.stringify(req.headers))
  var lraIds = lraUrl.match('[^/]+$')
  return lraIds[0] 
}

const circuit = circuitBreaker(roi.get, circuitOptions);
circuit.fallback(fallback);

// Define routes
router.routes = (store) => {
  console.log(store)
  router.get('/bonjour', (req, resp) => {

    var host = 'lra-coordinator'
    if(process.env.npm_config_lra_http_host) host = process.env.npm_config_lra_http_host
    var port = 8080
    if(process.env.npm_config_lra_http_port) port = process.env.npm_config_lra_http_port
    var baseUrl = `${req.protocol}://${req.headers['host']}${req.baseUrl}`
    var lraUrl = req.headers['long-running-action'];

    var headerString = `<${baseUrl}/leave>; rel="leave"; title="leave URI"; type="text/plain",<${baseUrl}/complete>; rel="complete"; title="complete URI"; type="text/plain",<${baseUrl}/compensate>; rel="compensate"; title="compensate URI"; type="text/plain",<${baseUrl}/status>; rel="status"; title="status URI"; type="text/plain"`
  	if(debug)
        console.log(`base url: ${baseUrl},\n lra url: ${lraUrl},\n all req.headers: ${JSON.stringify(req.headers)},\n header to coordinator: ${headerString}`);

    request({
          method: "PUT",
          uri: lraUrl,
          headers: {
              'Content-Type': 'text/plain',
              'long-running-action': lraUrl,
              "Link": headerString
          },
          // forever: true,
          body: headerString
      }, function (error, response, body){
    	  if(debug)
              console.log(`Response: ${JSON.stringify(response)},\n body: ${body},\n error: ${error}`);
    	  var resultGreetings = sayBonjour()
          if(lraUrl && response.statusCode != 200) resultGreetings += " (failed)"
   		  resp.type('text/plain').send(resultGreetings)
      }
    );
  });

  router.put('/complete', (req, resp) => {
    var lraId = getAndTraceLraCall(req, resp, 'complete')
    console.log(`I was happy to say 'bonjour' [${lraId} completed]`);
    resp.type('text/plain').send('Ok');
  });

  router.put('/compensate', (req, resp) => {
    var lraId = getAndTraceLraCall(req, resp, 'compensate')
    console.log(`I'm sorry, I didn't mean the greetings 'bonjour'. I'm taking it back [${lraId} compensated]`)
    resp.type('text/plain').send('Ok');
  });

  router.get('/status', (req, resp) => {
    getAndTraceLraCall(req, resp, 'status')
    resp.type('text/plain').send('Completed');
  });


  router.get('/health', (req, resp) => {
    resp.type('text/plain').send('I am ok');
  });

  router.get('/bonjour-chaining', (req, resp) =>
    circuit.fire({endpoint: `http://${nextService}:8080/api/${nextService}-chaining`}).then((response) => {
      resp.set('Access-Control-Allow-Origin', '*');
      resp.type('application/json').send(response);
    }).catch((e) => resp.send(e))
  );

  // Configure keycloak based on keycloak.json and the KEYCLOAK_AUTH_SERVER_URL env var
  const customKeyCloakConfig = JSON.parse(fs.readFileSync(`${__dirname}/keycloak.json`).toString());
  customKeyCloakConfig.authServerUrl = process.env.KEYCLOAK_AUTH_SERVER_URL;
  const keycloak = new Keycloak({ scope: 'USERS', store }, customKeyCloakConfig);

  // add a logout route
  router.use(keycloak.middleware({ logout: '/logout' }));

  // add a secured route
  router.get('/bonjour-secured', keycloak.protect(),
    (req, resp) => resp
      .type('text/plain')
      .send(`This is a Secured resource. You're logged as ${req.kauth.grant.access_token.content.name}`));

  return router;
};

module.exports = exports = router;
