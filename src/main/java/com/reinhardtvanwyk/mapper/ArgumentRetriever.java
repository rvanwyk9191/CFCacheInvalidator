package com.reinhardtvanwyk.mapper;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import net.minidev.json.JSONArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ArgumentRetriever {

    private Logger logger = LogManager.getLogger(ArgumentRetriever.class);

    private final Map<String, Object> codePipelineJob;
    private String userParameters;

    public ArgumentRetriever(Map<String, Object> event) {
        codePipelineJob = (Map<String, Object>) event.get("CodePipeline.job");
        logger.info("Code Pipeline Job Arg: {}", codePipelineJob);
    }

    public String getId() {
        logger.info("Retrieving id");
        return JsonPath.read(codePipelineJob, "$.id");
    }

    public String getUserParameters() {
        logger.info("Retrieving user parameters");
        if (userParameters == null) {
            setUserParameters();
        }
        return this.userParameters;
    }

    private void setUserParameters() {
        String argumentPath = "$.data.actionConfiguration.configuration.UserParameters";
        userParameters = JsonPath.read(codePipelineJob, argumentPath);
    }

    public String getDistributionId() {
        return JsonPath.read(this.userParameters, "$.distribution");
    }

    public List<String> getItems() {
        if (userParameters == null) {
            setUserParameters();
        }
        Configuration configuration = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();
        JSONArray items = JsonPath.parse(this.userParameters, configuration).read("$.items");
        if (items == null) {
            return Arrays.asList("/*");
        }

        List<String> itemsForReturn = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            itemsForReturn.add((String) items.get(i));
        }
        return itemsForReturn;
    }

}
