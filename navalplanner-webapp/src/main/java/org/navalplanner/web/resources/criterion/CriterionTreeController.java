/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.resources.criterion;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static org.navalplanner.web.I18nHelper._;
import org.apache.commons.lang.Validate;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.TreeModel;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.api.Tree;

public class CriterionTreeController extends GenericForwardComposer {

    private Tree tree;

    private CriterionTreeitemRenderer renderer = new CriterionTreeitemRenderer();

    private TreeViewStateSnapshot snapshotOfOpenedNodes;

    private Component messagesContainer;

    private IMessagesForUser messagesForUser;

    private final ICriterionsModel_V2 criterionsModel;

    private Component vbox;

    public CriterionTreeitemRenderer getRenderer() {
        return renderer;
    }

    public CriterionTreeController(ICriterionsModel_V2 _criterionsModel) {
        Validate.notNull(_criterionsModel);
        this.criterionsModel = _criterionsModel;
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        messagesForUser = new MessagesForUser(messagesContainer);
        comp.setVariable("criterionTreeController", this, true);
        this.vbox = comp;
    }

    public TreeModel getCriterionTreeModel() {
        if (getModel() == null) {
            return null;
        }
        return getModel().asTree();
    }

    private ICriterionTreeModel getModel() {
        return criterionsModel.getCriterionTreeModel();
    }

    public class CriterionTreeitemRenderer implements TreeitemRenderer {

        public CriterionTreeitemRenderer() {
        }

        @Override
        public void render(Treeitem item, Object data) throws Exception {
            final CriterionDTO criterionForThisRow = (CriterionDTO) data;
            item.setValue(data);

            if (snapshotOfOpenedNodes != null) {
                snapshotOfOpenedNodes.openIfRequired(item);
            }

            Treecell cellForName = new Treecell();
            cellForName.appendChild(Util.bind(new Textbox(),
                    new Util.Getter<String>() {

                        @Override
                        public String get() {
                            return criterionForThisRow.getName();
                        }
                    }, new Util.Setter<String>() {

                        @Override
                        public void set(String value) {
                            criterionForThisRow.setName(value);
                        }
                    }));


            Treecell cellForActive = new Treecell();
            cellForActive.setStyle("center");
            Checkbox checkboxActive = new Checkbox();
            cellForActive.appendChild(Util.bind(checkboxActive,
                    new Util.Getter<Boolean>() {

                        @Override
                        public Boolean get() {
                            return criterionForThisRow.isActive();
                        }
                    }, new Util.Setter<Boolean>() {

                        @Override
                        public void set(Boolean value) {
                            criterionForThisRow.setActive(value);
                        }
                    }));

            checkboxActive.addEventListener(Events.ON_CHECK,new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    getModel().updateEnabledCriterions(criterionForThisRow.isActive(),criterionForThisRow);
                    reloadTree();
                }
            });

            Treerow tr = null;
            /*
             * Since only one treerow is allowed, if treerow is not null, append
             * treecells to it. If treerow is null, contruct a new treerow and
             * attach it to item.
             */
            if (item.getTreerow() == null) {
                tr = new Treerow();
                tr.setParent(item);
            } else {
                tr = item.getTreerow();
                tr.getChildren().clear();
            }
            // Attach treecells to treerow
            tr.setDraggable("true");
            tr.setDroppable("true");

            cellForName.setParent(tr);
            cellForActive.setParent(tr);

