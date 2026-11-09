package com.yourdomain;

import com.yourdomain.dao.DatabaseInitializer;
import com.yourdomain.dao.UserDao;
import com.yourdomain.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // 1. Initialize the database; stop safely if it fails.
        if (!DatabaseInitializer.initialize()) {
            System.out.println("Startup stopped: the database could not be initialized.");
            return;
        }

        // 2. One shared Scanner (closed automatically on exit).
        try (Scanner scanner = new Scanner(System.in)) {
            // 3. One shared UserDao.
            UserDao userDao = new UserDao();
            // 4-5. Create Authentication and start it.
            new Authentication(userDao, scanner).start();
        }
    }
}
