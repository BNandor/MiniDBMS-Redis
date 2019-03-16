package gui;

import comm.Client;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public TreeView treeView;
    public MenuItem closeMenu;
    public Button refreshButton;
    public Button runButton;
    private TextArea textArea;
    public ScrollPane scrollPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TreeItem<String> root = Client.getClient().getDatabases().visit();
        treeView.setRoot(root);

        treeView.setCellFactory(new Callback<TreeView, TreeCell>() {
            @Override
            public TreeCell call(TreeView treeView) {
                return new MyTreeCell();
            }
        });

        textArea = new TextArea();
        scrollPane.setContent(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
    }

    public void updateTreeView() {
        treeView.getRoot().getChildren().clear();
        TreeItem<String> root = Client.getClient().getDatabases().visit();
        treeView.setRoot(root);
    }

    public void closeWindow(ActionEvent actionEvent) {
        Client.getClient().write("exit\n");
        Platform.exit();
    }

    private void setSaveAccelerator() {
        Scene scene = runButton.getScene();

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), new Runnable() {
            @Override
            public void run() {
                runButton.fire();
            }
        });

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN), new Runnable() {
            @Override
            public void run() {
                refreshButton.fire();
            }
        });

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN), new Runnable() {
            @Override
            public void run() {
                closeMenu.fire();
            }
        });

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), new Runnable() {
            @Override
            public void run() {
                runSelectedLines();
            }
        });
    }

    private void runSelectedLines() {
        String sql = "";

        for (String line : textArea.getSelectedText().split("\\n")) {
            String processedLine = line.trim().replaceAll("\\s+", " ");

            if (processedLine.equals("")) {
                continue;
            }

            if (processedLine.contains(";")) {
                sql += processedLine.replaceFirst(".$", "").trim();

                Client.getClient().write(sql + "\n");
                String answer = Client.getClient().readLine();

                if (!answer.equals("OK")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText(null);
                    alert.showAndWait();
                }

                sql = "";
            } else {
                sql += processedLine + " ";
            }
        }
    }

    public void setup() {
        setSaveAccelerator();
    }

    public void runEverything(ActionEvent actionEvent) {
        String sql = "";

        for (String line : textArea.getText().split("\\n")) {
            String processedLine = line.trim().replaceAll("\\s+", " ");

            if (processedLine.equals("")) {
                continue;
            }

            if (processedLine.contains(";")) {
                sql += processedLine.replaceFirst(".$", "").trim();

                Client.getClient().write(sql + "\n");
                String answer = Client.getClient().readLine();

                if (!answer.equals("OK")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText(null);
                    alert.showAndWait();
                }

                sql = "";
            } else {
                sql += processedLine + " ";
            }
        }
    }
}
