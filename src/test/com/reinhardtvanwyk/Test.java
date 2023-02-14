package com.reinhardtvanwyk;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Test {

    public static void main(String[] args) throws IOException {
        Map<String, Object> userParameters = new HashMap<>();
        userParameters.put("items", Arrays.asList("item1", "item2"));
        String json = "{ }";
        //System.out.println(getItems(json));
    }

}
