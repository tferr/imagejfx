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
package ijfx.ui.explorer;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import ijfx.core.metadata.MetaData;
import ijfx.core.metadata.MetaDataKeyPriority;
import ijfx.core.metadata.MetaDataOwner;
import ijfx.core.metadata.MetaDataSet;
import ijfx.core.metadata.MetaDataSetUtils;
import ijfx.service.ui.HintService;
import ijfx.service.ui.LoadingScreenService;
import ijfx.service.uicontext.UiContextService;
import ijfx.ui.activity.Activity;
import ijfx.ui.explorer.cell.FolderListCellCtrl;
import ijfx.ui.explorer.event.DisplayedListChanged;
import ijfx.ui.explorer.event.FolderAddedEvent;
import ijfx.ui.explorer.event.FolderDeletedEvent;
import ijfx.ui.explorer.event.ExploredListChanged;
import ijfx.ui.explorer.event.FolderUpdatedEvent;
import ijfx.ui.explorer.smartaction.ExplorerAction;
import ijfx.ui.filter.DefaultMetaDataFilterFactory;
import ijfx.ui.filter.MetaDataFilterFactory;
import ijfx.ui.filter.MetaDataOwnerFilter;
import ijfx.ui.main.ImageJFX;
import ijfx.ui.main.SideMenuBinding;
import ijfx.ui.utils.CollectionUtils;
import ijfx.ui.utils.DragPanel;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import static javafx.scene.input.KeyCode.T;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import mongis.utils.CallbackTask;
import mongis.utils.FXUtilities;
import mongis.utils.ProgressHandler;
import mongis.utils.TextFileUtils;
import org.reactfx.EventStreams;
import org.scijava.InstantiableException;
import org.scijava.app.StatusService;
import org.scijava.event.EventHandler;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;

/**
 *
 * @author Cyril MONGIS, 2016
 */
@Plugin(type = Activity.class, name = "explore")
public class ExplorerActivity extends AnchorPane implements Activity {

    @FXML
    private ListView<Folder> folderListView;

    @FXML
    private BorderPane contentBorderPane;

    @FXML
    private ToggleButton filterToggleButton;

    @FXML
    private TextField filterTextField;

    @FXML
    private Accordion filterVBox;

    @FXML
    private ScrollPane filterScrollPane;

    @FXML
    private ToggleButton fileModeToggleButton;

    @FXML
    private ToggleButton planeModeToggleButton;

    @FXML
    private ToggleButton objectModeToggleButton;

    @FXML
    private Button statisticsButton;

    @FXML
    private MenuButton moreMenuButton;

    private ToggleGroup explorationModeToggleGroup;

    private final ObjectProperty<ExplorerView> currentView = new SimpleObjectProperty<>();
    @Parameter
    private FolderManagerService folderManagerService;

    @Parameter
    private ExplorerService explorerService;

    @Parameter
    private LoadingScreenService loadingScreenService;

    @Parameter
    private StatusService statusService;

    @Parameter
    private PluginService pluginService;

    @Parameter
    private UiContextService uiContextService;

    @Parameter
    private UIService uiService;

    @Parameter
    private HintService hintService;

    @FXML
    private TabPane tabPane;

    private ExplorerView view;

    private List<Runnable> folderUpdateHandler = new ArrayList<>();

    private Property<ExplorationMode> explorationModeProperty = new SimpleObjectProperty<>();

    private BooleanProperty folderListEmpty = new SimpleBooleanProperty(true);

    private BooleanProperty explorerListEmpty = new SimpleBooleanProperty(true);

    private DragPanel dragPanel;

    private final String NO_FOLDER_TEXT = "Click on \"Add folder\" or drop a\nfolder here to explorer it";
    private final FontAwesomeIcon NO_FOLDER_ICON = FontAwesomeIcon.DOWNLOAD;
    private final String EMPTY_FOLDER_TEXT = "Empty";
    private final FontAwesomeIcon EMPTY_FOLDER_ICON = FontAwesomeIcon.FROWN_ALT;

    Logger logger = ImageJFX.getLogger();

    List<Explorable> currentItems;

