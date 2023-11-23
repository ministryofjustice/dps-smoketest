# DPS Smoke Tests

[![CircleCI](https://circleci.com/gh/ministryofjustice/dps-smoketest/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/dps-smoketest)
[![Docker](https://quay.io/repository/hmpps/dps-smoketest/status)](https://quay.io/repository/dps-smoketest/status)
[![API docs](https://img.shields.io/badge/API_docs_(needs_VPN)-view-85EA2D.svg?logo=swagger)](https://dps-smoketest-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

# Overview

A service to run e2e smoke tests over applications deployed in DPS environments.

Clients of this service should be dumb - all setting up of data, running of tests and subsequent assertions should be 
performed in this service.

## Scope
This is not intended to be a huge suite of functional tests that probe every outcome of some or all of the DPS ecosystem.

The goal of these tests is to give confidence in a deployment by testing high value happy paths. 

As such the tests should be easy to trigger and assert on, and cover as many dependant services as is reasonable.

## Usage
These tests should be called by any services that may impact upon the test following a deployment of that service.

These tests are run as part of the automated delivery pipeline.

## Data
Smoke tests in this suite should:
* Create and tear down their own test data
* Where this is not possible use manually created test data but set up and tear down that data with this service
* Assert that test data is fit for testing prior to running the test - and fail fast if not

## Test Profiles
Test profiles distinguish inputs and outputs for a test in different environments or for different test clients.

e.g. If you want to run your test on both T3 and Preprod, create a profile for each to capture the different inputs /
outputs.

e.g. If you want to run the test on T3 for both service A and service B, create a profile for each service to capture 
the different inputs / outputs.

Note that we do not want to be in the situation where different services run the same smoke test on the same environment
with the same test data - this will clearly cause conflicts.

## Server Sent Events
It is recommended to follow the example of `/smoke-test/prison-offender-events/{testProfile}` by returning a Flux of 
TestStatus events which can be reported on by the test client.

This allows for long running tests that avoid timeouts and provide frequent status updates thus enhancing the experience
for test clients.

The downside is that you will have to implement your own error handling as support is weak for SSE (e.g. Controller 
Advice is not supported).

But if you have a quick test that doesn't need SSE then that's great, go for it.

## Supporting Services
You may need to create additional APIs in other services to configure / assert on test data - please call those 
endpoints from this service and not from the test client (to centralize test logic).

Note that any such supporting APIs should be protected by the SMOKE_TEST role and should be feature switched to not run 
in production.

(We may extend these tests to production ghost prisons at some point in the future, but not yet.)

# Where are the Tests?
The tests themselves are documented in the OpenApi docs.
