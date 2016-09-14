/*
    This file is part of ImageJ FX.

    ImageJ FX is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ImageJ FX is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
    
     Copyright 2015,2016 Cyril MONGIS, Michael Knop
	
 */
package ijfx.ui.widgets;

import static com.squareup.okhttp.internal.Internal.logger;
import ijfx.ui.batch.ExplorableTableHelper;
import ijfx.ui.batch.FileInputModel;
import ijfx.ui.batch.MetaDataSetOwnerHelper;
import ijfx.ui.explorer.Explorable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import mongis.utils.FXUtilities;

/**
 *
 * @author cyril
 */
public class ExplorableSelector extends BorderPane{

    @FXML
    TableView<Explorable> tableView;

    @FXML
    TextField filterTextField;

    @FXML
    Button markButton;

    @FXML
    Label markedLabel; // label representing the number of marked object

    ObservableList<Explorable> filteredFiles = FXCollections.observableArrayList();

    ObservableList<Explorable> addedFiles = FXCollections.observableArrayList();

    MetaDataSetOwnerHelper<Explorable> helper;

    BooleanBinding isFilterOn;

    /**
     * List of selected explorable
     */
    ListProperty<Explorable> selectedFilesListProperty;

    /**
     * Binding representing is multiple object are selected
     */
    BooleanBinding isMultipleSelection;

    /**
     * Binding representing the lalbel of the button used to mark and unmark
     * elements
     */
    StringBinding markButtonText;

    StringBinding markLabelText;

    private final static String MARK_SELECTION = "Mark selection";
    private final static String MARK_ALL = "Mark all";
    private final static String MARK_ALL_FILTERED = "Mark all filtered";

    private final static String MARK_LABEL_TEXT = "%d files marked for processing";

    public ExplorableSelector() {

        try {
            FXUtilities.injectFXML(this,"/ijfx/ui/widgets/ExplorableSelector.fxml");

            helper = new ExplorableTableHelper(tableView);
            tableView.setItems(filteredFiles);

            //markedColumn.setCellFactory(this::generateCheckBoxCell);
            //    markedColumn.setEditable(true);
            tableView.setEditable(true);
            tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            tableView.setItems(filteredFiles);

            filterTextField.setOnKeyTyped(this::onKeyTyped);

            // defining remaining properties
            // true when the filter has something written inside
            isFilterOn = Bindings.notEqual("", filterTextField.textProperty());

            // property giving access to the number of selected files inside the table view
            selectedFilesListProperty = new SimpleListProperty<>(tableView.getSelectionModel().getSelectedItems());

            // true when multiple selection
            isMultipleSelection = Bindings.greaterThan(selectedFilesListProperty.sizeProperty(), 1);

            // creating a binding calling a method deciding for the mark name
            markButtonText = Bindings.createStringBinding(this::getMarkButtonText, isFilterOn, isMultipleSelection);

            // binding the mark button to the property
            markButton.textProperty().bind(markButtonText);

            markLabelText = Bindings.createStringBinding(this::updateMarkedLabel, addedFiles);

            markedLabel.textProperty().bind(markLabelText);

            addedFiles.addListener(this::onItemsAdded);
            
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

    }

    /*
    
        FXML UI Methods
    
     */
    private String updateMarkedLabel() {

        long count = addedFiles.stream().filter(this::isMarked).count();

        return String.format(MARK_LABEL_TEXT, count);

    }

   

   

    @FXML
    private void markSelection() {
        if (isMultipleSelection.getValue()) {
            tableView
                    .getSelectionModel()
                    .getSelectedItems()
                    .forEach(this::markFileForSelection);
        } else if (isFilterOn.getValue()) {
            filteredFiles.forEach(this::markFileForSelection);
        } else {
            addedFiles.forEach(this::markFileForSelection);
        }
        onSelected(null, null, null);
    }

    @FXML
    private void unmarkSelection() {
        addedFiles.forEach(fileInputModel -> fileInputModel.selectedProperty().setValue(false));
        onSelected(null, null, null);
    }

    @FXML
    private void deleteSelection() {
        addedFiles.forEach(fileInputModel -> fileInputModel.selectedProperty().setValue(false));
    }

    @FXML
    private void deleteAll() {

        addedFiles.forEach(finput -> {
            finput.selectedProperty().removeListener(this::onSelected);
        });

        addedFiles.clear();
        filteredFiles.clear();
    }

    private void markFileForSelection(Explorable fileInputModel) {
        fileInputModel.selectedProperty().setValue(true);

    }

    private boolean isMarked(Explorable finputModel) {
        return finputModel.selectedProperty().getValue();
    }

    private String getMarkButtonText() {

        if (isMultipleSelection.getValue()) {
            return MARK_SELECTION;
        } else if (isFilterOn.getValue()) {
            return MARK_ALL_FILTERED;
        } else {
            return MARK_ALL;
        }

    }

   



    private void onKeyTyped(KeyEvent event) {
        updateFilter();
    }

    public void updateFilter() {
        final String filterContent = filterTextField.getText().toLowerCase();

        // if nothing is on the filter field
        if (filterContent.trim().equals("")) {
            this.filteredFiles.clear();
            this.filteredFiles.addAll(addedFiles);

        } // filtering the files
        else {
            System.out.println("Filtering");
            List<Explorable> filteredFiles = addedFiles
                    .parallelStream()
                    .filter(explorable -> {
               return explorable.getTitle().toLowerCase().contains(filterContent);

            }).collect(Collectors.toList());

            this.filteredFiles.clear();
            this.filteredFiles.addAll(filteredFiles);

        }
    }

   

    public void onSelected(Observable obs, Boolean oldValue, Boolean newValue) {
        if (oldValue == null || oldValue.equals(newValue)) {
            return;
        }
        markLabelText.invalidate();
        markLabelText.getValue();
    }
    
    
    public void addItem(Collection<Explorable> items) {
        this.addedFiles.addAll(items);
       
    }

    public void onItemsAdded(Change<? extends Explorable> change) {
        while(change.next()) {
            
            
            helper.setColumnsFromItems(addedFiles);
            updateFilter();
            
            change.getAddedSubList()
                    .forEach(expl->expl.selectedProperty().addListener(this::onSelected));
            
            change.getRemoved()
                    .forEach(expl->expl.selectedProperty().removeListener(this::onSelected));
            
        }
    }
    
    
    
}  