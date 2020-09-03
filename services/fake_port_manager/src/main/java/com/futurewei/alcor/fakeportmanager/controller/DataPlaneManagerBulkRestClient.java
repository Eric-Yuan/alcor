package com.futurewei.alcor.fakeportmanager.controller;

import com.futurewei.alcor.common.stats.DurationStatistics;
import com.futurewei.alcor.web.entity.dataplane.NetworkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;


@Configuration
public class DataPlaneManagerBulkRestClient {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${microservices.dataplane.service.url:#{\"\"}}")
    private String dataPlaneManagerUrl;

    RestTemplate restTemplate = new RestTemplate();

    @DurationStatistics
    public void createNetworkConfig(NetworkConfiguration message) throws Exception {
        HttpEntity<NetworkConfiguration> request = new HttpEntity<>(message);
        restTemplate.postForObject(dataPlaneManagerUrl, request, Object.class);
    }

}