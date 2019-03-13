package gui;

import comm.Client;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class addDatabaseController {
    public Button cancelButton;
    public Button addButton;
    public TextField databaseName;

    public void addDatabase(ActionEvent actionEvent) {
        Client.getClient().write("CREATE DATABASE " + databaseName.getText() + "\n");
        String answer = Client.getClient().readLine();

        if (!answer.equals("OK")) {
            databaseName.clear();
            Alert alert = new Alert(Alert.AlertType.ERROR, answer);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.showAndWait();
        } else {
            Stage stage = (Stage) addButton.getScene().getWindow();
            stage.close();
        }
    }

    public void cancelAddition(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
