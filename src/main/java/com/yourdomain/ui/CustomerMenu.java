package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.Scanner;

public class CustomerMenu {
    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;

    public CustomerMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.user = user;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- Customer Menu ---");
            System.out.println("1. Select an available computer");
            System.out.println("2. Start a session");
            System.out.println("3. View remaining time");
            System.out.println("4. View session history");
            System.out.println("5. Logout");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            if (choice.equals("5")) return;
            if (choice.matches("[1-4]")) {
                System.out.println("TODO: connect your existing feature here.");
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}
