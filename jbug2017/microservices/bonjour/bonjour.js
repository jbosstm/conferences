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
const path = require('path');
const express = require('express');
const session = require('express-session');
const api = require('./lib/api');
const tracingConfiguration = require('./lib/tracing');

const app = express();

app.use(express.static(path.join(__dirname, 'public/swagger')));

// tracing initialization
tracingConfiguration.init(app);

// Create a session-store to be used by both the express-session
// middleware and the keycloak middleware.
const memoryStore = new session.MemoryStore();

app.use(session({
  secret: 'mySecret',
  resave: false,
  saveUninitialized: true,
  store: memoryStore
}));

// Enable CORS
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  next();
});

app.use('/api', api.routes(memoryStore));

// default route (should be swagger)
app.get('/', (req, res) => res.send('Logged out'));


// --port=8181
var port = 8080
if(process.env.npm_config_http_port) port = process.env.npm_config_http_port

const server = app.listen(port, '0.0.0.0', () => {
  const host = server.address().address;
  const port = server.address().port;

  console.log('Bonjour service running at http://%s:%s', host, port);
});
