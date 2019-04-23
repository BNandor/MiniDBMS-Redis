package gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;

public class SelectController {
    public VBox vbox;
    public ScrollPane closeButton;

    public void load(ArrayList<String> colNames, ArrayList<ArrayList<String>> rows) {
        Platform.runLater(() -> {
            TableView<ArrayList<String>> tableView = new TableView<>();
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            for (int i = 0; i < colNames.size(); ++i) {
                TableColumn<ArrayList<String>, String> tableColumn = new TableColumn<>(colNames.get(i));
                final int colIndex = i;

                tableColumn.setCellValueFactory(cellData -> {
                    ArrayList<String> row = cellData.getValue();
                    return new SimpleStringProperty(row.get(colIndex));
                });

                tableView.getColumns().add(tableColumn);
            }

            for (ArrayList<String> row : rows) {
                tableView.getItems().add(row);
            }

            vbox.getChildren().add(tableView);
        });
    }

    public void closeWindow(ActionEvent actionEvent) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
