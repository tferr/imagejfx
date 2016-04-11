/*
 * /*
 *     This file is part of ImageJ FX.
 *
 *     ImageJ FX is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ImageJ FX is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
 *
 * 	Copyright 2015,2016 Cyril MONGIS, Michael Knop
 *
 */
package ijfx.ui.plugin;

import ijfx.ui.UiPlugin;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import ijfx.ui.main.ImageJFX;
import ijfx.ui.main.Localization;
import ijfx.ui.notification.NotificationService;
import ijfx.service.ui.AppService;
import ijfx.service.ui.HintService;
import java.io.File;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import ijfx.ui.UiConfiguration;
import ijfx.service.uiplugin.UiPluginService;
import ijfx.ui.activity.ActivityService;
import ijfx.ui.batch.FileBatchProcessorPanel;
import ijfx.ui.main.PerformanceActivity;
import java.util.List;
import java.util.stream.Collectors;
import javafx.event.Event;
import javafx.scene.control.Menu;
import javafx.scene.input.MouseEvent;

import org.scijava.event.EventService;

/**
 *
 * @author Cyril MONGIS, 2015
 */
@Plugin(type = UiPlugin.class)
@UiConfiguration(id = "debug-button", context = "debug", order = 4.0, localization = Localization.TOP_RIGHT)
public class DebugButton extends MenuButton implements UiPlugin {

    @Parameter
    private UiPluginService uiPluginService;

    @Parameter
    private NotificationService notificationService;

    @Parameter
    private EventService eventService;

    @Parameter
    private AppService appService;

    @Parameter
    private HintService hintService;

    @Parameter
    private ActivityService activityService;

    Menu reloadMenu = new Menu("Reload UiPlugin");
    Menu activityMenu = new Menu("Show activity");

    public DebugButton() {
        super("D", GlyphsDude.createIcon(FontAwesomeIcon.BUG));
        getStyleClass().add("icon");
        addItem("Reload CSS", this::reloadCss);
        addItem("Reload Debug Button", event -> uiPluginService.reload(DebugButton.class));
        addItem("Reload App Browser", event -> appService.reloadCurrentView());
        addItem("Test hints", this::testHints);
        getItems().add(reloadMenu);
        addEventHandler(MouseEvent.MOUSE_ENTERED,this::updateReloadMenu);
        System.out.println("Added");
    }

    @Override
    public Node getUiElement() {

        return this;
    }

    public void addItem(String label, EventHandler<ActionEvent> handler) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(handler);
        getItems().add(item);
    }

    @Override
    public UiPlugin init() {

        return this;
    }

    public void triggerDebugEvent(ActionEvent event) {
        eventService.publish(new DebugEvent("sideMenu"));
    }

    public void reloadCss(ActionEvent event) {

        String debugStyleSheet = "file:./src/main/resources/ijfx/ui/main/flatterfx.css";

        if (new File(debugStyleSheet.replace("file:", "")).exists() == false) {

        } else {

        }

        getScene().getStylesheets().removeAll(ImageJFX.STYLESHEET_ADDR, debugStyleSheet);

        getScene().getStylesheets().add(debugStyleSheet);

        //eventService.publish(new DebugEvent("reloadSideMenu"));
    }

    public void reloadAnOther(ActionEvent event) {
        activityService.openByType(PerformanceActivity.class);
    }

    public void testHints(ActionEvent event) {
        hintService.displayHints(FileBatchProcessorPanel.class, true);
    }

    private void reloadPlugin(ActionEvent event) {
        MenuItem mi = (MenuItem) event.getTarget();
        UiPlugin ui = (UiPlugin) mi.getUserData();
        uiPluginService.reload(ui.getClass());
    }

    private void updateReloadMenu(MouseEvent event) {
        System.out.println("Showing");
        if (reloadMenu.getItems().size() <= 0) {
            System.out.println("Reloading");
            List<MenuItem> items = uiPluginService
                    .getUiPluginList()
                    .parallelStream()
                    .map(p -> {
                        MenuItem mi = new MenuItem(p.getClass().getSimpleName());
                        mi.setUserData(p);
                        mi.setOnAction(this::reloadPlugin);
                        return mi;
                    })
                    .sorted((o1, o2) -> o1.getText().compareTo(o2.getText()))
                    .collect(Collectors.toList());
            reloadMenu.getItems().addAll(items);
        }
    }

}
