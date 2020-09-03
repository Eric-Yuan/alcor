package com.futurewei.alcor.fakeportmanager;

import com.futurewei.alcor.common.db.DbBaseConfiguration;
import com.futurewei.alcor.web.json.JsonHandlerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({JsonHandlerConfiguration.class})
public class FakePortManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FakePortManagerApplication.class, args);
    }

}
