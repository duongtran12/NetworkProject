package server;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final Map<String, String> users = new HashMap<>();

    static {
        users.put("duong", "123");  
        users.put("admin", "admin");
    }

    public static boolean isValidUser(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }
}
