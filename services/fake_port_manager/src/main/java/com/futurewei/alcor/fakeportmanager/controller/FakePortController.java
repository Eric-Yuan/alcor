/*
Copyright 2019 The Alcor Authors.

Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/
package com.futurewei.alcor.fakeportmanager.controller;

import com.futurewei.alcor.common.utils.Ipv4AddrUtil;
import com.futurewei.alcor.web.entity.dataplane.InternalPortEntity;
import com.futurewei.alcor.web.entity.dataplane.InternalSubnetEntity;
import com.futurewei.alcor.web.entity.dataplane.NeighborInfo;
import com.futurewei.alcor.web.entity.dataplane.NetworkConfiguration;
import com.futurewei.alcor.web.entity.port.*;
import com.futurewei.alcor.web.entity.route.RouteEntity;
import com.futurewei.alcor.web.entity.subnet.SubnetEntity;
import com.futurewei.alcor.web.entity.vpc.VpcEntity;
import io.netty.util.internal.MacAddressUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


@RestController
public class FakePortController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private DataPlaneManagerBulkRestClient dpmClient;

    private static int HOST_NUM = 1000;
    private static int NEIGHBOR_NUM = 1000;
    private static int SUBNET_NUM = 1000;

    public static class ScaleTestResult {
        public String startTime;
        public String endTime;

        public ScaleTestResult() {
        }

        public ScaleTestResult(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }

    public static class Parameters {
        public int create_count = 1;

        public int getCreate_count() {
            return create_count;
        }

        public void setCreate_count(int create_count) {
            this.create_count = create_count;
        }
    }

    private byte[] longToBytes(long x) {
        return new byte[] {
                (byte) x,
                (byte) (x >> 8),
                (byte) (x >> 16),
                (byte) (x >> 24),
                (byte) (x >> 32),
                (byte) (x >> 40)};
    }

    public long bytesToLong(byte[] bytes) {
        return ((long) bytes[5] & 0xff) << 40
                | ((long) bytes[4] & 0xff) << 32
                | ((long) bytes[3] & 0xff) << 24
                | ((long) bytes[2] & 0xff) << 16
                | ((long) bytes[1] & 0xff) << 8
                | ((long) bytes[0] & 0xff);
    }

    @PostMapping({"/scale-test/bulk-create"})
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ScaleTestResult bulkCreate(@RequestBody Parameters parameters) throws Exception {
        String startTime = new Date().toString();

        int count = parameters.create_count;

        String projectId = "project-1";
        String vpcId = "vpc-1";
        String vpcName = "vpc-1";
        String vpcCidr = "172.16.0.0/12";

        VpcEntity vpcEntity = new VpcEntity(projectId, vpcId, vpcName, vpcCidr, null);

        List<InternalSubnetEntity> subnetInfoList = new ArrayList<>(SUBNET_NUM);
        List<NeighborInfo> neighborInfoList = new ArrayList<>(NEIGHBOR_NUM);
        long hostIPBase = Ipv4AddrUtil.ipv4ToLong("10.0.0.1");
        long macBase = bytesToLong(MacAddressUtil.parseMAC("fe:ab:00:00:00:00"));
        long vpcIpBase = Ipv4AddrUtil.ipv4ToLong("172.16.0.0");

        int loopCount = 0;
        for (int subnetLoop = 0; subnetLoop < SUBNET_NUM; subnetLoop++) {
            String subnetId = "subnet-" + (subnetLoop + 1);
            String subnetName = "subnet-" + (subnetLoop + 1);
            long subnetIPBase = vpcIpBase + subnetLoop * 4096;
            String subnetCidr = Ipv4AddrUtil.longToIpv4(subnetIPBase + subnetLoop * 4096) + "/20";
            Long tunnelId = subnetLoop + 1L;
            subnetInfoList.add(new InternalSubnetEntity(
                    new SubnetEntity(projectId, vpcId, subnetId, subnetName, subnetCidr),
                    tunnelId));

            for (int portLoop = 0; portLoop < NEIGHBOR_NUM / SUBNET_NUM; portLoop++) {
                String hostIp = Ipv4AddrUtil.longToIpv4(hostIPBase + loopCount + 1);
                String hostId = "host-" + (loopCount + 1);
                String portId = "port-" + (loopCount + 1);
                String macAddr = MacAddressUtil.formatAddress(longToBytes(macBase + loopCount + 1));
                String portIp = Ipv4AddrUtil.longToIpv4(subnetIPBase + portLoop + 2);

                neighborInfoList.add(new NeighborInfo(hostIp, hostId, portId, macAddr, portIp));
            }
        }

        for (int loop = 0; loop < count; loop++ ) {

            String macAddr = MacAddressUtil.formatAddress(longToBytes(macBase + NEIGHBOR_NUM + (loop + 1)));
            String portId = "port-" + (NEIGHBOR_NUM + (loop + 1));
            String subnetId = "subnet-" + (loop % 1000 + 1);
            String portIp = Ipv4AddrUtil.longToIpv4(vpcIpBase + (loop % SUBNET_NUM) * 4096
                    + NEIGHBOR_NUM / SUBNET_NUM +  (loop / SUBNET_NUM));
            List<PortEntity.FixedIp> fixedIps = new LinkedList<>();
            fixedIps.add(new PortEntity.FixedIp(subnetId, portIp));
            String bindingHostIP = Ipv4AddrUtil.longToIpv4(hostIPBase + (HOST_NUM / count) * loop + 1);

            PortEntity portEntity = new PortEntity();
            portEntity.setVpcId(vpcId);
            portEntity.setId(portId);
            portEntity.setFixedIps(fixedIps);
            portEntity.setMacAddress(macAddr);
            portEntity.setProjectId(projectId);
            portEntity.setBindingHostId(bindingHostIP);

            List<RouteEntity> routeEntities = new LinkedList<>();
            InternalPortEntity internalPortEntity = new InternalPortEntity(
                    portEntity, routeEntities, neighborInfoList, bindingHostIP);

            NetworkConfiguration networkConfiguration = new NetworkConfiguration();
            networkConfiguration.addPortEntity(internalPortEntity);
            networkConfiguration.addVpcEntity(vpcEntity);
            subnetInfoList.forEach(networkConfiguration::addSubnetEntity);

            System.out.print("networkConfiguration " + loop + ": \n" +
                    " portCount-" + networkConfiguration.getPortEntities().size() +
                    " vpcCount-" + networkConfiguration.getVpcs().size() +
                    " subnetCount-" + networkConfiguration.getSubnets().size() +
                    " neighborCountPerPort-" + networkConfiguration.getPortEntities().get(0).getNeighborInfos().size() +
                    "\n");

            dpmClient.createNetworkConfig(networkConfiguration);
        }

        return new ScaleTestResult(startTime, new Date().toString());
    }

}
