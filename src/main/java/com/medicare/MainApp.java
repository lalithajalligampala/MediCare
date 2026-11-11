package com.medicare;

import com.medicare.model.Medicine;
import com.medicare.model.HealthRecord;
import com.medicare.model.MedicationLog;
import com.medicare.model.UserProfile;

import com.medicare.storage.FileStorage;
import com.medicare.storage.HealthRecordStorage;
import com.medicare.storage.MedicationLogStorage;
import com.medicare.storage.UserProfileStorage;
import com.medicare.service.ReminderService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainApp extends Application {

    private Stage primaryStage;
    private VBox contentArea;
    private String loggedInUsername = "";

    private final ReminderService reminderService =
            new ReminderService();

    private final ObservableList<Medicine> medicineList =
            FXCollections.observableArrayList();

    private static final Path USERS_FILE =
            Path.of("data", "users.txt");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    // Modern Teal & Mint Dashboard Color Palette
    private final String primaryTeal = "#0D9488";
    private final String darkSidebar = "#0F766E";
    private final String background = "#F0FDF4";

    // Medication status colours
    private final String takenColor = "#7fd886";
    private final String missedColor = "#88c0ca";

    // =====================================================
    // APPLICATION START
    // =====================================================

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("MediCare - Smart Personal Health Assistant");
        primaryStage.setMinWidth(950);
        primaryStage.setMinHeight(650);

        showLoginScreen();
        primaryStage.show();

        primaryStage.setOnCloseRequest(e ->
                reminderService.stop()
        );
    }

    // =====================================================
    // APPLY CSS THEME
    // =====================================================

    private void applyTheme(Scene scene) {
        var css = getClass().getResource(
                "/com/medicare/medicare.css"
        );

        if (css != null) {
            scene.getStylesheets().add(
                    css.toExternalForm()
            );
        }
    }

    // =====================================================
    // LOGIN SCREEN
    // =====================================================

    private void showLoginScreen() {
        Label brand = new Label("MediCare");
        brand.setStyle("-fx-font-size: 42px; -fx-font-weight: 900; -fx-text-fill: white;");
        Label tagline = new Label("SMART PERSONAL HEALTH ASSISTANT");
        tagline.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #CCFBF1;");
        Label hero = new Label("Your health.\nYour care.\nAll in one place.");
        hero.setWrapText(true);
        hero.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label details = new Label("Manage medicines, track your health progress and get clear, helpful health information with MediCare.");
        details.setWrapText(true);
        details.setMaxWidth(380);
        details.setStyle("-fx-font-size: 15px; -fx-text-fill: #E6FFFB; -fx-line-spacing: 5px;");
        Label f1 = new Label("✓  Smart medicine reminders");
        Label f2 = new Label("✓  Personal health reports");
        Label f3 = new Label("✓  AI health information assistant");
        for (Label l : List.of(f1, f2, f3)) l.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        VBox left = new VBox(22, brand, tagline, new Region(), hero, details, f1, f2, f3);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(48));
        left.setPrefWidth(510);
        left.setMinWidth(0);
        // Rounded hero panel with a compact outer margin.
        left.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #115E59, #0D9488, #2DD4BF);" +
                "-fx-background-radius: 24px;" +
                "-fx-border-radius: 24px;"
        );

        Label title = new Label("Welcome back");
        title.setStyle("-fx-font-size: 29px; -fx-font-weight: bold; -fx-text-fill: #134E4A;");
        Label sub = new Label("Sign in to continue to your health dashboard");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        styleField(usernameField);
        styleField(passwordField);
        usernameField.setPrefHeight(46);
        passwordField.setPrefHeight(46);

        Label userLabel = new Label("USERNAME");
        Label passLabel = new Label("PASSWORD");
        for (Label l : List.of(userLabel, passLabel)) l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        Button loginButton = new Button("Sign in to MediCare");
        Button signupButton = new Button("Create a new account");
        stylePrimaryButton(loginButton);
        styleSecondaryButton(signupButton);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        signupButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(46);
        signupButton.setPrefHeight(44);
        Label message = new Label();
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #DC2626;");

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                message.setText("Please enter your username and password.");
                return;
            }
            try {
                if (authenticate(username, password)) {
                    loggedInUsername = username;
                    showMainDashboard();
                } else {
                    message.setText("Incorrect username or password. Please try again.");
                }
            } catch (IOException ex) {
                message.setText("Unable to access account data.");
            }
        });
        passwordField.setOnAction(e -> loginButton.fire());
        signupButton.setOnAction(e -> showSignupScreen());

        VBox form = new VBox(13, title, sub, new Region(), userLabel, usernameField,
                passLabel, passwordField, loginButton, signupButton, message);
        form.setPadding(new Insets(38));
        form.setMaxWidth(430);
        // A clean rounded form card: subtle border and soft shadow, without
        // bringing back the oversized empty frame from the earlier design.
        form.setStyle("-fx-background-color: transparent;");

        StackPane formPanel = new StackPane(form);
        formPanel.setAlignment(Pos.CENTER);
        formPanel.setPrefWidth(420);
        formPanel.setMinWidth(360);
        formPanel.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 24px;" +
                "-fx-border-color: #D5EEE7;" +
                "-fx-border-radius: 24px;" +
                "-fx-border-width: 1px;" +
                "-fx-effect: dropshadow(gaussian, rgba(15,118,110,0.12), 20, 0.10, 0, 5);"
        );

        // Keep the two rounded panels close together, with a modest outer
        // margin rather than a large universal border.
        HBox root = new HBox(16, left, formPanel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #ECFDF5, #CCFBF1);");
        HBox.setHgrow(left, Priority.ALWAYS);
        Scene scene = new Scene(root, 1180, 760);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    // =====================================================
    // SIGNUP SCREEN
    // =====================================================

    private void showSignupScreen() {
        Label title = new Label("Create Your Account");

        title.setStyle(
                "-fx-font-size: 27px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + primaryTeal + ";"
        );

        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Create a strong password");

        Label passwordHint = new Label(
                "Use 8+ characters with uppercase, lowercase, a number and a symbol."
        );
        passwordHint.setWrapText(true);
        passwordHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm password");

        styleField(usernameField);
        styleField(passwordField);
        styleField(confirmField);

        Button registerButton = new Button("Register");
        Button backButton = new Button("Back to Login");

        stylePrimaryButton(registerButton);
        styleSecondaryButton(backButton);

        registerButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setMaxWidth(Double.MAX_VALUE);

        Label message = new Label();
        message.setWrapText(true);

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();

            if (username.isEmpty() || password.isEmpty()
                    || confirm.isEmpty()) {
                message.setText(
                        "Please fill in all fields."
                );
                return;
            }

            if (username.length() < 3 ||
                    username.contains("|") ||
                    username.contains("\n") ||
                    username.contains("\r")) {
                message.setText(
                        "Username must have at least 3 characters and no special delimiter."
                );
                return;
            }

            String passwordError = validatePassword(password);
            if (passwordError != null) {
                message.setText(passwordError);
                return;
            }

            if (!password.equals(confirm)) {
                message.setText(
                        "Passwords do not match."
                );
                return;
            }

            try {
                if (usernameExists(username)) {
                    message.setText(
                            "Username already exists."
                    );
                    return;
                }

                Files.createDirectories(
                        USERS_FILE.getParent()
                );

                String account =
                        username + "|" + hashPassword(password)
                        + System.lineSeparator();

                Files.writeString(
                        USERS_FILE,
                        account,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );

                showAlert(
                        "Registration Successful",
                        "Account created. Please log in."
                );

                showLoginScreen();

            } catch (IOException ex) {
                message.setText(
                        "Registration failed: " + ex.getMessage()
                );
            }
        });

        backButton.setOnAction(e ->
                showLoginScreen()
        );

        VBox card = new VBox(
                17,
                title,
                usernameField,
                passwordField,
                passwordHint,
                confirmField,
                registerButton,
                backButton,
                message
        );

        card.setPadding(new Insets(35));
        card.setMaxWidth(400);
        card.setStyle(cardStyle());

        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F0FDF4, #CCFBF1, #99F6E4);"
        );

        Scene scene = new Scene(root, 1100, 700);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    // =====================================================
    // PASSWORD VALIDATION
    // =====================================================

    /**
     * Validates passwords for newly created accounts.
     * Existing accounts can still sign in with their original password.
     */
    private String validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return "Password must contain at least 8 characters.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must include at least one uppercase letter (A-Z).";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must include at least one lowercase letter (a-z).";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password must include at least one number (0-9).";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "Password must include at least one special character (for example, @, # or !).";
        }
        return null;
    }

    // =====================================================
    // AUTHENTICATION
    // =====================================================

    private boolean authenticate(
            String username,
            String password
    ) throws IOException {
        if (!Files.exists(USERS_FILE)) {
            return false;
        }

        String passwordHash = hashPassword(password);

        for (String account : Files.readAllLines(USERS_FILE)) {
            String[] parts = account.split("\\|", 2);

            if (parts.length == 2
                    && parts[0].equalsIgnoreCase(username)
                    && parts[1].equals(passwordHash)) {
                return true;
            }
        }

        return false;
    }

    private boolean usernameExists(String username)
            throws IOException {
        if (!Files.exists(USERS_FILE)) {
            return false;
        }

        for (String account : Files.readAllLines(USERS_FILE)) {
            String[] parts = account.split("\\|", 2);

            if (parts.length == 2
                    && parts[0].equalsIgnoreCase(username)) {
                return true;
            }
        }

        return false;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(
                        String.format("%02x", b)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // =====================================================
    // MAIN DASHBOARD
    // =====================================================

    private void showMainDashboard() {
        BorderPane root = new BorderPane();

        Label logo = new Label("MediCare");

        logo.setStyle(
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;"
        );

        String profileInitial = (loggedInUsername == null
                || loggedInUsername.isBlank())
                ? "U"
                : loggedInUsername.substring(0, 1).toUpperCase(Locale.ROOT);

        Button profileAvatar = new Button(profileInitial);
        profileAvatar.setMinSize(42, 42);
        profileAvatar.setPrefSize(42, 42);
        profileAvatar.setMaxSize(42, 42);
        profileAvatar.setTooltip(new Tooltip("Open my profile"));
        profileAvatar.setStyle(
                "-fx-background-color: rgba(255,255,255,0.96);" +
                "-fx-text-fill: " + primaryTeal + ";" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 50%;" +
                "-fx-border-color: rgba(255,255,255,0.75);" +
                "-fx-border-radius: 50%;" +
                "-fx-border-width: 2px;" +
                "-fx-cursor: hand;"
        );
        profileAvatar.setOnAction(e -> showPage("Profile"));

        Button logoutButton = new Button("Logout");

        logoutButton.setStyle(
                "-fx-background-color: white;" +
                "-fx-text-fill: " + primaryTeal + ";" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        );

        logoutButton.setOnAction(e -> {
            reminderService.stop();
            loggedInUsername = "";
            showLoginScreen();
        });

        Region headerSpacer = new Region();

        HBox.setHgrow(
                headerSpacer,
                Priority.ALWAYS
        );

        HBox header = new HBox(
                18,
                logo,
                headerSpacer,
                profileAvatar,
                logoutButton
        );

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(
                new Insets(15, 25, 15, 25)
        );

        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #0F766E, #14B8A6, #2DD4BF);"
        );

        root.setTop(header);

        VBox sidebar = new VBox(12);

        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);

        sidebar.setStyle(
                "-fx-background-color: " + darkSidebar + ";" +
                "-fx-border-color: transparent rgba(255,255,255,0.1) transparent transparent;" +
                "-fx-border-width: 0 1px 0 0;"
        );

        Label menuTitle = new Label("MAIN MENU");

        menuTitle.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #99F6E4;"
        );

        Button dashboardButton = createButton("Dashboard");
        Button profileButton = createButton("My Profile");
        Button medicineButton = createButton("Medicines");
        Button reminderButton = createButton("Reminders");
        Button healthButton = createButton("Health Records");
        Button trackingButton = createButton("Medication Tracking");
        Button reportsButton = createButton("Reports");
        Button aiButton = createButton("MediCare AI");

        dashboardButton.setOnAction(e ->
                showDashboardPage()
        );

        profileButton.setOnAction(e ->
                showPage("Profile")
        );

        medicineButton.setOnAction(e ->
                showPage("Medicines")
        );

        reminderButton.setOnAction(e ->
                showPage("Reminders")
        );

        healthButton.setOnAction(e ->
                showPage("Health Records")
        );

        trackingButton.setOnAction(e ->
                showPage("Medication Tracking")
        );

        reportsButton.setOnAction(e ->
                showPage("Reports")
        );

        aiButton.setOnAction(e -> showAIAssistantPage());

        sidebar.getChildren().addAll(
                menuTitle,
                dashboardButton,
                profileButton,
                medicineButton,
                reminderButton,
                healthButton,
                trackingButton,
                reportsButton,
                aiButton
        );

        root.setLeft(sidebar);

        contentArea = new VBox(20);

        contentArea.setPadding(new Insets(25));

        contentArea.setStyle(
                "-fx-background-color: " + background + ";"
        );

        ScrollPane scrollPane = new ScrollPane(contentArea);

        scrollPane.setFitToWidth(true);

        scrollPane.setStyle(
                "-fx-background: " + background + ";"
        );

        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 1200, 750);

        applyTheme(scene);

        primaryStage.setScene(scene);

        reminderService.start();

        showDashboardPage();
    }

    // =====================================================
    // DASHBOARD
    // =====================================================

    private void showDashboardPage() {
        contentArea.getChildren().clear();

        contentArea.getChildren().add(
                pageTitle("Dashboard")
        );

        try {
            List<Medicine> medicines =
                    FileStorage.loadMedicines();

            List<HealthRecord> records =
                    HealthRecordStorage.loadRecords();

            List<MedicationLog> logs =
                    MedicationLogStorage.loadLogs();

            LocalDate today = LocalDate.now();

            long activeMedicines = medicines.stream()
                    .filter(m ->
                            !today.isBefore(m.getStartDate())
                                    && !today.isAfter(m.getEndDate())
                    )
                    .count();

            long todayTaken = logs.stream()
                    .filter(log ->
                            log.getDate().equals(today)
                                    && "Taken".equalsIgnoreCase(
                                    log.getStatus()
                            )
                    )
                    .count();

            long todayMissed = logs.stream()
                    .filter(log ->
                            log.getDate().equals(today)
                                    && "Missed".equalsIgnoreCase(
                                    log.getStatus()
                            )
                    )
                    .count();

            HBox cards = new HBox(
                    15,
                    makeCard(
                            "Total Medicines",
                            String.valueOf(medicines.size())
                    ),
                    makeCard(
                            "Active Medicines",
                            String.valueOf(activeMedicines)
                    ),
                    makeCard(
                            "Health Records",
                            String.valueOf(records.size())
                    ),
                    makeCard(
                            "Taken Today",
                            String.valueOf(todayTaken)
                    ),
                    makeCard(
                            "Missed Today",
                            String.valueOf(todayMissed)
                    )
            );

            cards.setSpacing(15);

            contentArea.getChildren().add(cards);

            LocalDate weekStart =
                    today.with(DayOfWeek.MONDAY);

            LocalDate weekEnd =
                    weekStart.plusDays(6);

            BarChart<String, Number> dailyChart =
                    createMedicationReportChart(
                            logs,
                            today,
                            today,
                            false
                    );

            VBox dailyCard = new VBox(
                    12,
                    dailyChart
            );

            dailyCard.setPadding(new Insets(18));
            dailyCard.setStyle(cardStyle());

            Label dailyTitle =
                    new Label("Daily Medication Report");

            dailyTitle.setStyle(
                    "-fx-font-size: 17px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #0F766E;"
            );

            contentArea.getChildren().addAll(
                    dailyTitle,
                    dailyCard
            );

            BarChart<String, Number> weeklyChart =
                    createMedicationReportChart(
                            logs,
                            weekStart,
                            weekEnd,
                            true
                    );

            VBox weeklyCard = new VBox(
                    12,
                    weeklyChart
            );

            weeklyCard.setPadding(new Insets(18));
            weeklyCard.setStyle(cardStyle());

            Label weeklyTitle =
                    new Label("Weekly Medication Report");

            weeklyTitle.setStyle(
                    "-fx-font-size: 17px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #0F766E;"
            );

            contentArea.getChildren().addAll(
                    weeklyTitle,
                    weeklyCard
            );

            Label note = new Label(
                    "Note: Medication graphs are based on entries saved in Medication Tracking."
            );

            note.setWrapText(true);

            note.setStyle(
                    "-fx-text-fill: #64748B;"
            );

            contentArea.getChildren().add(note);

        } catch (IOException ex) {
            showAlert(
                    "Dashboard Error",
                    ex.getMessage()
            );
        }
    }

    // =====================================================
    // MEDICARE AI ASSISTANT - GOOGLE GEMINI
    // =====================================================

    private void showAIAssistantPage() {
        Label heading = pageTitle("MediCare AI Assistant");
        Label intro = new Label("Ask general health questions in English or Telugu. MediCare AI can explain information in simple language.");
        intro.setWrapText(true);
        intro.setStyle("-fx-text-fill: #64748B; -fx-font-size: 14px;");

        Label disclaimer = new Label("Health information only. This assistant cannot diagnose conditions or replace a qualified healthcare professional.");
        disclaimer.setWrapText(true);
        disclaimer.setStyle("-fx-background-color: #FFF7ED; -fx-text-fill: #9A3412; -fx-padding: 11px; -fx-background-radius: 10px;");

        VBox messages = new VBox(12);
        messages.setPadding(new Insets(18));
        messages.setStyle("-fx-background-color: #F8FFFC;");
        ScrollPane scroll = new ScrollPane(messages);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setStyle("-fx-background: #F8FFFC; -fx-background-color: #F8FFFC; -fx-border-color: #D5EEE7; -fx-border-radius: 14px;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        addAIBubble(messages, "Hello " + loggedInUsername + "! I'm MediCare AI. Ask me a general health question in English or Telugu.", false);

        TextArea prompt = new TextArea();
        prompt.setPromptText("Type your question... / మీ ప్రశ్నను టైప్ చేయండి...");
        prompt.setWrapText(true);
        prompt.setPrefRowCount(2);
        prompt.setMaxHeight(85);
        styleField(prompt);

        Button send = new Button("Send");
        Button clear = new Button("Clear chat");
        stylePrimaryButton(send);
        styleSecondaryButton(clear);
        Label status = new Label("Ready");
        status.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        Runnable sendQuestion = () -> {
            String question = prompt.getText().trim();
            if (question.isEmpty()) {
                status.setText("Type a question first.");
                return;
            }
            prompt.clear();
            prompt.setDisable(true);
            send.setDisable(true);
            status.setText("MediCare AI is thinking...");
            addAIBubble(messages, question, true);

            Thread task = new Thread(() -> {
                try {
                    // Medication-status questions are answered from the local MediCare
                    // files. Personal medication records are not sent to Gemini.
                    String reply = answerMedicationQuestionFromLocalRecords(question);

                    if (reply == null) {
                        String apiKey = System.getenv("GEMINI_API_KEY");
                        if (apiKey == null || apiKey.isBlank()) {
                            throw new IOException("Gemini API key is not configured. Set GEMINI_API_KEY and restart MediCare.");
                        }
                        reply = requestGemini(apiKey, question);
                    }

                    final String finalReply = reply;
                    Platform.runLater(() -> {
                        addAIBubble(messages, finalReply, false);
                        status.setText("Response received");
                        prompt.setDisable(false);
                        send.setDisable(false);
                        scroll.setVvalue(1.0);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        addAIBubble(messages, "I couldn't complete that request. " + safeAIError(ex), false);
                        status.setText("Request failed");
                        prompt.setDisable(false);
                        send.setDisable(false);
                    });
                }
            }, "medicare-ai");
            task.setDaemon(true);
            task.start();
        };

        send.setOnAction(e -> sendQuestion.run());
        prompt.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                sendQuestion.run();
            }
        });
        clear.setOnAction(e -> {
            messages.getChildren().clear();
            addAIBubble(messages, "Chat cleared. How can I help you?", false);
            status.setText("Ready");
        });

        HBox controls = new HBox(12, clear, new Region(), status, send);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(controls.getChildren().get(1), Priority.ALWAYS);
        VBox panel = new VBox(13, disclaimer, scroll, prompt, controls);
        panel.setPadding(new Insets(20));
        panel.setStyle(cardStyle());
        contentArea.getChildren().setAll(heading, intro, panel);
    }

    private void addAIBubble(VBox messages, String text, boolean fromUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(650);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (fromUser ? "white" : "#23434A") + ";");
        VBox bubble = new VBox(label);
        bubble.setMaxWidth(700);
        bubble.setPadding(new Insets(12, 16, 12, 16));
        bubble.setStyle("-fx-background-color: " + (fromUser ? "#0D9488" : "#E8F7F2") + "; -fx-background-radius: 14px;");
        HBox row = new HBox(bubble);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messages.getChildren().add(row);
    }

    /**
     * Answers medication-status questions from the local medicine and history files.
     * Returns null for general health questions so those can be sent to Gemini.
     * The current MedicationLog model stores a date-level status, not a separate
     * status for every reminder time; the response explains that limitation.
     */
    private String answerMedicationQuestionFromLocalRecords(String question)
            throws IOException {

        if (!isMedicationStatusQuestion(question)) {
            return null;
        }

        LocalDate today = LocalDate.now();
        List<Medicine> medicines = FileStorage.loadMedicines();
        List<MedicationLog> logs = MedicationLogStorage.loadLogs();
        List<MedicationLog> todaysLogs = new ArrayList<>();

        for (MedicationLog log : logs) {
            if (today.equals(log.getDate())) {
                todaysLogs.add(log);
            }
        }

        boolean telugu = containsTelugu(question);
        StringBuilder answer = new StringBuilder();

        if (telugu) {
            answer.append("ఈరోజు (").append(today).append(") మీ MediCare మందుల రికార్డులు:\n\n");
        } else {
            answer.append("Your MediCare medication records for today (")
                    .append(today).append("):\n\n");
        }

        int activeCount = 0;
        for (Medicine medicine : medicines) {
            LocalDate start = medicine.getStartDate();
            LocalDate end = medicine.getEndDate();
            boolean activeToday = (start == null || !today.isBefore(start))
                    && (end == null || !today.isAfter(end));

            if (!activeToday) {
                continue;
            }

            activeCount++;
            answer.append("• ").append(medicine.getName())
                    .append(" — ").append(medicine.getDosage()).append('\n');

            List<String> reminderTimes = medicine.getReminderTimes();
            if (!reminderTimes.isEmpty()) {
                answer.append(telugu ? "  రిమైండర్ సమయాలు: " : "  Reminder times: ")
                        .append(String.join(", ", reminderTimes)).append('\n');
            }

            List<String> recordedStatuses = new ArrayList<>();
            for (MedicationLog log : todaysLogs) {
                if (sameText(medicine.getName(), log.getMedicineName())
                        && sameText(medicine.getDosage(), log.getDosage())) {
                    String statusValue = log.getStatus() == null
                            ? (telugu ? "తెలియని స్థితి" : "Unknown status")
                            : log.getStatus();
                    String displayStatus = statusValue;
                    if (telugu && "Taken".equalsIgnoreCase(statusValue)) {
                        displayStatus = "తీసుకున్నట్లు నమోదు";
                    } else if (telugu && "Missed".equalsIgnoreCase(statusValue)) {
                        displayStatus = "మిస్ అయినట్లు నమోదు";
                    } else if (!telugu && "Taken".equalsIgnoreCase(statusValue)) {
                        displayStatus = "Taken (recorded)";
                    } else if (!telugu && "Missed".equalsIgnoreCase(statusValue)) {
                        displayStatus = "Missed (recorded)";
                    }
                    if (!recordedStatuses.contains(displayStatus)) {
                        recordedStatuses.add(displayStatus);
                    }
                }
            }

            if (recordedStatuses.isEmpty()) {
                answer.append(telugu
                        ? "  ఈరోజు స్థితి: ఇంకా నమోదు కాలేదు. ఇది మందు తీసుకోలేదని నిర్ధారించదు.\n"
                        : "  Today's status: Not recorded. This does not prove the medicine was not taken.\n");
            } else {
                answer.append(telugu ? "  ఈరోజు నమోదు: " : "  Today's recorded status: ")
                        .append(String.join(", ", recordedStatuses)).append('\n');
            }
            answer.append('\n');
        }

        // Include today's log entries even if the medicine is no longer active
        // or its name/dosage differs from the current medicine record.
        List<MedicationLog> additionalLogs = new ArrayList<>();
        for (MedicationLog log : todaysLogs) {
            boolean matchedActiveMedicine = false;
            for (Medicine medicine : medicines) {
                LocalDate start = medicine.getStartDate();
                LocalDate end = medicine.getEndDate();
                boolean activeToday = (start == null || !today.isBefore(start))
                        && (end == null || !today.isAfter(end));
                if (activeToday
                        && sameText(medicine.getName(), log.getMedicineName())
                        && sameText(medicine.getDosage(), log.getDosage())) {
                    matchedActiveMedicine = true;
                    break;
                }
            }
            if (!matchedActiveMedicine) {
                additionalLogs.add(log);
            }
        }

        if (!additionalLogs.isEmpty()) {
            answer.append(telugu ? "ఈరోజు చరిత్రలోని ఇతర నమోదు:\n" : "Other entries recorded today:\n");
            for (MedicationLog log : additionalLogs) {
                answer.append("• ").append(log.getMedicineName())
                        .append(" — ").append(log.getDosage())
                        .append(" — ").append(log.getStatus()).append('\n');
            }
            answer.append('\n');
        }

        if (activeCount == 0 && todaysLogs.isEmpty()) {
            answer.append(telugu
                    ? "ఈరోజుకు సంబంధించిన క్రియాశీల మందుల వివరాలు లేదా తీసుకున్న/మిస్ అయిన నమోదు కనిపించలేదు.\n"
                    : "No active medicine records or taken/missed entries were found for today.\n");
        }

        answer.append(telugu
                ? "\nగమనిక: మీ ప్రస్తుత ట్రాకింగ్ రికార్డు తేదీ ఆధారంగా స్థితిని మాత్రమే భద్రపరుస్తుంది; ప్రతి రిమైండర్ సమయానికి విడిగా స్థితి నమోదు చేయదు. నమోదు లేనిది ‘తీసుకోలేదు’ అని అర్థం కాదు. మందుల మోతాదును మార్చే ముందు వైద్యుడిని సంప్రదించండి."
                : "\nNote: your current tracker stores a date-level status, not a separate status for each reminder time. No entry means ‘not recorded’, not necessarily ‘not taken’. Do not change a prescribed dose based on this summary.");

        return answer.toString();
    }

    private boolean isMedicationStatusQuestion(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        boolean mentionsMedicine = q.contains("medicine") || q.contains("medicines")
                || q.contains("medication") || q.contains("medications")
                || q.contains("tablet") || q.contains("tablets")
                || q.contains("మందు") || q.contains("మందులు");
        boolean asksAboutStatus = q.contains("today") || q.contains("took")
                || q.contains("take") || q.contains("taken") || q.contains("missed")
                || q.contains("pending") || q.contains("status") || q.contains("history")
                || q.contains("schedule") || q.contains("reminder")
                || q.contains("ఈరోజు") || q.contains("తీసుకున్న")
                || q.contains("మిస్") || q.contains("పెండింగ్") || q.contains("షెడ్యూల్");
        return mentionsMedicine && asksAboutStatus;
    }

    private boolean containsTelugu(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) >= '\u0C00' && value.charAt(i) <= '\u0C7F') {
                return true;
            }
        }
        return false;
    }

    private boolean sameText(String first, String second) {
        return first != null && second != null
                && first.trim().equalsIgnoreCase(second.trim());
    }

    private String requestGemini(String apiKey, String question) throws Exception {
        String instructions =
                "You are MediCare AI, a careful health information assistant. " +
                "LANGUAGE RULE (must follow): First identify the language of the user's latest question. " +
                "If the user writes in English, answer entirely in clear, natural English. " +
                "If the user writes in Telugu, answer entirely in natural Telugu. " +
                "If the user mixes English and Telugu, answer in the language used most; if balanced, " +
                "a concise bilingual answer is acceptable. Never default to Telugu. Never translate an " +
                "English question into Telugu unless the user asks for translation. " +
                "Keep medicine names, units and medical terms clear. " +
                "Explain general health concepts in simple language. Do not diagnose, prescribe, or advise " +
                "changing prescribed medicines. Encourage a qualified clinician for personal medical decisions. " +
                "For severe chest pain, difficulty breathing, stroke symptoms or loss of consciousness, " +
                "advise seeking emergency medical help immediately. Do not claim to have access to health " +
                "records unless they are explicitly provided in the question. This is not a substitute for professional care.";
        String body = "{\"contents\":[{\"parts\":[{\"text\":\""
                + escapeJson(instructions + "\n\nIMPORTANT: Respond in the same language as this user's question.\nUser question: " + question)
                + "\"}]}],\"generationConfig\":{\"maxOutputTokens\":900}}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        // Retry temporary server overloads and rate limits. This method is called
        // from the AI worker thread, so waiting here will not freeze the JavaFX UI.
        HttpResponse<String> response = null;
        int[] retryableStatuses = {408, 429, 500, 502, 503, 504};
        for (int attempt = 1; attempt <= 3; attempt++) {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                break;
            }

            boolean retryable = false;
            for (int retryableStatus : retryableStatuses) {
                if (status == retryableStatus) {
                    retryable = true;
                    break;
                }
            }

            if (!retryable || attempt == 3) {
                throw new IOException("Gemini HTTP " + status + ": " + response.body());
            }

            Thread.sleep(attempt * 3000L); // wait 3 seconds, then 6 seconds
        }

        if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini request failed after retries.");
        }

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(response.body());
        if (!m.find()) {
            throw new IOException("Gemini response did not contain answer text: " + response.body());
        }
        return unescapeJson(m.group(1));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }
            char n = value.charAt(++i);
            switch (n) {
                case 'n': out.append('\n'); break;
                case 'r': out.append('\r'); break;
                case 't': out.append('\t'); break;
                case '"': out.append('"'); break;
                case '\\': out.append('\\'); break;
                case '/': out.append('/'); break;
                default: out.append(n);
            }
        }
        return out.toString();
    }

    private String safeAIError(Exception ex) {
        String message = ex.getMessage();
        if (message == null) return ex.getClass().getSimpleName();
        return message.length() > 160 ? message.substring(0, 160) : message;
    }

    // =====================================================
    // COMMON STYLES
    // =====================================================

    private String cardStyle() {
        return "-fx-background-color: white;" +
                "-fx-background-radius: 18px;" +
                "-fx-border-color: #DCEFEA;" +
                "-fx-border-radius: 18px;" +
                "-fx-border-width: 1px;" +
                "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.10), 18, 0.1, 0, 5);";
    }

    private void styleField(Control field) {
        field.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-color: #CBD5E1;" +
                "-fx-border-radius: 12px;" +
                "-fx-border-width: 1px;" +
                "-fx-padding: 10px;" +
                "-fx-font-size: 13px;"
        );
    }

    /** Applies the shared rounded, readable table appearance used by the medicine pages. */
    private <T> void styleDataTable(TableView<T> table, double height) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(42);
        table.setPrefHeight(height);
        table.setMinHeight(190);
        table.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 14px;" +
                "-fx-border-color: #D7EAE5;" +
                "-fx-border-radius: 14px;" +
                "-fx-border-width: 1px;" +
                "-fx-padding: 3px;"
        );
        table.setPlaceholder(new Label("No records to display yet."));
        table.setRowFactory(view -> new TableRow<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setStyle("");
                } else {
                    setStyle(isSelected()
                            ? "-fx-background-color: #CCFBF1;"
                            : (getIndex() % 2 == 0
                                ? "-fx-background-color: #FFFFFF;"
                                : "-fx-background-color: #F8FCFB;"));
                }
            }
        });
    }

    private Label sectionHeading(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #115E59;"
        );
        return label;
    }

    private Label helperText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        return label;
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: " + primaryTeal + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 11px 22px;" +
                "-fx-background-radius: 12px;" +
                "-fx-cursor: hand;"
        );
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: #E2E8F0;" +
                "-fx-text-fill: #334155;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 11px 22px;" +
                "-fx-background-radius: 12px;" +
                "-fx-cursor: hand;"
        );
    }

    private Button createButton(String name) {
        Button button = new Button(name);

        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(46);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #E2E8F0;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 0 15px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 0 15px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #E2E8F0;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 0 15px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;"
        ));

        return button;
    }

    private Label pageTitle(String text) {
        Label label = new Label(text);

        label.setStyle(
                "-fx-font-size: 27px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #0F766E;"
        );

        return label;
    }

    private Label pageSubtitle(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #64748B;"
        );
        return label;
    }

    private VBox makeCard(String heading, String value) {
        Label title = new Label(heading);

        title.setStyle(
                "-fx-text-fill: #64748B;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        Label number = new Label(value);

        number.setStyle(
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + primaryTeal + ";"
        );

        VBox card = new VBox(
                12,
                title,
                number
        );

        card.setPadding(new Insets(20));
        card.setMinWidth(150);
        card.setPrefWidth(190);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(cardStyle());

        HBox.setHgrow(
                card,
                Priority.ALWAYS
        );

        card.setOnMouseEntered(e -> {
            card.setTranslateY(-3);

            card.setStyle(
                    "-fx-background-color: #F0FDFA;" +
                    "-fx-background-radius: 16px;" +
                    "-fx-border-color: #5EEAD4;" +
                    "-fx-border-radius: 16px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.15), 18, 0.1, 0, 5);"
            );
        });

        card.setOnMouseExited(e -> {
            card.setTranslateY(0);

            card.setStyle(cardStyle());
        });

        return card;
    }

    // =====================================================
    // PAGE NAVIGATION
    // =====================================================

    private void showPage(String page) {
        contentArea.getChildren().clear();

        switch (page) {
            case "Profile":
                showProfilePage();
                break;
            case "Medicines":
                showMedicinePage();
                break;
            case "Reminders":
                showReminderPage();
                break;
            case "Health Records":
                showHealthRecordsPage();
                break;
            case "Reports":
                showReportsPage();
                break;
            case "Medication Tracking":
                showMedicationTrackingPage();
                break;
            default:
                showDashboardPage();
        }
    }

    // =====================================================
    // DAILY AND WEEKLY MEDICATION BAR CHART
    // =====================================================

    private BarChart<String, Number> createMedicationReportChart(
            List<MedicationLog> logs,
            LocalDate startDate,
            LocalDate endDate,
            boolean weekly) {

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(weekly ? "Day of Week" : "Date");
        xAxis.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Medication Records");
        yAxis.setLowerBound(0);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        BarChart<String, Number> chart =
                new BarChart<>(xAxis, yAxis);

        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setCategoryGap(28);
        chart.setBarGap(6);
        chart.setPrefHeight(320);
        chart.setMinHeight(280);

        XYChart.Series<String, Number> takenSeries =
                new XYChart.Series<>();
        takenSeries.setName("Taken (Completed)");

        XYChart.Series<String, Number> missedSeries =
                new XYChart.Series<>();
        missedSeries.setName("Missed (Pending)");

        if (weekly) {
            for (int i = 0; i < 7; i++) {
                LocalDate currentDate =
                        startDate.plusDays(i);

                String dayName =
                        currentDate.getDayOfWeek()
                                .getDisplayName(
                                        java.time.format.TextStyle.SHORT,
                                        Locale.ENGLISH
                                );

                long takenCount = logs.stream()
                        .filter(log ->
                                log.getDate().equals(currentDate)
                                        && "Taken".equalsIgnoreCase(
                                        log.getStatus()
                                )
                        )
                        .count();

                long missedCount = logs.stream()
                        .filter(log ->
                                log.getDate().equals(currentDate)
                                        && "Missed".equalsIgnoreCase(
                                        log.getStatus()
                                )
                        )
                        .count();

                takenSeries.getData().add(
                        new XYChart.Data<>(
                                dayName,
                                takenCount
                        )
                );

                missedSeries.getData().add(
                        new XYChart.Data<>(
                                dayName,
                                missedCount
                        )
                );
            }
        } else {
            long takenCount = logs.stream()
                    .filter(log ->
                            !log.getDate().isBefore(startDate)
                                    && !log.getDate().isAfter(endDate)
                                    && "Taken".equalsIgnoreCase(
                                    log.getStatus()
                            )
                    )
                    .count();

            long missedCount = logs.stream()
                    .filter(log ->
                            !log.getDate().isBefore(startDate)
                                    && !log.getDate().isAfter(endDate)
                                    && "Missed".equalsIgnoreCase(
                                    log.getStatus()
                            )
                    )
                    .count();

            takenSeries.getData().add(
                    new XYChart.Data<>(
                            "Today",
                            takenCount
                    )
            );

            missedSeries.getData().add(
                    new XYChart.Data<>(
                            "Today",
                            missedCount
                    )
            );
        }

        chart.getData().addAll(
                takenSeries,
                missedSeries
        );

        // Taken (Completed) - Green
        // Missed (Pending) - Blue
        javafx.application.Platform.runLater(() -> {
            // Ensure chart bars and legend symbols have been created.
            chart.applyCss();
            chart.layout();

            for (XYChart.Data<String, Number> data : takenSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle(
                            "-fx-bar-fill: " + takenColor + ";" +
                            "-fx-background-radius: 6px 6px 0 0;"
                    );
                }
            }

            for (XYChart.Data<String, Number> data : missedSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle(
                            "-fx-bar-fill: " + missedColor + ";" +
                            "-fx-background-radius: 6px 6px 0 0;"
                    );
                }
            }

            // Apply the same colours to the legend squares below the graph.
            for (javafx.scene.Node legendSymbol :
                    chart.lookupAll(".chart-legend-item-symbol")) {

                if (legendSymbol.getStyleClass().contains("default-color0")) {
                    legendSymbol.setStyle(
                            "-fx-background-color: " + takenColor + ";"
                    );
                } else if (legendSymbol.getStyleClass().contains("default-color1")) {
                    legendSymbol.setStyle(
                            "-fx-background-color: " + missedColor + ";"
                    );
                }
            }
        });

        return chart;
    }

    // =====================================================
    // MEDICINE MANAGEMENT
    // =====================================================

    private void showMedicinePage() {
        contentArea.getChildren().clear();

        contentArea.getChildren().addAll(
                pageTitle("Medicine Management"),
                pageSubtitle("Organise your treatment plan and set reminders for every dose.")
        );

        TextField nameField = new TextField();
        TextField dosageField = new TextField();

        nameField.setPromptText("Medicine name");
        dosageField.setPromptText(
                "Dosage details (e.g. 500 mg)"
        );

        styleField(nameField);
        styleField(dosageField);

        ComboBox<String> frequencyBox = new ComboBox<>();

        frequencyBox.getItems().addAll(
                "Once daily",
                "Twice daily",
                "Three times daily",
                "As prescribed"
        );

        frequencyBox.setPromptText("Select frequency");
        frequencyBox.setMaxWidth(Double.MAX_VALUE);
        frequencyBox.setPrefHeight(42);
        styleField(frequencyBox);

        VBox timingFieldsBox = new VBox(10);
        List<TextField> timingFields = new ArrayList<>();

        DatePicker startPicker =
                new DatePicker(LocalDate.now());

        DatePicker endPicker = new DatePicker();
        startPicker.setMaxWidth(Double.MAX_VALUE);
        endPicker.setMaxWidth(Double.MAX_VALUE);
        startPicker.setPrefHeight(42);
        endPicker.setPrefHeight(42);
        styleField(startPicker);
        styleField(endPicker);

        Medicine[] editingMedicine = {null};

        frequencyBox.setOnAction(e ->
                updateTimingFields(
                        timingFieldsBox,
                        timingFields,
                        frequencyBox.getValue(),
                        List.of()
                )
        );

        GridPane form = new GridPane();

        form.setHgap(22);
        form.setVgap(16);
        form.setPadding(new Insets(12, 10, 8, 10));

        addFormRow(
                form, 0, "Medicine Name:", nameField
        );

        addFormRow(
                form, 1, "Dosage:", dosageField
        );

        addFormRow(
                form, 2, "Frequency:", frequencyBox
        );

        form.add(
                new Label("Reminder Timings:"),
                0,
                3
        );

        form.add(
                timingFieldsBox,
                1,
                3
        );

        addFormRow(
                form, 4, "Start Date:", startPicker
        );

        addFormRow(
                form, 5, "End Date:", endPicker
        );

        Button saveButton =
                new Button("Save Medicine");

        Button cancelButton =
                new Button("Cancel Edit");

        stylePrimaryButton(saveButton);
        styleSecondaryButton(cancelButton);

        cancelButton.setVisible(false);
        cancelButton.setManaged(false);

        TableView<Medicine> table =
                new TableView<>();

        ObservableList<Medicine> items =
                FXCollections.observableArrayList();

        addMedicineColumn(
                table, "Medicine", Medicine::getName
        );

        addMedicineColumn(
                table, "Dosage", Medicine::getDosage
        );

        addMedicineColumn(
                table, "Frequency", Medicine::getFrequency
        );

        addMedicineColumn(
                table,
                "Reminder Timings",
                medicine -> String.join(
                        ", ",
                        medicine.getReminderTimes()
                )
        );

        addMedicineColumn(
                table,
                "Start Date",
                medicine -> medicine.getStartDate().toString()
        );

        addMedicineColumn(
                table,
                "End Date",
                medicine -> medicine.getEndDate().toString()
        );

        styleDataTable(table, 330);

        try {
            items.setAll(
                    FileStorage.loadMedicines()
            );
        } catch (IOException ex) {
            showAlert(
                    "Loading Error",
                    ex.getMessage()
            );
        }

        table.setItems(items);

        saveButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String dosage = dosageField.getText().trim();
            String frequency = frequencyBox.getValue();

            LocalDate start = startPicker.getValue();
            LocalDate end = endPicker.getValue();

            if (name.isEmpty() || dosage.isEmpty()
                    || frequency == null
                    || start == null || end == null) {
                showAlert(
                        "Validation Error",
                        "Fill in all medicine details."
                );
                return;
            }

            if (name.contains("|") || dosage.contains("|")
                    || name.contains("\n") || dosage.contains("\n")
                    || name.contains("\r") || dosage.contains("\r")) {
                showAlert(
                        "Invalid Input",
                        "Remove | and line breaks from the fields."
                );
                return;
            }

            if (end.isBefore(start)) {
                showAlert(
                        "Invalid Dates",
                        "End date cannot be before start date."
                );
                return;
            }

            List<String> reminderTimes =
                    new ArrayList<>();

            for (TextField field : timingFields) {
                String time = field.getText().trim();

                if (time.isEmpty()) {
                    showAlert(
                            "Timing Required",
                            "Enter all reminder timings."
                    );
                    return;
                }

                try {
                    LocalTime.parse(
                            time,
                            TIME_FORMAT
                    );
                } catch (Exception ex) {
                    showAlert(
                            "Invalid Time",
                            "Enter time in format: 08:00 AM."
                    );
                    return;
                }

                reminderTimes.add(time);
            }

            try {
                String excludeId =
                        editingMedicine[0] == null
                                ? null
                                : editingMedicine[0].getId();

                if (medicineAlreadyExists(
                        name,
                        dosage,
                        excludeId)) {
                    showAlert(
                            "Duplicate Medicine",
                            "This medicine with the same dosage is already added."
                    );
                    return;
                }

                if (editingMedicine[0] == null) {
                    Medicine medicine = new Medicine(
                            name,
                            dosage,
                            frequency,
                            reminderTimes,
                            start,
                            end
                    );

                    FileStorage.saveMedicine(medicine);

                    showAlert(
                            "Success",
                            "Medicine added successfully."
                    );
                } else {
                    Medicine medicine = new Medicine(
                            editingMedicine[0].getId(),
                            name,
                            dosage,
                            frequency,
                            reminderTimes,
                            start,
                            end
                    );

                    FileStorage.updateMedicine(medicine);

                    showAlert(
                            "Success",
                            "Medicine updated successfully."
                    );
                }

                showMedicinePage();

            } catch (IOException ex) {
                showAlert(
                        "Storage Error",
                        ex.getMessage()
                );
            }
        });

        Button editButton =
                new Button("Edit Selected");

        stylePrimaryButton(editButton);

        editButton.setOnAction(e -> {
            Medicine selected = table.getSelectionModel()
                    .getSelectedItem();

            if (selected == null) {
                showAlert(
                        "Selection Required",
                        "Select a medicine from the table first."
                );
                return;
            }

            editingMedicine[0] = selected;

            nameField.setText(selected.getName());
            dosageField.setText(selected.getDosage());

            frequencyBox.setValue(
                    selected.getFrequency()
            );

            updateTimingFields(
                    timingFieldsBox,
                    timingFields,
                    selected.getFrequency(),
                    selected.getReminderTimes()
            );

            startPicker.setValue(
                    selected.getStartDate()
            );

            endPicker.setValue(
                    selected.getEndDate()
            );

            saveButton.setText("Update Medicine");

            cancelButton.setVisible(true);
            cancelButton.setManaged(true);
        });

        cancelButton.setOnAction(e ->
                showMedicinePage()
        );

        Button deleteButton =
                new Button("Delete Selected");

        deleteButton.setStyle(
                "-fx-background-color: #EF4444;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 11px 22px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;"
        );

        deleteButton.setOnAction(e -> {
            Medicine selected = table.getSelectionModel()
                    .getSelectedItem();

            if (selected == null) {
                showAlert(
                        "Selection Required",
                        "Select a medicine to delete."
                );
                return;
            }

            Alert confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION
            );

            confirmation.setTitle("Delete Medicine");
            confirmation.setHeaderText("Confirm deletion");

            confirmation.setContentText(
                    "Are you sure you want to delete "
                            + selected.getName() + "?"
            );

            ButtonType result = confirmation.showAndWait()
                    .orElse(ButtonType.CANCEL);

            if (result == ButtonType.OK) {
                try {
                    FileStorage.deleteMedicine(selected);

                    showAlert(
                            "Deleted",
                            "Medicine deleted successfully."
                    );

                    showMedicinePage();

                } catch (IOException ex) {
                    showAlert(
                            "Delete Error",
                            ex.getMessage()
                    );
                }
            }
        });

        HBox actionButtons = new HBox(
                12,
                saveButton,
                cancelButton
        );

        HBox tableButtons = new HBox(
                12,
                editButton,
                deleteButton
        );

        Label formHeading = new Label("Add or update a medicine");
        formHeading.setStyle(
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #134E4A;"
        );
        Label formDescription = pageSubtitle(
                "Enter the medicine details below, then choose a clock time for each reminder."
        );
        VBox formIntro = new VBox(5, formHeading, formDescription);

        VBox formCard = new VBox(
                14,
                formIntro,
                form,
                actionButtons
        );

        formCard.setPadding(new Insets(22));
        formCard.setStyle(cardStyle());

        VBox.setVgrow(
                table,
                Priority.ALWAYS
        );

        Label savedHeading = sectionHeading("Your saved medicines");
        Label savedDescription = helperText(
                "Select a row to edit or remove a medicine from your list."
        );

        // Keep the table compact when there are only a few medicines.
        table.setPrefHeight(Math.max(190, Math.min(360, items.size() * 43 + 48)));
        table.setPlaceholder(new Label("No medicines saved yet. Add your first medicine above."));

        contentArea.getChildren().addAll(
                formCard,
                new VBox(4, savedHeading, savedDescription),
                tableButtons,
                table
        );
    }

    // =====================================================
    // DUPLICATE MEDICINE VALIDATION
    // =====================================================

    private boolean medicineAlreadyExists(
            String name,
            String dosage,
            String excludeId
    ) throws IOException {
        List<Medicine> medicines =
                FileStorage.loadMedicines();

        for (Medicine existing : medicines) {
            boolean sameName =
                    existing.getName().trim()
                            .equalsIgnoreCase(name.trim());

            boolean sameDosage =
                    existing.getDosage().trim()
                            .equalsIgnoreCase(dosage.trim());

            boolean sameRecord =
                    excludeId != null
                            && excludeId.equals(existing.getId());

            if (sameName && sameDosage && !sameRecord) {
                return true;
            }
        }

        return false;
    }

    // =====================================================
    // DYNAMIC REMINDER TIMING FIELDS
    // =====================================================

    private void updateTimingFields(
            VBox container,
            List<TextField> fields,
            String frequency,
            List<String> existingTimes) {

        container.getChildren().clear();
        fields.clear();

        if (frequency == null) {
            return;
        }

        int count;

        switch (frequency) {
            case "Twice daily":
                count = 2;
                break;
            case "Three times daily":
                count = 3;
                break;
            default:
                count = 1;
                break;
        }

        String[] labels = {
                "First dose:",
                "Second dose:",
                "Third dose:"
        };

        for (int i = 0; i < count; i++) {
            TextField timeField = new TextField();
            timeField.setPromptText("Choose a reminder time");
            timeField.setPrefHeight(42);
            styleField(timeField);

            if (i < existingTimes.size()) {
                timeField.setText(existingTimes.get(i));
            }

            Button clockButton = new Button("◷");
            clockButton.setTooltip(new Tooltip("Choose time"));
            clockButton.setMinSize(44, 42);
            clockButton.setPrefSize(44, 42);
            clockButton.setStyle(
                    "-fx-background-color: #CCFBF1;" +
                    "-fx-text-fill: #0F766E;" +
                    "-fx-font-size: 23px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 10px;" +
                    "-fx-border-color: #99F6E4;" +
                    "-fx-border-radius: 10px;" +
                    "-fx-cursor: hand;"
            );
            clockButton.setOnAction(e -> showTimePicker(timeField));

            HBox inputWithClock = new HBox(9, timeField, clockButton);
            inputWithClock.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(timeField, Priority.ALWAYS);

            Label label = new Label(labels[i]);
            label.setStyle(
                    "-fx-text-fill: #334155;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;"
            );

            VBox timingRow = new VBox(7, label, inputWithClock);
            container.getChildren().add(timingRow);
            fields.add(timeField);
        }
    }

    /**
     * Opens a small clock-style picker and writes the selected time
     * into the reminder text field in the app's existing hh:mm AM/PM format.
     */
    private void showTimePicker(TextField targetField) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Choose reminder time");
        dialog.setHeaderText("Set when you want to take this medicine");

        ButtonType setTime = new ButtonType("Set time", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                setTime,
                ButtonType.CANCEL
        );

        Spinner<Integer> hourSpinner = new Spinner<>(1, 12, 8);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);
        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        hourSpinner.setPrefWidth(88);
        minuteSpinner.setPrefWidth(88);

        ComboBox<String> periodBox = new ComboBox<>(
                FXCollections.observableArrayList("AM", "PM")
        );
        periodBox.setValue("AM");
        periodBox.setPrefWidth(82);

        String current = targetField.getText() == null
                ? ""
                : targetField.getText().trim();

        if (!current.isEmpty()) {
            try {
                LocalTime parsed = LocalTime.parse(current, TIME_FORMAT);
                int hour = parsed.getHour() % 12;
                hourSpinner.getValueFactory().setValue(hour == 0 ? 12 : hour);
                minuteSpinner.getValueFactory().setValue(parsed.getMinute());
                periodBox.setValue(parsed.getHour() < 12 ? "AM" : "PM");
            } catch (Exception ignored) {
                // Keep the default 08:00 AM when the current text is invalid.
            }
        }

        Label hourLabel = new Label("Hour");
        Label minuteLabel = new Label("Minute");
        Label periodLabel = new Label("AM / PM");
        for (Label label : List.of(hourLabel, minuteLabel, periodLabel)) {
            label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        }

        GridPane pickerGrid = new GridPane();
        pickerGrid.setHgap(12);
        pickerGrid.setVgap(8);
        pickerGrid.setPadding(new Insets(12));
        pickerGrid.add(hourLabel, 0, 0);
        pickerGrid.add(minuteLabel, 1, 0);
        pickerGrid.add(periodLabel, 2, 0);
        pickerGrid.add(hourSpinner, 0, 1);
        pickerGrid.add(minuteSpinner, 1, 1);
        pickerGrid.add(periodBox, 2, 1);

        dialog.getDialogPane().setContent(pickerGrid);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #F8FFFC;" +
                "-fx-background-radius: 14px;"
        );

        dialog.showAndWait().filter(result -> result == setTime).ifPresent(result -> {
            try {
                int hour = Integer.parseInt(hourSpinner.getEditor().getText().trim());
                int minute = Integer.parseInt(minuteSpinner.getEditor().getText().trim());
                if (hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                    showAlert("Invalid Time", "Choose an hour from 1–12 and a minute from 0–59.");
                    return;
                }
                int hour24 = hour % 12;
                if ("PM".equals(periodBox.getValue())) {
                    hour24 += 12;
                }
                targetField.setText(
                        LocalTime.of(hour24, minute).format(TIME_FORMAT)
                );
            } catch (Exception ex) {
                showAlert("Invalid Time", "Please choose a valid reminder time.");
            }
        });
    }

    // =====================================================
    // USER PROFILE
    // =====================================================

    private void showProfilePage() {
        contentArea.getChildren().clear();

        contentArea.getChildren().add(
                pageTitle("My Profile")
        );

        UserProfile profile = null;

        try {
            profile = UserProfileStorage.loadProfile();
        } catch (IOException ex) {
            showAlert(
                    "Loading Error",
                    ex.getMessage()
            );
        }

        if (profile == null) {
            showProfileEditForm(null);
            return;
        }

        final UserProfile savedProfile = profile;

        Label description = new Label(
                "Your personal information"
        );

        description.setStyle(
                "-fx-text-fill: #64748B;"
        );

        String initial = savedProfile.getName().isBlank()
                ? "U"
                : savedProfile.getName()
                .substring(0, 1)
                .toUpperCase();

        Label avatar = new Label(initial);

        avatar.setMinSize(75, 75);
        avatar.setMaxSize(75, 75);
        avatar.setAlignment(Pos.CENTER);

        avatar.setStyle(
                "-fx-background-color: #CCFBF1;" +
                "-fx-background-radius: 50%;" +
                "-fx-text-fill: " + primaryTeal + ";" +
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;"
        );

        Label name = new Label(
                savedProfile.getName()
        );

        name.setStyle(
                "-fx-font-size: 23px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1E293B;"
        );

        Label username = new Label(
                "Username: " + loggedInUsername
        );

        Label age = new Label(
                "Age: " + savedProfile.getAge()
        );

        Label contact = new Label(
                "Contact: " +
                (savedProfile.getContact() == null
                        || savedProfile.getContact().isBlank()
                        ? "Not provided"
                        : savedProfile.getContact())
        );

        for (Label label : List.of(
                username,
                age,
                contact
        )) {
            label.setStyle(
                    "-fx-font-size: 15px;" +
                    "-fx-text-fill: #475569;"
            );
        }

        Button editButton =
                new Button("Edit Profile");

        stylePrimaryButton(editButton);

        editButton.setOnAction(e ->
                showProfileEditForm(savedProfile)
        );

        VBox details = new VBox(
                13,
                name,
                username,
                age,
                contact,
                editButton
        );

        VBox profileCard = new VBox(
                20,
                avatar,
                details
        );

        profileCard.setPadding(new Insets(30));
        profileCard.setMaxWidth(550);
        profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.setStyle(cardStyle());

        contentArea.getChildren().addAll(
                description,
                profileCard
        );
    }

    private void showProfileEditForm(
            UserProfile existingProfile
    ) {
        contentArea.getChildren().clear();

        contentArea.getChildren().add(
                pageTitle(
                        existingProfile == null
                                ? "Create Your Profile"
                                : "Edit Profile"
                )
        );

        TextField nameField = new TextField();
        TextField ageField = new TextField();
        TextField contactField = new TextField();

        nameField.setPromptText("Enter full name");
        ageField.setPromptText("Enter age");

        contactField.setPromptText(
                "Contact information (optional)"
        );

        if (existingProfile != null) {
            nameField.setText(
                    existingProfile.getName()
            );

            ageField.setText(
                    String.valueOf(existingProfile.getAge())
            );

            contactField.setText(
                    existingProfile.getContact()
            );
        }

        styleField(nameField);
        styleField(ageField);
        styleField(contactField);

        GridPane form = new GridPane();

        form.setHgap(15);
        form.setVgap(18);
        form.setPadding(new Insets(25));

        form.add(
                new Label("Full Name:"),
                0,
                0
        );

        form.add(nameField, 1, 0);

        form.add(
                new Label("Age:"),
                0,
                1
        );

        form.add(ageField, 1, 1);

        form.add(
                new Label("Contact:"),
                0,
                2
        );

        form.add(contactField, 1, 2);

        ColumnConstraints first =
                new ColumnConstraints(120);

        ColumnConstraints second =
                new ColumnConstraints();

        second.setHgrow(Priority.ALWAYS);

        form.getColumnConstraints().addAll(
                first,
                second
        );

        Button saveButton =
                new Button("Save Profile");

        Button cancelButton =
                new Button("Cancel");

        stylePrimaryButton(saveButton);
        styleSecondaryButton(cancelButton);

        saveButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String ageText = ageField.getText().trim();
            String contact = contactField.getText().trim();

            if (name.isEmpty() || ageText.isEmpty()) {
                showAlert(
                        "Validation Error",
                        "Enter your name and age."
                );
                return;
            }

            try {
                int age = Integer.parseInt(ageText);

                if (age < 1 || age > 120) {
                    showAlert(
                            "Invalid Age",
                            "Enter an age between 1 and 120."
                    );
                    return;
                }

                UserProfile updated =
                        new UserProfile(
                                name,
                                age,
                                contact
                        );

                UserProfileStorage.saveProfile(updated);

                showAlert(
                        "Success",
                        "Profile saved successfully."
                );

                showProfilePage();

            } catch (NumberFormatException ex) {
                showAlert(
                        "Invalid Age",
                        "Age must be a number."
                );
            } catch (IOException ex) {
                showAlert(
                        "Storage Error",
                        ex.getMessage()
                );
            }
        });

        cancelButton.setOnAction(e ->
                showProfilePage()
        );

        HBox buttons = new HBox(
                12,
                saveButton,
                cancelButton
        );

        VBox card = new VBox(
                18,
                form,
                buttons
        );

        card.setPadding(new Insets(20));
        card.setMaxWidth(600);
        card.setStyle(cardStyle());

        contentArea.getChildren().add(card);
    }

    // =====================================================
    // MEDICINE TABLE COLUMN HELPER
    // =====================================================

    private void addMedicineColumn(
            TableView<Medicine> table,
            String title,
            java.util.function.Function<Medicine, String> getter
    ) {
        TableColumn<Medicine, String> column =
                new TableColumn<>(title);

        column.setCellValueFactory(data ->
                new SimpleStringProperty(
                        getter.apply(data.getValue())
                )
        );

        table.getColumns().add(column);
    }

    // =====================================================
    // REMINDERS
    // =====================================================

    private void showReminderPage() {
        contentArea.getChildren().clear();

        contentArea.getChildren().addAll(
                pageTitle("Medication Reminders"),
                pageSubtitle("A simple overview of the doses scheduled for today.")
        );

        Label dateLabel = new Label("TODAY • " + LocalDate.now());
        dateLabel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: #0F766E; -fx-background-color: #CCFBF1;" +
                "-fx-background-radius: 12px; -fx-padding: 10px 14px;"
        );

        Label info = new Label();
        info.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        TableView<Medicine> table =
                new TableView<>();

        ObservableList<Medicine> items =
                FXCollections.observableArrayList();

        addMedicineColumn(
                table, "Medicine", Medicine::getName
        );

        addMedicineColumn(
                table, "Dosage", Medicine::getDosage
        );

        addMedicineColumn(
                table, "Frequency", Medicine::getFrequency
        );

        addMedicineColumn(
                table,
                "Reminder Timings",
                medicine -> String.join(
                        ", ",
                        medicine.getReminderTimes()
                )
        );

        TableColumn<Medicine, String> status =
                new TableColumn<>("Schedule Status");

        status.setCellValueFactory(data -> {
            String result;

            try {
                LocalTime now = LocalTime.now();

                List<String> times =
                        data.getValue().getReminderTimes();

                long upcoming = times.stream()
                        .map(time ->
                                LocalTime.parse(
                                        time,
                                        TIME_FORMAT
                                )
                        )
                        .filter(time ->
                                now.isBefore(time)
                        )
                        .count();

                result = upcoming > 0
                        ? upcoming + " upcoming timing(s)"
                        : "All scheduled times passed";

            } catch (Exception ex) {
                result = "Invalid time";
            }

            return new SimpleStringProperty(result);
        });

        table.getColumns().add(status);

        styleDataTable(table, 360);
        table.setItems(items);

        Runnable refresh = () -> {
            items.clear();

            try {
                for (Medicine medicine :
                        FileStorage.loadMedicines()) {
                    LocalDate today = LocalDate.now();

                    if (!today.isBefore(
                            medicine.getStartDate()
                    ) && !today.isAfter(
                            medicine.getEndDate()
                    )) {
                        items.add(medicine);
                    }
                }

                dateLabel.setText("TODAY • " + LocalDate.now());

                info.setText(
                        items.size() + (items.size() == 1
                                ? " medicine is scheduled for today."
                                : " medicines are scheduled for today.")
                );

            } catch (IOException ex) {
                showAlert(
                        "Loading Error",
                        ex.getMessage()
                );
            }
        };

        Button refreshButton =
                new Button("Refresh Reminders");

        stylePrimaryButton(refreshButton);

        refreshButton.setOnAction(e ->
                refresh.run()
        );

        refresh.run();

        Label tableHeading = sectionHeading("Today's medicine schedule");
        Label note = helperText(
                "Reminder alerts are active while MediCare is running. " +
                "Use Refresh Reminders to reload your latest schedule."
        );
        HBox reminderTop = new HBox(14, dateLabel, new Region(), refreshButton);
        reminderTop.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(reminderTop.getChildren().get(1), Priority.ALWAYS);

        VBox card = new VBox(
                16,
                reminderTop,
                info,
                tableHeading,
                table,
                note
        );

        card.setPadding(new Insets(24));
        card.setStyle(cardStyle());

        contentArea.getChildren().add(card);

        VBox.setVgrow(table, Priority.ALWAYS);
    }

    // =====================================================
    // HEALTH RECORDS WITH WEIGHT AND BP GRAPHS
    // =====================================================

    private void showHealthRecordsPage() {
        contentArea.getChildren().clear();
        contentArea.getChildren().addAll(
                pageTitle("Personal Health Records"),
                pageSubtitle("Record your measurements, review your history, and remove incorrect entries.")
        );

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextField weightField = new TextField();
        TextField systolicField = new TextField();
        TextField diastolicField = new TextField();
        weightField.setPromptText("e.g. 65.5 kg");
        systolicField.setPromptText("e.g. 120 mmHg");
        diastolicField.setPromptText("e.g. 80 mmHg");
        styleField(weightField);
        styleField(systolicField);
        styleField(diastolicField);

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(13);
        form.setPadding(new Insets(20));
        addFormRow(form, 0, "Date:", datePicker);
        addFormRow(form, 1, "Weight (kg):", weightField);
        addFormRow(form, 2, "Systolic BP (mmHg):", systolicField);
        addFormRow(form, 3, "Diastolic BP (mmHg):", diastolicField);

        Label validationHint = helperText(
                "Entry checks: weight 1–500 kg; systolic 50–300 mmHg; " +
                "diastolic 30–200 mmHg; systolic must be higher than diastolic. " +
                "These are data-entry plausibility checks, not medical targets."
        );
        validationHint.setMaxWidth(Double.MAX_VALUE);

        Button saveButton = new Button("Save Health Record");
        stylePrimaryButton(saveButton);

        TableView<HealthRecord> table = new TableView<>();
        ObservableList<HealthRecord> records = FXCollections.observableArrayList();
        addHealthColumn(table, "Date", r -> r.getDate().toString());
        addHealthColumn(table, "Weight (kg)", r -> String.format(Locale.ROOT, "%.1f", r.getWeight()));
        addHealthColumn(table, "Systolic BP", r -> String.valueOf(r.getSystolic()));
        addHealthColumn(table, "Diastolic BP", r -> String.valueOf(r.getDiastolic()));
        styleDataTable(table, 330);

        try {
            records.setAll(HealthRecordStorage.loadRecords());
        } catch (IOException ex) {
            showAlert("Loading Error", "Could not load health records: " + ex.getMessage());
        }
        table.setItems(records);

        VBox graphContainer = new VBox(20);
        graphContainer.setPadding(new Insets(15));
        graphContainer.setStyle(cardStyle());

        Runnable refreshHealthView = () -> {
            try {
                records.setAll(HealthRecordStorage.loadRecords());
                graphContainer.getChildren().setAll(
                        createHealthCharts(new ArrayList<>(records), "Your Health Progress")
                );
            } catch (IOException ex) {
                showAlert("Refresh Error", "Could not refresh health records: " + ex.getMessage());
            }
        };
        refreshHealthView.run();

        saveButton.setOnAction(e -> {
            if (datePicker.getValue() == null || weightField.getText().isBlank()
                    || systolicField.getText().isBlank() || diastolicField.getText().isBlank()) {
                showAlert("Missing Information", "Please fill in the date, weight and both blood pressure values.");
                return;
            }

            try {
                LocalDate recordDate = datePicker.getValue();
                if (recordDate.isAfter(LocalDate.now())) {
                    showAlert("Invalid Date", "A health record date cannot be in the future.");
                    return;
                }

                double weight = Double.parseDouble(weightField.getText().trim());
                int systolic = Integer.parseInt(systolicField.getText().trim());
                int diastolic = Integer.parseInt(diastolicField.getText().trim());

                if (!Double.isFinite(weight) || weight < 1 || weight > 500) {
                    showAlert("Invalid Weight", "Enter a weight from 1 to 500 kg.");
                    weightField.requestFocus();
                    return;
                }
                if (systolic < 50 || systolic > 300) {
                    showAlert("Invalid Systolic BP", "Enter a systolic value from 50 to 300 mmHg.");
                    systolicField.requestFocus();
                    return;
                }
                if (diastolic < 30 || diastolic > 200) {
                    showAlert("Invalid Diastolic BP", "Enter a diastolic value from 30 to 200 mmHg.");
                    diastolicField.requestFocus();
                    return;
                }
                if (systolic <= diastolic) {
                    showAlert("Invalid Blood Pressure", "Systolic BP must be greater than diastolic BP. Please check both readings.");
                    systolicField.requestFocus();
                    return;
                }

                HealthRecordStorage.saveRecord(new HealthRecord(recordDate, weight, systolic, diastolic));
                refreshHealthView.run();
                weightField.clear();
                systolicField.clear();
                diastolicField.clear();
                datePicker.setValue(LocalDate.now());
                showAlert("Saved", "Health record saved successfully.");
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Weight must be a number and blood pressure values must be whole numbers.");
            } catch (IOException ex) {
                showAlert("Storage Error", "Could not save the health record: " + ex.getMessage());
            }
        });

        Button deleteSelectedButton = new Button("Delete Selected");
        deleteSelectedButton.setStyle(
                "-fx-background-color: #DC2626; -fx-text-fill: white;" +
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-padding: 11px 18px; -fx-background-radius: 10px; -fx-cursor: hand;"
        );
        deleteSelectedButton.setOnAction(e -> {
            HealthRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select a Record", "Click a row in the history table before deleting it.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Health Record");
            confirm.setHeaderText("Delete the selected health record?");
            confirm.setContentText("This will permanently remove the selected entry from your local history.");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    HealthRecordStorage.deleteRecord(selected);
                    refreshHealthView.run();
                    showAlert("Deleted", "The selected health record was deleted.");
                } catch (IOException ex) {
                    showAlert("Delete Error", "Could not delete the record: " + ex.getMessage());
                }
            }
        });

        Button deleteAllButton = new Button("Delete All Records");
        deleteAllButton.setStyle(
                "-fx-background-color: #991B1B; -fx-text-fill: white;" +
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-padding: 11px 18px; -fx-background-radius: 10px; -fx-cursor: hand;"
        );
        deleteAllButton.setOnAction(e -> {
            if (records.isEmpty()) {
                showAlert("Nothing to Delete", "There are no health records to remove.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete All Health Records");
            confirm.setHeaderText("Are you sure you want to delete ALL health records?");
            confirm.setContentText("This permanently clears the health_records.txt history and cannot be undone.");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    HealthRecordStorage.deleteAllRecords();
                    refreshHealthView.run();
                    showAlert("History Cleared", "All health records have been deleted.");
                } catch (IOException ex) {
                    showAlert("Delete Error", "Could not clear health history: " + ex.getMessage());
                }
            }
        });

        HBox actionButtons = new HBox(12, deleteSelectedButton, deleteAllButton);
        actionButtons.setAlignment(Pos.CENTER_LEFT);

        VBox formCard = new VBox(14, form, validationHint, saveButton);
        formCard.setPadding(new Insets(18));
        formCard.setStyle(cardStyle());

        contentArea.getChildren().addAll(
                formCard,
                sectionHeading("Health Record History"),
                helperText("Select a row to delete one entry, or clear the complete history."),
                table,
                actionButtons,
                sectionHeading("Health Graphs"),
                graphContainer
        );
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    // =====================================================
    // CREATE WEIGHT AND BLOOD PRESSURE CHARTS
    // =====================================================

    private VBox createHealthCharts(
            List<HealthRecord> records,
            String heading
    ) {
        VBox container = new VBox(18);

        Label title = new Label(heading);
        title.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #287F75;"
        );

        if (records.isEmpty()) {
            Label emptyLabel = new Label(
                    "No health records available. Save your first record to view trend graphs."
            );
            emptyLabel.setStyle(
                    "-fx-text-fill: #64748B;" +
                    "-fx-font-size: 14px;"
            );
            container.getChildren().addAll(title, emptyLabel);
            return container;
        }

        List<HealthRecord> sortedRecords = new ArrayList<>(records);
        sortedRecords.sort(Comparator.comparing(HealthRecord::getDate));

        // WEIGHT PROGRESS - smooth filled area chart
        CategoryAxis weightXAxis = new CategoryAxis();
        weightXAxis.setLabel("Date");

        NumberAxis weightYAxis = new NumberAxis();
        weightYAxis.setLabel("Weight (kg)");
        weightYAxis.setForceZeroInRange(false);
        weightYAxis.setAutoRanging(true);

        AreaChart<String, Number> weightChart =
                new AreaChart<>(weightXAxis, weightYAxis);
        weightChart.setTitle("Weight Progress Trend");
        weightChart.setAnimated(false);
        weightChart.setCreateSymbols(true);
        weightChart.setLegendVisible(false);
        weightChart.setPrefHeight(300);
        weightChart.setMinHeight(280);
        weightChart.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-padding: 8px;"
        );

        XYChart.Series<String, Number> weightSeries = new XYChart.Series<>();
        weightSeries.setName("Weight");
        for (HealthRecord record : sortedRecords) {
            weightSeries.getData().add(new XYChart.Data<>(
                    record.getDate().toString(), record.getWeight()
            ));
        }
        weightChart.getData().add(weightSeries);

        // BLOOD PRESSURE PROGRESS - two filled trend series
        CategoryAxis bpXAxis = new CategoryAxis();
        bpXAxis.setLabel("Date");

        NumberAxis bpYAxis = new NumberAxis();
        bpYAxis.setLabel("Blood Pressure (mmHg)");
        bpYAxis.setForceZeroInRange(false);
        bpYAxis.setAutoRanging(true);

        AreaChart<String, Number> bpChart =
                new AreaChart<>(bpXAxis, bpYAxis);
        bpChart.setTitle("Blood Pressure Dynamics (Systolic vs Diastolic)");
        bpChart.setAnimated(false);
        bpChart.setCreateSymbols(true);
        bpChart.setLegendVisible(true);
        bpChart.setPrefHeight(320);
        bpChart.setMinHeight(300);
        bpChart.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-padding: 8px;"
        );

        XYChart.Series<String, Number> systolicSeries = new XYChart.Series<>();
        systolicSeries.setName("Systolic BP");

        XYChart.Series<String, Number> diastolicSeries = new XYChart.Series<>();
        diastolicSeries.setName("Diastolic BP");

        for (HealthRecord record : sortedRecords) {
            String date = record.getDate().toString();
            systolicSeries.getData().add(new XYChart.Data<>(
                    date, record.getSystolic()
            ));
            diastolicSeries.getData().add(new XYChart.Data<>(
                    date, record.getDiastolic()
            ));
        }

        bpChart.getData().addAll(systolicSeries, diastolicSeries);

        // Apply teal gradient fills and smooth-looking rounded trend lines.
        // The styles are applied after JavaFX creates the chart series nodes.
        javafx.application.Platform.runLater(() -> {
            weightChart.applyCss();
            weightChart.layout();
            bpChart.applyCss();
            bpChart.layout();

            styleAreaSeries(weightChart, 0,
                    "#2F8F83",
                    "linear-gradient(to bottom, rgba(79, 190, 170, 0.48), rgba(79, 190, 170, 0.04))");

            styleAreaSeries(bpChart, 0,
                    "#2F8F83",
                    "linear-gradient(to bottom, rgba(79, 190, 170, 0.42), rgba(79, 190, 170, 0.03))");

            styleAreaSeries(bpChart, 1,
                    "#68AEB0",
                    "linear-gradient(to bottom, rgba(104, 174, 176, 0.38), rgba(104, 174, 176, 0.03))");
        });

        container.getChildren().addAll(title, weightChart, bpChart);
        return container;
    }

    // Styles the generated JavaFX AreaChart series paths.
    private void styleAreaSeries(
            AreaChart<String, Number> chart,
            int seriesIndex,
            String lineColor,
            String fillGradient
    ) {
        String seriesClass = ".default-color" + seriesIndex;

        for (javafx.scene.Node node : chart.lookupAll(
                seriesClass + ".chart-series-area-line")) {
            node.setStyle(
                    "-fx-stroke: " + lineColor + ";" +
                    "-fx-stroke-width: 2.8px;" +
                    "-fx-stroke-line-cap: round;" +
                    "-fx-stroke-line-join: round;"
            );
        }

        for (javafx.scene.Node node : chart.lookupAll(
                seriesClass + ".chart-series-area-fill")) {
            node.setStyle("-fx-fill: " + fillGradient + ";");
        }
    }

    private void addHealthColumn(
            TableView<HealthRecord> table,
            String title,
            java.util.function.Function<HealthRecord, String> getter
    ) {
        TableColumn<HealthRecord, String> column =
                new TableColumn<>(title);

        column.setCellValueFactory(data ->
                new SimpleStringProperty(
                        getter.apply(data.getValue())
                )
        );

        table.getColumns().add(column);
    }

    // =====================================================
    // REPORTS
    // =====================================================

    private void showReportsPage() {
        contentArea.getChildren().clear();

        contentArea.getChildren().add(
                pageTitle("Health Reports")
        );

        DatePicker datePicker =
                new DatePicker(LocalDate.now());

        Button dailyButton =
                new Button("Generate Daily Report");

        Button weeklyButton =
                new Button("Generate Weekly Report");

        stylePrimaryButton(dailyButton);
        stylePrimaryButton(weeklyButton);

        VBox reportCharts = new VBox(20);

        reportCharts.setPadding(new Insets(15));
        reportCharts.setStyle(cardStyle());

        dailyButton.setOnAction(e -> {
            LocalDate selectedDate =
                    datePicker.getValue();

            if (selectedDate == null) return;

            updateReportCharts(
                    selectedDate,
                    selectedDate,
                    reportCharts
            );
        });

        weeklyButton.setOnAction(e -> {
            LocalDate selectedDate =
                    datePicker.getValue();

            if (selectedDate == null) return;

            LocalDate start =
                    selectedDate.with(DayOfWeek.MONDAY);

            LocalDate end =
                    start.plusDays(6);

            updateReportCharts(
                    start,
                    end,
                    reportCharts
            );
        });

        VBox card = new VBox(
                15,
                new Label("Select Report Date:"),
                datePicker,
                new HBox(
                        12,
                        dailyButton,
                        weeklyButton
                )
        );

        card.setPadding(new Insets(20));
        card.setStyle(cardStyle());

        contentArea.getChildren().addAll(
                card,
                new Label("Report Graphs"),
                reportCharts
        );

    }

    private void updateReportCharts(
            LocalDate startDate,
            LocalDate endDate,
            VBox reportCharts
    ) {
        try {
            List<HealthRecord> allRecords =
                    HealthRecordStorage.loadRecords();

            List<HealthRecord> filteredRecords =
                    new ArrayList<>();

            for (HealthRecord record : allRecords) {
                if (!record.getDate().isBefore(startDate)
                        && !record.getDate().isAfter(endDate)) {
                    filteredRecords.add(record);
                }
            }

            reportCharts.getChildren().setAll(
                    createHealthCharts(
                            filteredRecords,
                            "Health Report: "
                                    + startDate
                                    + " to "
                                    + endDate
                    )
            );

        } catch (IOException ex) {
            showAlert(
                    "Graph Error",
                    ex.getMessage()
            );
        }
    }

    // =====================================================
        // MEDICATION TRACKING
    // =====================================================

    private void showMedicationTrackingPage() {
        contentArea.getChildren().clear();

        contentArea.getChildren().addAll(
                pageTitle("Medication Tracking"),
                pageSubtitle("Keep a clear daily record of medicines you have taken or missed.")
        );

        Label description = helperText(
                "Choose a medicine below, then record its status. Your history is saved locally on this computer."
        );

        ComboBox<String> medicineBox =
                new ComboBox<>();

        medicineBox.setPromptText(
                "Select a medicine"
        );

        medicineBox.setMaxWidth(520);
        medicineBox.setPrefHeight(44);
        styleField(medicineBox);

        try {
            for (Medicine medicine :
                    FileStorage.loadMedicines()) {
                String item = medicine.getName()
                        + " - " + medicine.getDosage();

                if (!medicineBox.getItems().contains(item)) {
                    medicineBox.getItems().add(item);
                }
            }
        } catch (IOException ex) {
            showAlert(
                    "Loading Error",
                    ex.getMessage()
            );
        }

        TableView<MedicationLog> table =
                new TableView<>();

        ObservableList<MedicationLog> history =
                FXCollections.observableArrayList();

        addLogColumn(
                table,
                "Date",
                log -> log.getDate().toString()
        );

        addLogColumn(
                table,
                "Medicine",
                MedicationLog::getMedicineName
        );

        addLogColumn(
                table,
                "Dosage",
                MedicationLog::getDosage
        );

        addLogColumn(
                table,
                "Status",
                MedicationLog::getStatus
        );

        styleDataTable(table, 360);

        try {
            List<MedicationLog> loadedLogs = MedicationLogStorage.loadLogs();
            loadedLogs.sort(Comparator.comparing(MedicationLog::getDate).reversed());
            history.setAll(loadedLogs);
        } catch (IOException ex) {
            showAlert(
                    "Loading Error",
                    ex.getMessage()
            );
        }

        table.setItems(history);

        Button takenButton =
                new Button("Mark as Taken");

        Button missedButton =
                new Button("Mark as Missed");

        Button refreshButton =
                new Button("Refresh History");

        // Taken (Completed) - Green
        takenButton.setStyle(
                "-fx-background-color: #7fd886;" +
                "-fx-text-fill: #14532D;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 12px;" +
                "-fx-padding: 12px 20px;" +
                "-fx-cursor: hand;"
        );

        // Missed (Pending) - Blue
        missedButton.setStyle(
                "-fx-background-color: #88c0ca;" +
                "-fx-text-fill: #164E63;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 12px;" +
                "-fx-padding: 12px 20px;" +
                "-fx-cursor: hand;"
        );

        stylePrimaryButton(refreshButton);
        refreshButton.setOnAction(e -> {
            try {
                List<MedicationLog> latestLogs = MedicationLogStorage.loadLogs();
                latestLogs.sort(Comparator.comparing(MedicationLog::getDate).reversed());
                history.setAll(latestLogs);
            } catch (IOException ex) {
                showAlert("History Error", ex.getMessage());
            }
        });

        takenButton.setOnAction(e ->
                saveMedicationStatus(
                        medicineBox,
                        "Taken",
                        history
                )
        );

        missedButton.setOnAction(e ->
                saveMedicationStatus(
                        medicineBox,
                        "Missed",
                        history
                )
        );

        Label chooseLabel = new Label("SELECT MEDICINE");
        chooseLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-text-fill: #475569;"
        );
        Label historyHeading = sectionHeading("Medication history");
        Label historyDescription = helperText(
                "Your saved Taken and Missed entries appear here, newest first."
        );
        HBox actionRow = new HBox(12, takenButton, missedButton, refreshButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(8, 0, 8, 0));

        VBox recordCard = new VBox(13, description, chooseLabel, medicineBox, actionRow);
        recordCard.setPadding(new Insets(20));
        recordCard.setStyle(
                "-fx-background-color: #F8FCFB;" +
                "-fx-background-radius: 14px;" +
                "-fx-border-color: #DCEFEA;" +
                "-fx-border-radius: 14px;" +
                "-fx-border-width: 1px;"
        );

        VBox card = new VBox(
                17,
                recordCard,
                new VBox(4, historyHeading, historyDescription),
                table
        );

        card.setPadding(new Insets(24));
        card.setStyle(cardStyle());

        contentArea.getChildren().add(card);

        VBox.setVgrow(
                table,
                Priority.ALWAYS
        );
    }

    private void addLogColumn(
            TableView<MedicationLog> table,
            String title,
            java.util.function.Function<MedicationLog, String> getter
    ) {
        TableColumn<MedicationLog, String> column =
                new TableColumn<>(title);

        column.setCellValueFactory(data ->
                new SimpleStringProperty(
                        getter.apply(data.getValue())
                )
        );

        table.getColumns().add(column);
    }

    private void saveMedicationStatus(
            ComboBox<String> medicineBox,
            String status,
            ObservableList<MedicationLog> history
    ) {
        String selected = medicineBox.getValue();

        if (selected == null || selected.isBlank()) {
            showAlert(
                    "Medicine Required",
                    "Select a medicine first."
            );
            return;
        }

        String[] parts = selected.split(" - ", 2);
        String medicineName = parts[0];

        String dosage =
                parts.length > 1 ? parts[1] : "";

        try {
            MedicationLog log = new MedicationLog(
                    LocalDate.now(),
                    medicineName,
                    dosage,
                    status
            );

            MedicationLogStorage.saveOrUpdateLog(log);

            List<MedicationLog> loadedLogs = MedicationLogStorage.loadLogs();
            loadedLogs.sort(Comparator.comparing(MedicationLog::getDate).reversed());
            history.setAll(loadedLogs);

            showAlert(
                    "Status Saved",
                    medicineName + " marked as " + status + "."
            );

        } catch (IOException ex) {
            showAlert(
                    "Storage Error",
                    ex.getMessage()
            );
        }
    }

    // =====================================================
    // FORM ROW HELPER
    // =====================================================

    private void addFormRow(
            GridPane form,
            int row,
            String label,
            Control field
    ) {
        Label text = new Label(label);
        text.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #475569;"
        );

        form.add(text, 0, row);
        form.add(field, 1, row);

        ColumnConstraints first =
                new ColumnConstraints(130);

        ColumnConstraints second =
                new ColumnConstraints();

        second.setHgrow(Priority.ALWAYS);

        if (form.getColumnConstraints().isEmpty()) {
            form.getColumnConstraints().addAll(
                    first,
                    second
            );
        }

        field.setMaxWidth(Double.MAX_VALUE);
    }

    // =====================================================
    // ALERT
    // =====================================================

    private void showAlert(
            String title,
            String message
    ) {
        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // =====================================================
    // MAIN
    // =====================================================

    public static void main(String[] args) {
        launch(args);
    }
}