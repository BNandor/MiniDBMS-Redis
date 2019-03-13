package gui;

import comm.Client;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public TreeView treeView;
    public TextArea textArea;
    public ImageView refreshButton;
    public MenuItem closeMenu;

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
}
