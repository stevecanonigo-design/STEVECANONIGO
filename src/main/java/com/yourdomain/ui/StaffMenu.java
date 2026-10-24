package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.Scanner;

public class StaffMenu {
    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;

    public StaffMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.user = user;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- Cafe Staff Menu ---");
            System.out.println("1. Start a customer session");
            System.out.println("2. End a customer session");
            System.out.println("3. View active and available computers");
            System.out.println("4. Calculate customer fee");
            System.out.println("5. Manage computer records");
            System.out.println("6. Logout");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            if (choice.equals("6")) return;
            if (choice.matches("[1-5]")) {
                System.out.println("TODO: connect your existing feature here.");
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}
