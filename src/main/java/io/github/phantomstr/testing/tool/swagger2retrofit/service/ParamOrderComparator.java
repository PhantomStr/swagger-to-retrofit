package io.github.phantomstr.testing.tool.swagger2retrofit.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

class ParamOrderComparator implements Comparator<String> {

    Map<String, Integer> order = new HashMap<>();

    {
        order.put("@Path", 1);
        order.put("@Body", 2);
        order.put("@Query", 3);
        order.put("@Header", 4);
    }

    @Override
    public int compare(String o1, String o2) {
        int o1Weight = order.entrySet().stream()
                .filter(s -> o1.startsWith(s.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(1000);
        int o2Weight = order.entrySet().stream()
                .filter(s -> o2.startsWith(s.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(1000);

        return Integer.compare(o1Weight, o2Weight);
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

}
