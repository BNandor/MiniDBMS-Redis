package gui;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import comm.Client;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import queries.misc.selectresultprotocol.Header;
import queries.misc.selectresultprotocol.Page;
import queries.misc.selectresultprotocol.Row;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
        treeView.setCellFactory(treeView -> new MyTreeCell());

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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), () -> runButton.fire());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN), () -> refreshButton.fire());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN), () -> closeMenu.fire());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), () -> {
            try {
                runSelectedLines();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void runSelectedLines() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("selectPopup.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(700);
        SelectController selectController = loader.getController();

        String sql = "";

        for (String line : textArea.getSelectedText().split("\\n")) {
            String processedLine = line.trim().replaceAll("\\s+", " ");

            if (processedLine.equals("")) {
                continue;
            }

            if (processedLine.contains(";")) {
                sql += processedLine.replaceFirst(".$", "").trim();
                Client.getClient().write(sql + "\n");

                if (sql.toLowerCase().startsWith("select")) {
                    String answer = Client.getClient().readLine();
                    if (!answer.equals("READY")) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                        alert.setTitle("Error Dialog");
                        alert.setHeaderText(null);
                        alert.showAndWait();
                        break;
                    }

                    XmlMapper xmlMapper = new XmlMapper();
                    Header readHeader = xmlMapper.readValue(Client.getClient().readLine(), Header.class);
                    ArrayList<ArrayList<String>> rows = new ArrayList<>();

                    for (int i = 0; i < readHeader.getPageNumber(); i++) {
                        Page page = xmlMapper.readValue(Client.getClient().readLine(), Page.class);
                        for (Row row : page.getRows()) {
                            rows.add(row.getValues());
                        }
                    }

                    answer = Client.getClient().readLine();
                    if (!answer.equals("OK")) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                        alert.setTitle("Error Dialog");
                        alert.setHeaderText(null);
                        alert.showAndWait();
                        break;
                    }

                    // update window with new output
                    selectController.load(readHeader.getColumnNames(), rows);

                    if (!stage.isShowing()) {
                        stage.show();
                    }
                } else {
                    String answer = Client.getClient().readLine();

                    if (!answer.equals("OK")) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                        alert.setTitle("Error Dialog");
                        alert.setHeaderText(null);
                        alert.showAndWait();
                        break;
                    }
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

    public void runEverything(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("selectPopup.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setMinWidth(1085);
        stage.setMinHeight(700);
        SelectController selectController = loader.getController();

        String sql = "";

        for (String line : textArea.getText().split("\\n")) {
            String processedLine = line.trim().replaceAll("\\s+", " ");

            if (processedLine.equals("")) {
                continue;
            }

            if (processedLine.contains(";")) {
                sql += processedLine.replaceFirst(".$", "").trim();
                Client.getClient().write(sql + "\n");

                if (sql.toLowerCase().startsWith("select")) {
                    String answer = Client.getClient().readLine();
                    if (!answer.equals("READY")) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                        alert.setTitle("Error Dialog");
                        alert.setHeaderText(null);
                        alert.showAndWait();
                        break;
                    }

                    XmlMapper xmlMapper = new XmlMapper();
                    Header readHeader = xmlMapper.readValue(Client.getClient().readLine(), Header.class);
                    ArrayList<ArrayList<String>> rows = new ArrayList<>();

                    for (int i = 0; i < readHeader.getPageNumber(); i++) {
                        Page page = xmlMapper.readValue(Client.getClient().readLine(), Page.class);
                        for (Row row : page.getRows()) {
                            rows.add(row.getValues());
                        }
                    }

                    answer = Client.getClient().readLine();
                    if (!answer.equals("OK")) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                        alert.setTitle("Error Dialog");
                        alert.setHeaderText(null);
                        alert.showAndWait();
                        break;
                    }

                    // update window with new output
                    selectController.load(readHeader.getColumnNames(), rows);

                    if (!stage.isShowing()) {
                        stage.show();
                    }
                } else {
                    String answer = Client.getClient().readLine();

                    if (!answer.equals("OK")) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                        alert.setTitle("Error Dialog");
                        alert.setHeaderText(null);
                        alert.showAndWait();
                        break;
                    }
                }

                sql = "";
            } else {
                sql += processedLine + " ";
            }
        }
    }
}