    public ExplorerActivity() {
        try {
            FXUtilities.injectFXML(this);

            // contentBorderPane.setCenter(view.getNode());
            folderListView.setCellFactory(this::createFolderListCell);
            folderListView.getSelectionModel().selectedItemProperty().addListener(this::onFolderSelectionChanged);

            SideMenuBinding binding = new SideMenuBinding(filterScrollPane);

            binding.showProperty().bind(filterToggleButton.selectedProperty());
            filterScrollPane.setTranslateX(-300);

            explorationModeToggleGroup = new ToggleGroup();
            explorationModeToggleGroup.getToggles().addAll(fileModeToggleButton, planeModeToggleButton, objectModeToggleButton);

            fileModeToggleButton.setUserData(ExplorationMode.FILE);
            planeModeToggleButton.setUserData(ExplorationMode.PLANE);
            objectModeToggleButton.setUserData(ExplorationMode.OBJECT);

            explorationModeToggleGroup.selectedToggleProperty().addListener(this::onToggleSelectionChanged);

            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {

                currentView.setValue((ExplorerView) newTab.getUserData());

            });

            currentView.addListener(this::onViewModeChanged);

            dragPanel = new DragPanel("No folder open", FontAwesomeIcon.DASHCUBE);

            folderListEmpty.addListener(this::onFolderListEmptyPropertyChange);
            explorerListEmpty.addListener(this::onExplorerListEmptyPropertyChange);

            //fluentIconBinding(fileModeToggleButton,planeModeToggleButton,objectModeToggleButton);
            EventStreams.valuesOf(filterTextField.textProperty()).successionEnds(Duration.ofSeconds(1))
                    .subscribe(this::updateTextFilter);

        } catch (IOException ex) {
            Logger.getLogger(ExplorerActivity.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void init() {
        if (view == null) {
            List<ExplorerView> views = pluginService.createInstancesOfType(ExplorerView.class);

            List<Tab> buttons = views.stream().map(this::createTab).collect(Collectors.toList());
            //Map<ExplorerView, ToggleButton> viewButtons = views.stream().collect(Collectors.toMap(v -> v, this::createViewToggle));

            tabPane.getTabs().addAll(buttons);

            //buttons.get(0).getStyleClass().add("first");
            //buttons.get(buttons.size() - 1).getStyleClass().add("last");
            currentView.setValue(views.get(0));

            // add the items to the menu in the background
            new CallbackTask<PluginService,List<MenuItem>>().
                    setInput(pluginService)
                    .run(this::initActionMenu)
                    .then(moreMenuButton.getItems()::addAll)
                    .start();
        }
    }

    @Override
    public Node getContent() {
        return this;
    }

    @Override
    public Task updateOnShow() {

        init();

        explorationModeToggleGroup.selectToggle(getToggleButton(folderManagerService.getCurrentExplorationMode()));
        return new CallbackTask<Void, List<Explorable>>()
                .run(this::update)
                .then(this::updateUi)
                .start();
    }

    // get the list of items to show from the explorer service
    public List<Explorable> update(Void v) {
        if (folderManagerService.getCurrentFolder() == null) {
            return new ArrayList<Explorable>();
        } else {
            return explorerService.getFilteredItems();
        }
    }

    public void onFolderSelectionChanged(Observable obs, Folder oldValue, Folder newValue) {
        folderManagerService.setCurrentFolder(newValue);
        filterTextField.setText("");
    }

    public void updateFolderList() {

        CollectionUtils.syncronizeContent(folderManagerService.getFolderList(), folderListView.getItems());

    }

    public void updateExplorerView(ExplorerView view) {
        this.view = view;
        //contentBorderPane.setCenter(view.getNode());

        view.setItem(explorerService.getFilteredItems());
    }

    public void updateUi(List<? extends Explorable> explorable) {

        updateFolderList();

        folderListEmpty.setValue(folderListView.getItems().isEmpty());
        explorerListEmpty.setValue(explorable == null || explorable.isEmpty());
        if (explorable != null) {
            view.setItem(explorable);
        }

        if (folderListEmpty.getValue()) {
            hintService.displayHints("/ijfx/ui/explorer/ExplorerActivity-tutorial-1.hints.json", false);
        }
    }

    // returns true if the folder is not displayed yet
    private boolean isNotDisplayed(Folder folder) {
        return !folderListView.getItems().contains(folder);
    }

    @FXML
    public void addFolder() {
        File f = FXUtilities.openFolder("Open a folder", null);

        if (f != null) {
            folderManagerService.addFolder(f);

        }
    }

    @FXML
    public void removeFolder() {
        folderManagerService.removeFolder(folderListView.getSelectionModel().getSelectedItem());
    }

    @EventHandler
    public void onFolderAdded(FolderAddedEvent event) {
        Platform.runLater(this::updateFolderList);
    }

    @EventHandler
    public void onFolderDeleted(FolderDeletedEvent event) {
        Platform.runLater(this::updateFolderList);
    }

    @EventHandler
    public void onExploredItemListChanged(ExploredListChanged event) {
        Platform.runLater(this::updateFilters);
        hintService.displayHints("/ijfx/ui/explorer/ExplorerActivity-tutorial-2.hints.json", false);
    }

    @EventHandler
    public void onDisplayedItemListChanged(DisplayedListChanged event) {
        Platform.runLater(() -> updateUi(event.getObject()));

    }

    @EventHandler
    public void onFolderUpdated(FolderUpdatedEvent event) {
        logger.info("Folder updated !");
        folderUpdateHandler.forEach(handler -> handler.run());
    }

    private class FolderListCell extends ListCell<Folder> {

        FolderListCellCtrl ctrl = new FolderListCellCtrl();

        public FolderListCell() {
            super();
            getStyleClass().add("selectable");
            itemProperty().addListener(this::onItemChanged);
        }

        public void onItemChanged(Observable obs, Folder oldValue, Folder newValue) {
            if (newValue == null) {
                setGraphic(null);
            } else {

                setGraphic(ctrl);

                ctrl.setItem(newValue);
            }
        }

        public void update() {

            Platform.runLater(ctrl::forceUpdate);
        }
    }

    private ListCell<Folder> createFolderListCell(ListView<Folder> listView) {
        FolderListCell cell = new FolderListCell();
        folderUpdateHandler.add(cell::update);
        return cell;
    }

    List<? extends MetaDataOwnerFilter> currentFilters = new ArrayList<>();

    public void updateFilters() {

        Task task = new CallbackTask<List<? extends Explorable>, List<MetaDataFilterWrapper>>()
                .setInput(explorerService.getItems())
                .setName("Updating filters...")
                .run(this::generateFilter)
                .then(this::replaceFilters)
                .start();

        loadingScreenService.frontEndTask(task, true);

    }

    protected void replaceFilters(List<MetaDataFilterWrapper> collect) {

        // stop listening to the current filters
        currentFilters.forEach(this::stopListeningToFilter);

        // making the new set of filters
        currentFilters = collect;

        filterVBox.getPanes().clear();

        // adding all the filter to the filter things
        filterVBox.getPanes().addAll(
                currentFilters
                .stream()
                .map(filter -> (TitledPane) filter.getContent())
                .collect(Collectors.toList())
        );

        // start listening to the new set
        currentFilters.forEach(this::listenFilter);

    }

    public List<MetaDataFilterWrapper> generateFilter(ProgressHandler handler, List<? extends Explorable> items) {

        MetaDataFilterFactory filterFactory = new DefaultMetaDataFilterFactory();

        handler.setProgress(0.1);

        Set<String> keySet = new HashSet();
        items
                .stream()
                .filter(owner -> owner != null)
                .map(owner -> owner.getMetaDataSet().keySet())
                .forEach(keys -> keySet.addAll(keys));

        handler.setTotal(1);

        return keySet
                .stream()
                .filter(MetaData::canDisplay)
                .sorted((k1, k2) -> k1.compareTo(k2))
                .map(key -> {
                    handler.increment(0.9 / keySet.size());
                    return new MetaDataFilterWrapper(key, filterFactory.generateFilter(explorerService.getItems(), key));
                })
                .filter(filter -> filter.getContent() != null)
                .collect(Collectors.toList());
    }

    private void listenFilter(MetaDataOwnerFilter filter) {
        filter.predicateProperty().addListener(this::onFilterChanged);
    }

    private void stopListeningToFilter(MetaDataOwnerFilter filter) {
        filter.predicateProperty().removeListener(this::onFilterChanged);
    }

    private void onFilterChanged(Observable obs, Predicate<MetaDataOwner> oldValue, Predicate<MetaDataOwner> newValue) {

        Predicate<MetaDataOwner> predicate = e -> true;

        List<Predicate<MetaDataOwner>> predicateList = currentFilters
                .stream()
                .map(f -> f.predicateProperty().getValue())
                .filter(v -> v != null)
                .collect(Collectors.toList());

        if (predicateList.isEmpty() == false) {

            for (Predicate<MetaDataOwner> p : predicateList) {
                predicate = predicate.and(p);
            }

        }

        explorerService.applyFilter(predicate);

    }

    /*
        View related functions
     */
    private Tab createTab(ExplorerView view) {

        Tab tab = new Tab(view.toString(), view.getNode());
        tab.closableProperty().setValue(false);
        tab.setGraphic(view.getIcon());
        tab.setUserData(view);

        tab.setText(view.getClass().getAnnotation(Plugin.class).label());

        return tab;

    }

    private void onViewModeChanged(Observable obs, ExplorerView oldValue, ExplorerView newValue) {
        updateExplorerView(newValue);
    }

    private class MetaDataFilterWrapper implements MetaDataOwnerFilter {

        private final MetaDataOwnerFilter filter;
        private final String title;
        private final TitledPane pane;

        public MetaDataFilterWrapper(String title, MetaDataOwnerFilter filter) {
            this.filter = filter;
            this.title = title;
            if (filter != null) {
                pane = new TitledPane(title, filter.getContent());
                pane.setExpanded(false);
                pane.getStyleClass().add("explorer-filter");
                pane.getContent().getStyleClass().add("content");
            } else {
                pane = null;
            }
        }

        @Override
        public Node getContent() {
            return pane;
        }

        @Override
        public Property<Predicate<MetaDataOwner>> predicateProperty() {
            return filter.predicateProperty();
        }
    }

    /*
        FXML Action
     */
    @FXML
    public void selectAll() {
        explorerService
                .getFilteredItems()
                .stream()
                .forEach(item -> item.selectedProperty().setValue(true));
    }

    @FXML
    public void onSegmentButtonPressed() {

        uiContextService.toggleContext("segment", !uiContextService.isCurrent("segment"));
        uiContextService.toggleContext("segmentation", !uiContextService.isCurrent("segmentation"));
        uiContextService.update();

    }

    @FXML
    public void onProcessButtonPressed() {
        uiContextService.toggleContext("batch", true);
        uiContextService.update();
    }

    @FXML
    public void openSelection() {

        explorerService.openSelection();

    }

    @FXML
    public void computeStatistics() {
        folderManagerService.completeStatistics();
    }

    @FXML
    public void explainMe() {
        uiService.showDialog("Function not implemented yet.");
    }

    @FXML
    public void tellMeMore() {
        hintService.displayHints("/ijfx/ui/explorer/ExplorerActivity-tutorial-3.hints.json", true);
    }

    @FXML
    public void exportToCSV() {

        List<MetaDataSet> mList = explorerService.getItems().stream()
                .map(e -> e.getMetaDataSet())
                .collect(Collectors.toList());

        String csvFile = MetaDataSetUtils.exportToCSV(mList, ",", true, MetaDataKeyPriority.getPriority(mList.get(0)));

        File saveFile = FXUtilities.saveFileSync("Export to CSV", null, "CSV", ".csv");
        if (saveFile != null) {
            try {
                TextFileUtils.writeTextFile(saveFile, csvFile);

                uiService.showDialog("CSV File successfully saved");
            } catch (IOException ex) {
                ImageJFX.getLogger().log(Level.SEVERE, null, ex);
                uiService.showDialog("Error when saving CSV File", DialogPrompt.MessageType.ERROR_MESSAGE);
            }
        }

    }

    private void onFolderListEmptyPropertyChange(Observable obs, Boolean oldV, Boolean isEmpty) {
        if (isEmpty) {
            dragPanel.setLabel(NO_FOLDER_TEXT)
                    .setIcon(NO_FOLDER_ICON);

        }

    }

    public void onExplorerListEmptyPropertyChange(Observable obs, Boolean oldV, Boolean isEmpty) {
        if (!isEmpty) {
            contentBorderPane.setCenter(tabPane);
            // getChildren().remove(dragPanel);
        } else {
            contentBorderPane.setCenter(dragPanel);

            dragPanel.setLabel(EMPTY_FOLDER_TEXT)
                    .setIcon(EMPTY_FOLDER_ICON);

        }
    }

    public boolean isEverythingSelected() {
        if (explorerService == null) {
            return false;
        }
        return explorerService.getFilteredItems().stream().filter(item -> item.selectedProperty().getValue()).count()
                == explorerService.getFilteredItems().size();
    }

    protected void updateTextFilter(final String query) {
        if (filterTextField.getText() != null && !filterTextField.getText().equals("")) {
            explorerService.setOptionalFilter(m -> m.getMetaDataSet().get(MetaData.FILE_NAME).getStringValue().toLowerCase().contains(query.toLowerCase()));
        } else {
            explorerService.setOptionalFilter(null);
        }
    }

    /*
        Exploration Mode Toggle related methods
    
     */
    protected ExplorationMode getExplorationMode(Toggle toggle) {
        return (ExplorationMode) toggle.getUserData();
    }

    protected Toggle getToggleButton(ExplorationMode mode) {
        return explorationModeToggleGroup.getToggles().stream().filter(toggle -> toggle.getUserData() == mode).findFirst().orElse(fileModeToggleButton);
    }

    protected void onToggleSelectionChanged(Observable obs, Toggle oldValue, Toggle newValue) {
        if (newValue == null) {
            return;
        }
        folderManagerService.setExplorationMode(getExplorationMode(newValue));
    }

    @EventHandler
    protected void onExplorationModeChanged(ExplorationModeChangeEvent event) {
        explorationModeToggleGroup.selectToggle(getToggleButton(event.getObject()));
    }

    @EventHandler
    protected void onExplorerServiceSelectionChanged(ExplorerSelectionChangedEvent event) {
        if (event.getObject().size() == 0) {
            logger.info("Nothing to selected");
        } else {
            logger.info("Selected objects :  " + event.getObject().size());
        }
    }

    private static void fluentIconBinding(ButtonBase... buttons) {
        for (ButtonBase b : buttons) {
            fluenIconBinding(b);
        }
    }

    private static void fluenIconBinding(ButtonBase button) {
        fluentIconBinding(button.textProperty(), button);
    }

    private static void fluentIconBinding(StringProperty property, ButtonBase node) {

        final String initialString = property.getValue();

        property.bind(Bindings.createStringBinding(() -> {
            if (shouldHideText(node)) {
                return "";
            } else {
                return initialString;
            }
        }, node.widthProperty(), node.layoutXProperty()));

    }

    private static boolean shouldHideText(ButtonBase node) {

        return node.getWidth() < 40;
    }

    /*
        Action Menu
     */
    public List<MenuItem> initActionMenu(PluginService pluginService) {

        return pluginService
                .getPluginsOfClass(ExplorerAction.class, ExplorerAction.class)
                .stream()
                .map(this::createMenuItem)
                .collect(Collectors.toList());

    }

    public <T> MenuItem createMenuItem(PluginInfo<ExplorerAction> infos) {
        String label = infos.getLabel();
        String icon = infos.getIconPath();
        String description = infos.getDescription();

        try {
            
            
            MenuItem item = new MenuItem();
            item.setOnAction(event->{
                runAction(infos);
            });
            item.setText(label);
            item.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.valueOf(icon)));
            return item;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error when loading " + label, e);
            return null;
        }

    }

    private void runAction(PluginInfo<ExplorerAction> infos){
        try {
        ExplorerAction action = infos.createInstance();
        runAction(action);
        }
        catch(Exception e) {
            logger.log(Level.SEVERE,"Error when creating action "+infos.getLabel());
            uiService.showDialog("Error when creating action "+infos.getLabel());
        }
    }
    
    private <T> void runAction(ExplorerAction<T> action) {
        
       new CallbackTask<Void, T>()
               
                .runLongCallable(action::call)
               .submit(loadingScreenService)
                .then(action::onFinished)
                .start();
        

    }

}
