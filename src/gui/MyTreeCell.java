package gui;

import javafx.scene.control.TreeCell;

public class MyTreeCell extends TreeCell<String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            return;
        }

        setText(getItem() == null ? "" : getItem());

        if (getTreeItem().getParent() == null) { // Databases
            setContextMenu(new RootContextMenu());
        } else if (getTreeItem().isLeaf() && getTreeItem().getParent().getValue().equals("Tables")) { // Table
            setContextMenu(new TableContextMenu(getTreeItem().getParent().getParent().getValue(), getTreeItem().getValue()));
        } else if (!getTreeItem().isLeaf() && getTreeItem().getParent().getValue().equals("Databases")) { // Database
            setContextMenu(new DatabaseContextMenu(getTreeItem().getValue()));
        } else {
            setContextMenu(null);
        }
    }
}
