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
package ijfx.ui.project_manager.projectdisplay;

import ijfx.ui.project_manager.project.*;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import ijfx.core.project.ProjectManagerService;
import ijfx.core.project.ProjectModifierService;
import ijfx.core.project.imageDBService.PlaneDB;
import ijfx.service.thumb.ThumbService;
import ijfx.ui.context.animated.Animation;
import ijfx.ui.main.ImageJFX;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.RotateTransition;
import javafx.animation.Transition;
import javafx.beans.Observable;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import mongis.utils.ConditionList;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import mongis.utils.FXUtilities;
import mongis.utils.ListCellController;

/**
 *
 * @author Cyril MONGIS, 2015
 */
public class IconItem extends BorderPane implements ListCellController<TreeItem<? extends PlaneOrMetaData>> {

    @FXML
    private CheckBox checkBox;

    @Parameter
    Context context;

    @FXML
    private Label itemLabel;

    @Parameter
    private ThumbService thumbService;

    @Parameter
    private ProjectModifierService modifierService;

    @Parameter
    private ProjectDisplayService projectDisplayService;
    
    @Parameter
    private PlaneSelectionService planeSelectionService;
    
    @Parameter
    private ProjectManagerService projectService;
    
    private FontAwesomeIconView folderIcon;

    private FontAwesomeIconView loadingIcon;

    private TreeItem item;

    private int thumbSize = 140;

    private long lastClick = 0;
    private final long doubleClickDelay = 500;

    FolderContextMenu folderContextMenu;

   
    
    
    public IconItem(TreeItem item) {
        this();
        setItem(item);
       
    }

    public IconItem() {
        super();
        
        try {
            FXUtilities.injectFXML(this);
        } catch (IOException ex) {
            ImageJFX.getLogger();
        }

        addEventHandler(MouseEvent.MOUSE_CLICKED, this::onClick);

        checkBox.selectedProperty().addListener(this::onCheckBoxClicked);

    }

    @Override
    public void setItem(TreeItem< ? extends PlaneOrMetaData> t) {
        
            item = null; // cancelling all possible actions on the item
        
            if (t instanceof CheckBoxTreeItem) {

                CheckBoxTreeItem chItem = (CheckBoxTreeItem) t;

                checkBox.selectedProperty().setValue(chItem.selectedProperty().getValue());

            }

            if (t != null) {
                itemLabel.setText(t.getValue().toString());
                item = t;
                
                ConditionList conditionList = new ConditionList(20);
                
                TreeItemUtils.goThroughLeaves(t, child->{
                    conditionList.add(planeSelectionService.isPlaneSelected(projectService.getCurrentProject(),child.getValue().getPlaneDB()));
                });
                
                checkBox.setSelected(conditionList.isOneTrue());
                
                
            } else {

            }

            
            
            updateIcon();

            /*
            if (folderContextMenu == null && context != null) {
                folderContextMenu = new FolderContextMenu(item, projectService.getCurrentProject(), context);
                itemLabel.setContextMenu(folderContextMenu);
            }*/
            

    }

    public void updateIcon() {

        if (containsPlaneDB(getItem())) {

            getThumb(getItem());

        } else {
            if (getItem().getChildren().size() == 1) {
                TreeItem firstChild = getItemFirstChild(getItem());
                if (containsPlaneDB(firstChild)) {
                    getThumb(firstChild);
                } else {
                    setCenter(getIconNode());
                }
            } else {
                setCenter(getIconNode());
            }

        }

    }

    @Override
    public TreeItem getItem() {
        return item;
    }

    public TreeItem getItemFirstChild(TreeItem item) {
        return (TreeItem) item.getChildren().get(0);
    }

    public FontAwesomeIconView getIconNode() {
        if (folderIcon == null) {
            folderIcon = new FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN);
            folderIcon.getStyleClass().add("folder-icon");
            folderIcon.setGlyphSize(40);
            folderIcon.prefWidth(150);
        }
        return folderIcon;
    }

    public FontAwesomeIconView getLoadingNode() {
        if (loadingIcon == null) {
            loadingIcon = new FontAwesomeIconView(FontAwesomeIcon.GEAR);

            loadingIcon.getStyleClass().add("folder-icon");
            loadingIcon.setGlyphSize(40);
            loadingIcon.prefWidth(150);
            
            //Animation.QUICK_EXPAND.configure(loadingIcon, ImageJFX.getAnimationDurationAsDouble()/2).play();
        }

        return loadingIcon;
    }

    public FontAwesomeIconView generateBigIcon(FontAwesomeIcon icon) {
        FontAwesomeIconView view = new FontAwesomeIconView(icon);
        view.getStyleClass().add("folder-icon");
        view.setGlyphSize(40);
        view.prefWidth(150);
        return view;
    }

    public PlaneDB getItemPlaneDB(TreeItem<PlaneOrMetaData> item) {
        return item.getValue().getPlaneDB();
    }

    public boolean containsPlaneDB(TreeItem<PlaneOrMetaData> item) {
        return item.getValue().isPlane();
    }

    private void updateSelection(TreeItem item, boolean value) {
        
      
        ((CheckBoxTreeItem) item).setSelected(value);
    }

    public void getThumb(final TreeItem<PlaneOrMetaData> item) {

        Task<Image> task = new Task<Image>() {
            public Image call() {

                try {

                    PlaneDB plane = item.getValue().getPlaneDB();

                    return thumbService.getThumb(plane.getFile(), (int) plane.getPlaneIndex(), thumbSize, thumbSize);
                } catch (IOException ex) {
                    ImageJFX.getLogger();
                }
                return null;
            }

        };
        task.setOnSucceeded(image -> {

            ImageView imageView = new ImageView();
            imageView.setImage(task.getValue());

            
                setCenter(imageView);
                Transition tr2 = Animation.FADEIN.configure(imageView, ImageJFX.getAnimationDurationAsDouble() / 2);
                
        });

        task.setOnFailed(event -> {

            setCenter(generateBigIcon(FontAwesomeIcon.REMOVE));
        });
        setCenter(getLoadingNode());
        ImageJFX.getThreadPool().submit(task);

    }

    /*   Double click related methods */
    public void onClick(MouseEvent event) {

        double clickInterval = lastClick - updateLastClick();
        clickInterval *= -1;

        if (clickInterval < doubleClickDelay) {
            
            System.out.println("Double click");
            //viewModelService.getViewModel(projectService.currentProjectProperty().getValue()).setCurrent((CheckBoxTreeItem) item);
            System.out.println(projectDisplayService.getProjectDisplay(projectService.getCurrentProject()).getCurrentPlaneSet().getName());
            projectDisplayService.getProjectDisplay(projectService.getCurrentProject()).getCurrentPlaneSet().setCurrentItem((ProjectTreeItem)item);
        }

    }

    public long updateLastClick() {
        lastClick = System.currentTimeMillis();
        return lastClick;
    }

    /* Checkbox related methods */
    public void onCheckBoxClicked(Observable event, Boolean oldValue, Boolean newValue) {
      
        if(item == null) return;
        planeSelectionService.setPlaneSelection(projectService.getCurrentProject(), item, newValue);
        
        
        
    }
    
    

}
