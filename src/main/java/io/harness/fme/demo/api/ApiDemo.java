package io.harness.fme.demo.api; 

import org.springframework.web.bind.annotation.*;

import io.harness.fme.demo.flags.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;
import io.split.client.api.SplitResult;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class ApiDemo {

    FeatureFlagService featureFlagService;

    public ApiDemo(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
        this.featureFlagService.notifyOnUpdate(new ITreatmentUpdate() {
            @Override
            public void update(String flagName, String previous, String next) {
                System.out.println("updated flag " + flagName + " (" + previous + " -> " + next + ")");
                updateApi("placeholder");
            }
        }, "placeholder", "api");
    }


    @GetMapping("/flag/{key}/{flagName}")
    public String getFeatureTreatment(@PathVariable String key, @PathVariable String flagName) {
        return featureFlagService.getTreatment(key, flagName);
    }

    @GetMapping("/image/{key}")
    public Map<String, String> getImage(@PathVariable String key) {
        return updateApi(key);	
    }

    @GetMapping
    public DisplayConfig getConfig() {
        System.out.println("getConfig()");
        return new DisplayConfig("this is test json", "https://www.google.com"); 
    }

    public Map<String, String> updateApi(String key) {
        Map<String, Object> userAttributes = new HashMap<String, Object>();
        userAttributes.put("region", "NA");

        SplitResult splitResult = featureFlagService.splitClient.getTreatmentWithConfig(key, "api", userAttributes);
        JSONObject jsonObj = new JSONObject(splitResult.config());
        String url = jsonObj.getString("url");

        Map<String, String> response = new TreeMap<>();
        response.put("url", url);
        return response;        
    }
}

