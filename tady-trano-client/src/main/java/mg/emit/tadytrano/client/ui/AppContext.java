package mg.emit.tadytrano.client.ui;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.api.Session;

import java.util.ArrayList;
import java.util.List;

/**
 * Contexte applicatif singleton : API client, session utilisateur, routage et notifications.
 */
public class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private final Session session = new Session();
    private final ApiClient apiClient;
    private final ThemeManager themeManager = new ThemeManager();
    private NavigationManager navigationManager;
    private StackPane notificationContainer;

    private final List<Runnable> authChangeListeners = new ArrayList<>();

    private AppContext() {
        String baseUrl = System.getProperty("tady.api.url", "http://localhost:8080");
        this.apiClient = new ApiClient(baseUrl, session);
    }

    public static AppContext get() {
        return INSTANCE;
    }

    public Session getSession() {
        return session;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public NavigationManager getNavigationManager() {
        return navigationManager;
    }

    public void setNavigationManager(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }

    public void setNotificationContainer(StackPane notificationContainer) {
        this.notificationContainer = notificationContainer;
    }

    public void onAuthChanged(Runnable listener) {
        authChangeListeners.add(listener);
    }

    public void notifierChangementAuth() {
        Platform.runLater(() -> {
            for (Runnable r : authChangeListeners) {
                try {
                    r.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void showToast(String message, boolean isError) {
        Platform.runLater(() -> {
            if (notificationContainer == null) return;

            Label toast = new Label(message);
            toast.getStyleClass().add(isError ? "toast-error" : "toast-success");
            StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
            toast.setTranslateX(-20);
            toast.setTranslateY(-20);

            notificationContainer.getChildren().add(toast);

            PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
            delay.setOnFinished(e -> notificationContainer.getChildren().remove(toast));
            delay.play();
        });
    }

    public void showSuccess(String message) {
        showToast(message, false);
    }

    public void showError(String message) {
        showToast(message, true);
    }
}
