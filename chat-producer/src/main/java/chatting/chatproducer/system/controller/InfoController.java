package chatting.chatproducer.system.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    @Value("${instance.id}")
    private String instanceId;

    @GetMapping("/instance-id")
    public String getInstanceId() {
        return instanceId;
    }
}

