/*
 *
 * Copyright 2019 The Alcor Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 * /
 */

package com.futurewei.alcor.apigateway.route;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityGroupRouteConfiguration {

    @Value("${microservices.sg.service.url}")
    private String sgUrl;

    @Bean
    public RouteLocator securityGroupLocator(RouteLocatorBuilder builder){
        return builder.routes()
                .route(r -> r.path("/*/security-groups", "/*/security-groups/*",
                        "/*/security-group-rules", "/*/security-group-rules/*",
                        "/project/*/security-groups", "/project/*/security-groups/*",
                        "/project/*/security-group-rules", "/project/*/security-group-rules/*")
                .uri(sgUrl))
                .build();

    }
}