            Treecell tcOperations = new Treecell();
            Button upbutton = new Button("", "/common/img/ico_bajar1.png");
            upbutton.setHoverImage("/common/img/ico_bajar.png");
            upbutton.setParent(tcOperations);
            upbutton.setSclass("icono");
            upbutton.addEventListener(Events.ON_CLICK, new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    getModel().down(criterionForThisRow);
                    reloadTree();
                }
            });

            Button downbutton = new Button("", "/common/img/ico_subir1.png");
            downbutton.setHoverImage("/common/img/ico_subir.png");
            downbutton.setParent(tcOperations);
            downbutton.setSclass("icono");
            downbutton.addEventListener(Events.ON_CLICK, new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    getModel().up(criterionForThisRow);
                    reloadTree();
                }
            });

            Button indentbutton = new Button("", "/common/img/ico_derecha1.png");
            indentbutton.setHoverImage("/common/img/ico_derecha.png");
            indentbutton.setParent(tcOperations);
            indentbutton.setSclass("icono");
            indentbutton.addEventListener(Events.ON_CLICK, new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    getModel().indent(criterionForThisRow);
                    reloadTree();
                }
            });

            Button unindentbutton = new Button("", "/common/img/ico_izq1.png");
            unindentbutton.setHoverImage("/common/img/ico_izq.png");
            unindentbutton.setParent(tcOperations);
            unindentbutton.setSclass("icono");
            unindentbutton.addEventListener(Events.ON_CLICK,
                    new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            getModel().unindent(criterionForThisRow);
                            reloadTree();
                        }
                    });

            Button removebutton = createButtonRemove(criterionForThisRow);
            removebutton.setParent(tcOperations);
            if(criterionForThisRow.isNewObject()){
            removebutton.addEventListener(Events.ON_CLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                getModel().removeNode(criterionForThisRow);
                reloadTree();
                }
            });
        }

            tcOperations.setParent(tr);
            tr.addEventListener("onDrop", new EventListener() {

                @Override
                public void onEvent(org.zkoss.zk.ui.event.Event arg0)
                        throws Exception {
                    DropEvent dropEvent = (DropEvent) arg0;
                    move((Component) dropEvent.getTarget(),
                            (Component) dropEvent.getDragged());
                }
            });
        }
    }

    private Button createButtonRemove(CriterionDTO criterion){
        String urlIcono;
        String urlHoverImage;
        String toolTipText;
        if(criterion.isNewObject()){
            urlIcono = "/common/img/ico_borrar1.png";
            urlHoverImage = "/common/img/ico_borrar.png";
            toolTipText = "Delete";
        }else{
            urlIcono = "/common/img/ico_borrar_out.png";
            urlHoverImage = "/common/img/ico_borrar.png";
            toolTipText = "Not deletable";
        }
        Button removebutton = new Button("", urlIcono);
        removebutton.setHoverImage(urlHoverImage);
        removebutton.setSclass("icono");
        removebutton.setTooltiptext(_(toolTipText));
        return removebutton;
    }

    public void up() {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        if (tree.getSelectedCount() == 1) {
            getModel().up(getSelectedNode());
        }
    }

    public void down() {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        if (tree.getSelectedCount() == 1) {
            getModel().down(getSelectedNode());
        }
    }

    public void move(Component dropedIn, Component dragged) {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        Treerow from = (Treerow) dragged;
        CriterionDTO fromNode = (CriterionDTO) ((Treeitem) from.getParent())
                .getValue();
        if (dropedIn instanceof Tree) {
            getModel().moveToRoot(fromNode,0);
        }
        if (dropedIn instanceof Treerow) {
            Treerow to = (Treerow) dropedIn;
            CriterionDTO toNode = (CriterionDTO) ((Treeitem) to.getParent())
                    .getValue();

            getModel().move(fromNode, toNode,0);
        }
        reloadTree();
    }

    public void addCriterion() {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        try {
            if((tree.getSelectedCount() == 1)
                && (this.criterionsModel.getAllowHierarchy())){
                getModel().addCriterionAt(getSelectedNode(),getName());
            } else {
                getModel().addCriterion(getName());
            }
            reloadTree();
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e);
        }
    }

    public void reloadTree(){
        Util.reloadBindings(tree);
    }

    private String getName() throws ValidationException{
        String name = ((Textbox)((Vbox)vbox).
                getFellow("criterionName")).getValue();
        getModel().thereIsOtherWithSameNameAndType(name);
        getModel().validateNameNotEmpty(name);
        return name;
    }

    private CriterionDTO getSelectedNode() {
        return (CriterionDTO) tree.getSelectedItemApi().getValue();
    }

    private static class TreeViewStateSnapshot {
        private final Set<Object> all;
        private final Set<Object> dataOpen;

        private TreeViewStateSnapshot(Set<Object> dataOpen, Set<Object> all) {
            this.dataOpen = dataOpen;
            this.all = all;
        }

        public static TreeViewStateSnapshot snapshotOpened(Tree tree) {
            Iterator<Treeitem> itemsIterator = tree.getTreechildrenApi()
                    .getItems().iterator();
            Set<Object> dataOpen = new HashSet<Object>();
            Set<Object> all = new HashSet<Object>();
            while (itemsIterator.hasNext()) {
                Treeitem treeitem = (Treeitem) itemsIterator.next();
                Object value = getAssociatedValue(treeitem);
                if (treeitem.isOpen()) {
                    dataOpen.add(value);
                }
                all.add(value);
            }
            return new TreeViewStateSnapshot(dataOpen, all);
        }

        private static Object getAssociatedValue(Treeitem treeitem) {
            return treeitem.getValue();
        }

        public void openIfRequired(Treeitem item) {
            Object value = getAssociatedValue(item);
            item.setOpen(isNewlyCreated(value) || wasOpened(value));
        }

        private boolean wasOpened(Object value) {
            return dataOpen.contains(value);
        }

        private boolean isNewlyCreated(Object value) {
            return !all.contains(value);
        }
    }
}
