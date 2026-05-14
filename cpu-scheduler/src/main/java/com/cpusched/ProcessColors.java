package com.cpusched;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.paint.Color;


public class ProcessColors {

    public static final String IDLE_HEX = "#2a2a4a";

private static final String[] PALETTE = {
    "#8c6239",  // Deep Espresso
    "#b08968",  // Warm Caramel
    "#7f5539",  // Roasted Bean
    "#9d8189",  // Muted Mocha
    "#6b705c",  // Sage Latte (Earthiness)
    "#a5a58d",  // Steamed Milk Grey
    "#cb997e",  // Soft Terracotta
    "#ddbea9",  // Pale Latte
    "#b7b7a4",  // Frosted Foam
    "#6d597a"   // Berry Infusion (Muted Purple)
};


    public static Map<String, String> buildColorMap(List<GanttEntry> gantt) {
        Map<String, String> map = new HashMap<>();
        int idx = 0;
        for (GanttEntry e : gantt) {
            String pid = e.getProcessId();
            if (!map.containsKey(pid)) {
                map.put(pid, pid.equals("IDLE") ? IDLE_HEX : PALETTE[idx++ % PALETTE.length]);
            }
        }
        return map;
    }

    public static Map<String, String> buildColorMap(List<String> ids, boolean unused) {
        Map<String, String> map = new HashMap<>();
        int idx = 0;
        for (String id : ids) {
            if (!map.containsKey(id)) {
                map.put(id, id.equals("IDLE") ? IDLE_HEX : PALETTE[idx++ % PALETTE.length]);
            }
        }
        return map;
    }

    public static String hex(int index) {
        return PALETTE[index % PALETTE.length];
    }

    public static Color color(String hex) {
        return Color.web(hex);
    }
}
