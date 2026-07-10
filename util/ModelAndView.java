package util;

import java.util.HashMap;

public class ModelAndView {

    String view;
    HashMap<String, Object> model = new HashMap<>();

    public void setAttribute(String key, Object value) {
        model.put(key, value);
    }

    public void setView(String v) {
        this.view = v;
    }

}
