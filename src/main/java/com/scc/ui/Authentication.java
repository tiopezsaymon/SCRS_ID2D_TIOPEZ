package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.user;
import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, Scanner sc){
        this.userDao = userDao;
        this.sc = sc;
    }


    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=======================");
            System.out.println("SCHOOL CLASSROOM RESERVATION SYSTEM");
            System.out.println("=======================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> handleLogin();
                case "2" -> handleRegister();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("-> [ERROR] Invalid option.");
            }
        }
    }

    private void handleRegister() {
        System.out.print("Enter new username: ");
        String username = sc.nextLine().trim();
        System.out.print("Enter password: ");
        String pass = sc.nextLine().trim();

        if (username.isEmpty() || pass.isEmpty()) {
            System.out.println("-> [ERROR] Username and password cannot be empty.");
            return;
        }

        System.out.println("Select role [1] STUDENT | [2] TEACHER | [3] CUSTODIAN | [4] ADMIN: ");
        String role;
        switch (sc.nextLine().trim()) {
            case "1" -> role = "STUDENT";
            case "2" -> role = "TEACHER";
            case "3" -> role = "CUSTODIAN";
            case "4" -> role = "ADMIN";
            default -> role = null;
        }

        if (role != null) {
            userDao.registerUser(username, pass, role);
        } else {
            System.out.println("Invalid role Selection!");
        }
    }

    private void handleLogin(){
        System.out.print("Enter username: ");
        String username = sc.nextLine();
        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        user user = userDao.login(username, password);

        if(user != null){
            System.out.println("\n -> Welcome "+ user.username()+" | ROLE : " +user.roles()+ "");
            switch(user.roles()){
                case "ADMIN" -> new AdminMenu(userDao, sc).display(user);
                default -> System.out.println("Account roles not found!");

            }
        }
    }



}