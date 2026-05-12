package com.academiq;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    private StackPane contentArea;
    private VBox sidebar;
    private Pane courseListPane;
    private Pane gradeEntryPane;
    private Pane dashboardPane;
    private Pane schedulePane;
    private Button coursesButton;
    private Button gradesButton;
    private Button dashboardButton;
    private Button scheduleButton;
    private Button activeButton;

    @Override
    public void start(Stage stage) {
        courseListPane = createCourseListPane();
        gradeEntryPane = createGradeEntryPane();
        dashboardPane = createDashboardPane();
        schedulePane = createSchedulePane();

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        sidebar = createSidebar();

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        navigateTo("courses");

        stage.setTitle("AcademiQ");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createSidebar() {
        VBox box = new VBox();
        box.getStyleClass().add("sidebar");

        Label title = new Label("AcademiQ");
        title.getStyleClass().add("app-title");

        Separator separator = new Separator();

        coursesButton = createNavButton("📚  Courses", "courses");
        gradesButton = createNavButton("📝  Grade Entry", "grades");
        dashboardButton = createNavButton("📊  Dashboard", "dashboard");
        scheduleButton = createNavButton("📅  Schedule", "schedule");

        VBox navButtons = new VBox(coursesButton, gradesButton, dashboardButton, scheduleButton);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label version = new Label("v1.0-SNAPSHOT");
        version.getStyleClass().add("version-label");

        box.getChildren().addAll(title, separator, navButtons, spacer, version);
        return box;
    }

    private Button createNavButton(String text, String viewName) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> navigateTo(viewName));
        return button;
    }

    private void navigateTo(String viewName) {
        contentArea.getChildren().clear();
        Button next = switch (viewName) {
            case "courses" -> {
                contentArea.getChildren().add(courseListPane);
                yield coursesButton;
            }
            case "grades" -> {
                contentArea.getChildren().add(gradeEntryPane);
                yield gradesButton;
            }
            case "dashboard" -> {
                contentArea.getChildren().add(dashboardPane);
                yield dashboardButton;
            }
            case "schedule" -> {
                contentArea.getChildren().add(schedulePane);
                yield scheduleButton;
            }
            default -> null;
        };

        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-active");
        }
        if (next != null) {
            if (!next.getStyleClass().contains("nav-active")) {
                next.getStyleClass().add("nav-active");
            }
            activeButton = next;
        }
    }

    private VBox createCourseListPane() {
        return createPlaceholder("Course List", "Select a term to view courses");
    }

    private VBox createGradeEntryPane() {
        return createPlaceholder("Grade Entry", "Select a course to enter grades");
    }

    private VBox createDashboardPane() {
        return createPlaceholder("Dashboard", "GPA overview and statistics");
    }

    private VBox createSchedulePane() {
        return createPlaceholder("Schedule", "Weekly class schedule and conflicts");
    }

    private VBox createPlaceholder(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("placeholder-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("placeholder-subtitle");

        VBox box = new VBox(title, subtitle);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
