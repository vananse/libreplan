/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
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

package org.libreplan.web.orders;


import static org.libreplan.web.I18nHelper._;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalDate;
import org.libreplan.business.advance.bootstrap.PredefinedAdvancedTypes;
import org.libreplan.business.advance.entities.AdvanceAssignment;
import org.libreplan.business.advance.entities.AdvanceMeasurement;
import org.libreplan.business.advance.entities.AdvanceType;
import org.libreplan.business.advance.entities.DirectAdvanceAssignment;
import org.libreplan.business.advance.entities.IndirectAdvanceAssignment;
import org.libreplan.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.libreplan.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.web.common.IMessagesForUser;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.MessagesForUser;
import org.libreplan.web.common.Util;
import org.zkoss.util.InvalidValueException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Chart;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.XYModel;

/**
 * Controller for show the advances of the selected order element<br />
 * @author Susana Montes Pedreria <smontes@wirelessgalicia.com>
 */

public class ManageOrderElementAdvancesController extends
        GenericForwardComposer {

    private static final Log LOG = LogFactory .getLog(ManageOrderElementAdvancesController.class);

    private IMessagesForUser messagesForUser;

    private int indexSelectedItem = -1;

    private IManageOrderElementAdvancesModel manageOrderElementAdvancesModel;

    private AdvanceTypeListRenderer advanceTypeListRenderer = new AdvanceTypeListRenderer();

    private AdvanceMeasurementRenderer advanceMeasurementRenderer = new AdvanceMeasurementRenderer();

    private Set<AdvanceAssignment> selectedAdvances = new HashSet<AdvanceAssignment>();

    private Component messagesContainerAdvances;

    private Tabbox tabboxOrderElement;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setVariable("manageOrderElementAdvancesController", this, true);
        messagesForUser = new MessagesForUser(messagesContainerAdvances);
    }

    public List<AdvanceMeasurement> getAdvanceMeasurements() {
        List<AdvanceMeasurement> measurements = manageOrderElementAdvancesModel
                .getAdvanceMeasurements();
        Collections.reverse(measurements);
        return measurements;
    }

    public List<AdvanceAssignment> getAdvanceAssignments() {
        return manageOrderElementAdvancesModel.getAdvanceAssignments();
    }

    public boolean close()  {
        return save();
    }

    private void validate() throws InvalidValueException {
        if (!validateDataForm()) {
            throw new InvalidValueException(_("values are not valid, the values must not be null"));
        }
        if (!validateReportGlobalAdvance()) {
            throw new InvalidValueException(_("spread values are not valid, at least one value should be true"));
        }
    }

    public boolean save() {
        try {
            validate();
            manageOrderElementAdvancesModel.confirmSave();
            return true;
        } catch (DuplicateAdvanceAssignmentForOrderElementException e) {
            messagesForUser.showMessage(Level.ERROR, _("cannot include a progress of the same progress type twice"));
        } catch (DuplicateValueTrueReportGlobalAdvanceException e) {
            messagesForUser.showMessage(
                    Level.ERROR, _("spread values are not valid, at least one value should be true"));
        } catch (InvalidValueException e) {
            messagesForUser.showMessage(Level.ERROR, e.getMessage());
        } catch (InstanceNotFoundException e) {
            messagesForUser.showMessage(
                    Level.ERROR, e.getMessage());
            LOG.error(_("Couldn't find element: {0}", e.getKey()), e);
        }
        increaseScreenHeight();
        return false;
    }

    private IOrderElementModel orderElementModel;

    public void openWindow(IOrderElementModel orderElementModel) {
        setOrderElementModel(orderElementModel);
        manageOrderElementAdvancesModel.initEdit(getOrderElement());
        selectedAdvances.clear();
        selectedAdvances.addAll(getAdvanceAssignments());
        createAndLoadBindings();
        selectSpreadAdvanceLine();
    }

    public void openWindow(OrderElement orderElement) {
        manageOrderElementAdvancesModel.initEdit(orderElement);
        selectedAdvances.clear();
        selectedAdvances.addAll(getAdvanceAssignments());
        createAndLoadBindings();
        selectSpreadAdvanceLine();
    }

    public void createAndLoadBindings() {
        Util.createBindingsFor(self);
        Util.reloadBindings(self);
    }

    public void setOrderElementModel(IOrderElementModel orderElementModel) {
        this.orderElementModel = orderElementModel;
    }

    private OrderElement getOrderElement() {
        return orderElementModel.getOrderElement();
    }

    private void increaseScreenHeight() {
        if ((tabboxOrderElement != null)
                && (tabboxOrderElement.getHeight() == null || !tabboxOrderElement
                        .getHeight().equals("680px"))) {
            tabboxOrderElement.setHeight("680px");
            tabboxOrderElement.invalidate();
        }
    }

    private void reloadAdvances() {
        Util.reloadBindings(self);
        setSelectedAdvanceLine();
    }

    private void setSelectedAdvanceLine() {
        if ((indexSelectedItem > -1)
                && (indexSelectedItem < editAdvances.getItemCount())) {
            editAdvances.setSelectedItem(editAdvances
                .getItemAtIndex(indexSelectedItem));
            editAdvances.invalidate();
        }
    }

    private Listbox editAdvances;

    public void selectAdvanceLine(Listitem selectedItem) {
        /*
         * validate the previous advance line before changing the selected
         * advance.
         */
        setSelectedAdvanceLine();
        findErrorsInMeasurements();

        /*
         * preparation to select the advance line. Set the current selected
         * index that will show when the grid reloads.
         */
        if (selectedItem != null) {
            AdvanceAssignment advance = (AdvanceAssignment) selectedItem
                    .getValue();
            indexSelectedItem = editAdvances.getIndexOfItem(selectedItem);
            prepareEditAdvanceMeasurements(advance);
            reloadAdvances();
        }
    }

    public void selectAdvanceLine(int index) {
        indexSelectedItem = index;
        if ((indexSelectedItem >= 0)
                && (indexSelectedItem < getAdvanceAssignments().size())) {
            prepareEditAdvanceMeasurements(getAdvanceAssignments().get(
                indexSelectedItem));
        }
        reloadAdvances();
    }

    public void selectSpreadAdvanceLine() {
        AdvanceAssignment advance = manageOrderElementAdvancesModel
                .getSpreadAdvance();
        if (advance != null) {
            indexSelectedItem = getAdvanceAssignments().indexOf(advance);
            prepareEditAdvanceMeasurements(advance);
        } else {
            selectAdvanceLine(getAdvanceAssignments().size() - 1);
        }
        reloadAdvances();
    }

    public void prepareEditAdvanceMeasurements(AdvanceAssignment advance) {
        if (advance != null && advance.getAdvanceType() != null) {
            manageOrderElementAdvancesModel
                    .prepareEditAdvanceMeasurements(advance);
        }
    }

    public void goToCreateLineAdvanceAssignment() {
        findErrorsInMeasurements();
        boolean fineResult = manageOrderElementAdvancesModel
                .addNewLineAdvanceAssignment();
        if (fineResult) {
            int position = getAdvanceAssignments().size() - 1;
            selectAdvanceLine(position);
            selectedAdvances.add(getAdvanceAssignments().get(position));
        } else {
            showMessageNotAddMoreAdvances();
        }
    }

    public void goToCreateLineAdvanceMeasurement() {
        AdvanceMeasurement newMeasure = manageOrderElementAdvancesModel
                .addNewLineAdvaceMeasurement();
        if ((newMeasure != null)
                && (manageOrderElementAdvancesModel
                        .hasConsolidatedAdvances(newMeasure
                                .getAdvanceAssignment()))) {
            newMeasure.setDate(null);
        }
        reloadAdvances();
    }

    public void goToRemoveLineAdvanceAssignment(Listitem listItem) {
        AdvanceAssignment advance = (AdvanceAssignment) listItem.getValue();
        if ((editAdvances.getItemCount() > 1)
                && (advance.getReportGlobalAdvance())) {
            showMessageDeleteSpread();
        } else if (manageOrderElementAdvancesModel
                .hasConsolidatedAdvances(advance)) {
            showErrorMessage(_("This progress assignment cannot be deleted or changed because it has some progress consolidation"));
        } else {
            manageOrderElementAdvancesModel
                    .removeLineAdvanceAssignment(advance);
            selectedAdvances.remove(advance);
            if (indexSelectedItem == editAdvances.getIndexOfItem(listItem)) {
                selectSpreadAdvanceLine();
            } else {
                if (indexSelectedItem > editAdvances.getIndexOfItem(listItem)) {
                    selectAdvanceLine(indexSelectedItem - 1);
                } else {
                    prepareEditAdvanceMeasurements(getAdvanceAssignments().get(
                            indexSelectedItem));
                    reloadAdvances();
                }
            }
        }
    }

    private Listbox editAdvancesMeasurement;

    public void goToRemoveLineAdvanceMeasurement(Listitem listItem) {
        AdvanceMeasurement advance = (AdvanceMeasurement) listItem.getValue();
        if (manageOrderElementAdvancesModel.canRemoveOrChange(advance)) {
            manageOrderElementAdvancesModel
                    .removeLineAdvanceMeasurement(advance);
            reloadAdvances();
        } else {
            showErrorMessage(_("This progress measurement cannot be deleted or changed because it is consolidated"));
        }
    }

    public String getInfoAdvance() {
        String infoAdvanceAssignment = manageOrderElementAdvancesModel
                .getInfoAdvanceAssignment();
        if (infoAdvanceAssignment.isEmpty()) {
            return _("Progress measurements");
        }

        return _("Progress measurements") + ": " + infoAdvanceAssignment;
    }

    public boolean isReadOnlyAdvanceMeasurements() {
       return manageOrderElementAdvancesModel.isReadOnlyAdvanceMeasurements();
    }

    public AdvanceTypeListRenderer getAdvancesRenderer() {
        return advanceTypeListRenderer;
    }

    public void updatesValue(final Decimalbox item){
        this.setPercentage();
        this.setCurrentValue();
    }

    public class AdvanceTypeListRenderer implements ListitemRenderer {
         @Override
        public void render(Listitem listItem, Object data) {
            final AdvanceAssignment advance = (AdvanceAssignment) data;
            listItem.setValue(advance);

            boolean isQualityForm = false;
            if (advance.getAdvanceType() != null) {
                isQualityForm = manageOrderElementAdvancesModel
                        .isQualityForm(advance);
            }

            if ((advance instanceof DirectAdvanceAssignment)
                    && ((DirectAdvanceAssignment) advance)
                            .getAdvanceMeasurements().isEmpty()
                    && !isQualityForm) {
                appendComboboxAdvanceType(listItem);
            } else {
                appendLabelAdvanceType(listItem);
            }
            appendDecimalBoxMaxValue(listItem, isQualityForm);
            appendDecimalBoxValue(listItem);
            appendLabelPercentage(listItem);
            appendDateBoxDate(listItem);
            appendRadioSpread(listItem);
            appendCalculatedCheckbox(listItem);
            appendChartCheckbox(listItem);
            appendOperations(listItem);
        }
    }

    private void appendComboboxAdvanceType(final Listitem listItem) {
        final DirectAdvanceAssignment advance = (DirectAdvanceAssignment) listItem
                .getValue();
        final Combobox comboAdvanceTypes = new Combobox();
        final List<AdvanceType> listAdvanceType = manageOrderElementAdvancesModel
                .getPossibleAdvanceTypes(advance);

        for(AdvanceType advanceType : listAdvanceType){
            if (!advanceType.getUnitName().equals(
                    PredefinedAdvancedTypes.CHILDREN.getTypeName())
                    && !advanceType.isQualityForm()) {
                Comboitem comboItem = new Comboitem();
                comboItem.setValue(advanceType);
                comboItem.setLabel(advanceType.getUnitName());
                comboItem.setParent(comboAdvanceTypes);

                if ((advance.getAdvanceType() != null)
                    && (advance.getAdvanceType().getId().equals(advanceType
                                .getId()))) {
                    comboAdvanceTypes.setSelectedItem(comboItem);
                }
            }
        }

        comboAdvanceTypes.addEventListener(Events.ON_SELECT,
                new EventListener() {
                    @Override
                    public void onEvent(Event event) {
                        setMaxValue(listItem, comboAdvanceTypes);
                        cleanFields(advance);
                        setPercentage();
                        reloadAdvances();
                    }
        });

        Util.bind(comboAdvanceTypes,
                    new Util.Getter<Comboitem>() {
                        @Override
                        public Comboitem get(){
                                return comboAdvanceTypes.getSelectedItem();
                        }
                    }, new Util.Setter<Comboitem>() {
                @Override
            public void set(Comboitem comboItem) {
                            if(((comboItem!=null))&&(comboItem.getValue() != null)&&
                                    (comboItem.getValue() instanceof AdvanceType)){
                                AdvanceType advanceType = (AdvanceType)comboItem.getValue();
                                advance.setAdvanceType(advanceType);
                    advance.setMaxValue(manageOrderElementAdvancesModel
                            .getMaxValue(advanceType));
                            }
                        }

                    });
        Listcell listCell = new Listcell();
        listCell.appendChild(comboAdvanceTypes);
        listItem.appendChild(listCell);
    }



    private void appendLabelAdvanceType(final Listitem listItem){
        final AdvanceAssignment advance = (AdvanceAssignment) listItem.getValue();
        Label unitName = new Label(advance.getAdvanceType().getUnitName());
        Listcell listCell = new Listcell();
        listCell.appendChild(unitName);
        listItem.appendChild(listCell);
    }

    private void appendDecimalBoxMaxValue(final Listitem listItem,
            boolean isQualityForm) {
        final AdvanceAssignment advanceAssignment = (AdvanceAssignment) listItem
                .getValue();
        final Decimalbox maxValue = new Decimalbox();
        maxValue.setScale(2);

        final DirectAdvanceAssignment directAdvanceAssignment;
        if ((advanceAssignment instanceof IndirectAdvanceAssignment)
                || isQualityForm
                || (advanceAssignment.getAdvanceType() != null && advanceAssignment
                        .getAdvanceType().getPercentage())
                || manageOrderElementAdvancesModel
                        .hasConsolidatedAdvances(advanceAssignment)) {
            maxValue.setDisabled(true);
        }
        if (advanceAssignment instanceof IndirectAdvanceAssignment) {
            directAdvanceAssignment = manageOrderElementAdvancesModel
                    .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
        } else {
            directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
        }

        Util.bind(maxValue, new Util.Getter<BigDecimal>() {
            @Override
            public BigDecimal get() {
                return directAdvanceAssignment.getMaxValue();
            }
        }, new Util.Setter<BigDecimal>() {

            @Override
            public void set(BigDecimal value) {
                if (!manageOrderElementAdvancesModel
                        .hasConsolidatedAdvances(advanceAssignment)) {
                    directAdvanceAssignment.setMaxValue(value);
                }
            }
        });
        maxValue.addEventListener(Events.ON_CHANGE,
                new EventListener() {
                    @Override
            public void onEvent(Event event) {
                if (manageOrderElementAdvancesModel
                        .hasConsolidatedAdvances(advanceAssignment)) {
                    throw new WrongValueException(
                            maxValue,
                            _("This progress assignment cannot be deleted or changed because it has some progress consolidation"));
                } else {
                    setPercentage();
                    reloadAdvances();
                }
                    }
                });

        Listcell listCell = new Listcell();
        listCell.appendChild(maxValue);
        listItem.appendChild(listCell);
        maxValue.setConstraint(checkMaxValue());
    }

    private void appendDecimalBoxValue(final Listitem listItem){
        final AdvanceAssignment advanceAssignment = (AdvanceAssignment) listItem
                .getValue();
        Decimalbox value = new Decimalbox();
        value.setScale(2);
        value.setDisabled(true);

        DirectAdvanceAssignment directAdvanceAssignment;
        if (advanceAssignment instanceof IndirectAdvanceAssignment) {
            directAdvanceAssignment = manageOrderElementAdvancesModel
                    .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
        } else {
            directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
        }

        final AdvanceMeasurement advanceMeasurement = this.manageOrderElementAdvancesModel
                .getLastAdvanceMeasurement(directAdvanceAssignment);
        if (advanceMeasurement != null) {
            Util.bind(value, new Util.Getter<BigDecimal>() {
                @Override
                public BigDecimal get() {
                    return advanceMeasurement.getValue();
                }
            });
        }
        Listcell listCell = new Listcell();
        listCell.appendChild(value);
        listItem.appendChild(listCell);
    }

    private void appendLabelPercentage(final Listitem listItem){
        final AdvanceAssignment advanceAssignment = (AdvanceAssignment) listItem
                .getValue();
        Label percentage = new Label();

        DirectAdvanceAssignment directAdvanceAssignment;
        if (advanceAssignment instanceof IndirectAdvanceAssignment) {
            directAdvanceAssignment = manageOrderElementAdvancesModel
                    .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
        } else {
            directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
        }
        final AdvanceMeasurement advanceMeasurement = this.manageOrderElementAdvancesModel
                .getLastAdvanceMeasurement(directAdvanceAssignment);
        if (advanceMeasurement != null) {
            percentage
                    .setValue(this.manageOrderElementAdvancesModel
                    .getPercentageAdvanceMeasurement(advanceMeasurement)
                    .toString()
                    + " %");
        }

        Listcell listCell = new Listcell();
        listCell.appendChild(percentage);
        listItem.appendChild(listCell);
    }

    private void appendDateBoxDate(final Listitem listItem){
        final AdvanceAssignment advanceAssignment = (AdvanceAssignment) listItem
                .getValue();
        Datebox date = new Datebox();
        date.setDisabled(true);

        DirectAdvanceAssignment directAdvanceAssignment;
        if (advanceAssignment instanceof IndirectAdvanceAssignment) {
            directAdvanceAssignment = manageOrderElementAdvancesModel
                    .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
        } else {
            directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
        }
        final AdvanceMeasurement advanceMeasurement = this.manageOrderElementAdvancesModel
                .getLastAdvanceMeasurement(directAdvanceAssignment);
        if (advanceMeasurement != null) {

            Util.bind(date, new Util.Getter<Date>() {
                @Override
                public Date get() {
                    if (advanceMeasurement.getDate() == null) {
                        return null;
                    }
                    return advanceMeasurement.getDate()
                            .toDateTimeAtStartOfDay().toDate();
                }
            });
        }
        Listcell listCell = new Listcell();
        listCell.appendChild(date);
        listItem.appendChild(listCell);
    }

    private void appendRadioSpread(final Listitem listItem){
        final AdvanceAssignment advanceAssignment = (AdvanceAssignment) listItem
                .getValue();

        final Radio reportGlobalAdvance = Util.bind(new Radio(),
                new Util.Getter<Boolean>() {

                    @Override
                    public Boolean get() {
                        return advanceAssignment.getReportGlobalAdvance();
                    }
                }, new Util.Setter<Boolean>() {

                    @Override
                    public void set(Boolean value) {
                        advanceAssignment.setReportGlobalAdvance(value);
                        setReportGlobalAdvance(listItem);
                    }
                });

        Listcell listCell = new Listcell();
        listCell.appendChild(reportGlobalAdvance);
        listItem.appendChild(listCell);

        if (((AdvanceAssignment) listItem.getValue()).getReportGlobalAdvance()) {
            reportGlobalAdvance.getRadiogroup().setSelectedItem(
                    reportGlobalAdvance);
            reportGlobalAdvance.getRadiogroup().invalidate();
        }
    }

    private void appendCalculatedCheckbox(final Listitem listItem){
        final AdvanceAssignment advance = (AdvanceAssignment) listItem.getValue();
        Checkbox calculated = new Checkbox();
        boolean isCalculated = advance instanceof IndirectAdvanceAssignment;
        calculated.setChecked(isCalculated);
        calculated.setDisabled(true);

        Listcell listCell = new Listcell();
        listCell.appendChild(calculated);
        listItem.appendChild(listCell);
    }

    private void appendChartCheckbox(final Listitem listItem) {
        final AdvanceAssignment advance = (AdvanceAssignment) listItem
                .getValue();
        final Checkbox chartCheckbox = new Checkbox();

        chartCheckbox.setChecked(selectedAdvances.contains(advance));
        chartCheckbox.addEventListener(Events.ON_CHECK, new EventListener() {
            @Override
            public void onEvent(Event event) {
                if (chartCheckbox.isChecked()) {
                    selectedAdvances.add(advance);
                } else {
                    selectedAdvances.remove(advance);
                }
                reloadAdvances();
            }
        });

        Listcell listCell = new Listcell();
        listCell.appendChild(chartCheckbox);
        listItem.appendChild(listCell);
    }

    private void appendOperations(final Listitem listItem) {
        Hbox hbox = new Hbox();
        appendAddMeasurement(hbox, listItem);
        appendRemoveButton(hbox, listItem);

        Listcell listCell = new Listcell();
        listCell.appendChild(hbox);
        listItem.appendChild(listCell);
    }

    private void appendAddMeasurement(final Hbox hbox, final Listitem listItem) {
        final AdvanceAssignment advance = (AdvanceAssignment) listItem
                .getValue();
        final Button addMeasurementButton = createAddMeasurementButton();

        addMeasurementButton.addEventListener(Events.ON_CLICK,
                new EventListener() {
                    @Override
                    public void onEvent(Event event) {
                        if (!listItem.equals(editAdvances.getSelectedItem())) {
                            selectAdvanceLine(listItem);
                        }
                        goToCreateLineAdvanceMeasurement();
                    }
                });

        if ((advance.getAdvanceType() != null)
                && (advance.getAdvanceType().isQualityForm())) {
            addMeasurementButton.setDisabled(true);
            addMeasurementButton
                    .setTooltiptext(_("Progress that are reported by quality forms can not be modified"));
        } else if (advance instanceof IndirectAdvanceAssignment) {
            addMeasurementButton.setDisabled(true);
            addMeasurementButton
                    .setTooltiptext(_("Calculated progress can not be modified"));
        }

        hbox.appendChild(addMeasurementButton);

    }

    private void appendRemoveButton(final Hbox hbox, final Listitem listItem) {
        final AdvanceAssignment advance = (AdvanceAssignment) listItem
                .getValue();
        final Button removeButton = createRemoveButton();

        removeButton.addEventListener(Events.ON_CLICK, new EventListener() {
            @Override
            public void onEvent(Event event) {
                goToRemoveLineAdvanceAssignment(listItem);
            }
        });

        if ((advance.getAdvanceType() != null)
                && (advance.getAdvanceType().isQualityForm())) {
            removeButton.setDisabled(true);
            removeButton
                    .setTooltiptext(_("Progress that are reported by quality forms can not be modified"));
        } else if (advance instanceof IndirectAdvanceAssignment) {
            removeButton.setDisabled(true);
            removeButton
                    .setTooltiptext(_("Calculated progress can not be removed"));
        } else if (manageOrderElementAdvancesModel
                .hasConsolidatedAdvances(advance)) {
            removeButton.setDisabled(true);
            removeButton
                    .setTooltiptext(_("Consolidated progress can not be removed"));
        }

        hbox.appendChild(removeButton);
    }

    private void setMaxValue(final Listitem item,Combobox comboAdvanceTypes) {
        Listcell listCell = (Listcell)item.getChildren().get(1);
        Decimalbox miBox = ((Decimalbox) listCell.getFirstChild());
        Comboitem selectedItem = comboAdvanceTypes.getSelectedItem();
        if(selectedItem != null){
            AdvanceType advanceType = ((AdvanceType) selectedItem.getValue());
            if(advanceType != null){
                DirectAdvanceAssignment advance = (DirectAdvanceAssignment) item
                        .getValue();
                advance.setMaxValue(manageOrderElementAdvancesModel
                        .getMaxValue(advanceType));
                miBox.setValue(manageOrderElementAdvancesModel
                        .getMaxValue(advanceType));
                miBox.invalidate();
            }
        }
    }

    private Constraint checkMaxValue() {
        return new Constraint() {
            @Override
            public void validate(Component comp, Object value)
                    throws WrongValueException {
                Listitem item = (Listitem) comp.getParent().getParent();
                DirectAdvanceAssignment advance = (DirectAdvanceAssignment) item
                        .getValue();
                if (!manageOrderElementAdvancesModel
                        .hasConsolidatedAdvances(advance)) {
                    if (value == null
                            || (BigDecimal.ZERO.compareTo((BigDecimal) value) >= 0)) {
                        ((Decimalbox) comp).setValue(advance.getAdvanceType()
                                .getDefaultMaxValue());
                        ((Decimalbox) comp).invalidate();
                        throw new WrongValueException(comp,
                                _("The max value must be greater than 0"));
                    }
                }
            }
        };
    }

    private void setPercentage(){
        if ((this.indexSelectedItem < editAdvances.getItemCount())
                && (this.indexSelectedItem >= 0)) {
            Listitem selectedItem = editAdvances.getItemAtIndex(indexSelectedItem);
            AdvanceAssignment advanceAssignment = (AdvanceAssignment) selectedItem
                    .getValue();

            DirectAdvanceAssignment directAdvanceAssignment;
            if (advanceAssignment instanceof IndirectAdvanceAssignment) {
                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
            } else {
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
            }
            final AdvanceMeasurement greatAdvanceMeasurement = this.manageOrderElementAdvancesModel
                    .getLastAdvanceMeasurement(directAdvanceAssignment);
            if (greatAdvanceMeasurement != null) {
                Listcell percentage = (Listcell) selectedItem.getChildren()
                        .get(3);
                ((Label) percentage.getFirstChild())
                        .setValue(this.manageOrderElementAdvancesModel
                                .getPercentageAdvanceMeasurement(
                                        greatAdvanceMeasurement).toString()
                                + " %");
                ((Label) percentage.getFirstChild()).invalidate();
            }
        }
    }

    private void setCurrentValue(){
      if(this.indexSelectedItem >= 0){
            Listitem selectedItem = editAdvances.getItemAtIndex(indexSelectedItem);
            AdvanceAssignment advanceAssignment = (AdvanceAssignment) selectedItem
                    .getValue();

            DirectAdvanceAssignment directAdvanceAssignment;
            if (advanceAssignment instanceof IndirectAdvanceAssignment) {
                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
            } else {
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
            }
            final AdvanceMeasurement greatAdvanceMeasurement = this.manageOrderElementAdvancesModel
                    .getLastAdvanceMeasurement(directAdvanceAssignment);
            if (greatAdvanceMeasurement != null) {
                Listcell value = (Listcell)selectedItem.getChildren().get(2);
                ((Decimalbox) value.getFirstChild())
                        .setValue(greatAdvanceMeasurement.getValue());
                ((Decimalbox) value.getFirstChild()).invalidate();
            }
        }
    }

    private Chart chart;

    public void setCurrentDate(Listitem item){
        this.manageOrderElementAdvancesModel.sortListAdvanceMeasurement();
        Util.reloadBindings(editAdvancesMeasurement);

        this.setCurrentDate();
        this.setPercentage();
        this.setCurrentValue();
        Util.reloadBindings(chart);
    }

    private void setCurrentDate(){
         if(this.indexSelectedItem >= 0){
            Listitem selectedItem = editAdvances.getItemAtIndex(indexSelectedItem);
            AdvanceAssignment advanceAssignment = (AdvanceAssignment) selectedItem
                    .getValue();

            DirectAdvanceAssignment directAdvanceAssignment;
            if (advanceAssignment instanceof IndirectAdvanceAssignment) {
                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
            } else {
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
            }
             final AdvanceMeasurement greatAdvanceMeasurement =
                 this.manageOrderElementAdvancesModel
                    .getLastAdvanceMeasurement(directAdvanceAssignment);
             if(greatAdvanceMeasurement != null){
                 Listcell date = (Listcell) selectedItem.getChildren().get(4);
                 LocalDate newDate = greatAdvanceMeasurement.getDate();
                 if (newDate != null) {
                     ((Datebox) date.getFirstChild()).setValue(newDate
                            .toDateTimeAtStartOfDay().toDate());
                 } else {
                    ((Datebox) date.getFirstChild()).setValue(null);
                }
             }
        }
    }

    private void cleanFields(DirectAdvanceAssignment advance) {
            this.manageOrderElementAdvancesModel
                    .cleanAdvance((DirectAdvanceAssignment) advance);
    }

    private void setReportGlobalAdvance(final Listitem item) {
        boolean spread = true;

        if (manageOrderElementAdvancesModel
                .hasAnyConsolidatedAdvanceCurrentOrderElement()) {
            showErrorMessage(_("Spread progress cannot be changed if there is a consolidation in any progress assignment from root task"));
            spread = false;
        } else {
            if (!radioSpreadIsConsolidated()) {
                for (AdvanceAssignment advance : this.getAdvanceAssignments()) {
                    advance.setReportGlobalAdvance(false);
                }
            } else {
                spread = false;
            }
        }

        ((AdvanceAssignment) item.getValue()).setReportGlobalAdvance(spread);
        Util.reloadBindings(editAdvances);
        setSelectedAdvanceLine();
    }

    private boolean radioSpreadIsConsolidated() {
        for (AdvanceAssignment advance : getAdvanceAssignments()) {
            if ((advance.getReportGlobalAdvance())
                    && (manageOrderElementAdvancesModel
                            .hasConsolidatedAdvances(advance))) {
                showErrorMessage(_("Spread progress cannot be changed if there is a consolidation in any progress assignment"));
                return true;
            }
        }
        return false;
    }

    private boolean validateDataForm(){
        return ((validateListAdvanceAssignment())
                &&(validateListAdvanceMeasurement()));
    }

    private boolean validateListAdvanceAssignment(){
        for(int i=0; i< editAdvances.getChildren().size(); i++){
            if(editAdvances.getChildren().get(i) instanceof Listitem){
                Listitem listItem = (Listitem) editAdvances.getChildren().get(i);
                AdvanceAssignment advance = (AdvanceAssignment) listItem
                        .getValue();
                if (advance != null) {
                    if (advance.getAdvanceType() == null) {
                        throw new WrongValueException(
                                getComboboxTypeBy(listItem),
                            _("Value is not valid, the type must be not empty"));
                    }

                    DirectAdvanceAssignment directAdvanceAssignment;
                    if (advance instanceof IndirectAdvanceAssignment) {
                        directAdvanceAssignment = manageOrderElementAdvancesModel
                            .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advance);
                    } else {
                        directAdvanceAssignment = (DirectAdvanceAssignment) advance;
                    }
                    if (directAdvanceAssignment != null
                        && directAdvanceAssignment.getMaxValue() == null) {
                        throw new WrongValueException(
                                getDecimalboxMaxValueBy(listItem),
                                _("Value is not valid, the current value must be not empty"));
                    }
                }
            }
        }
        return true;
    }

    private boolean validateListAdvanceMeasurement(){
        for(int i=0; i< editAdvancesMeasurement.getChildren().size(); i++){
            if(editAdvancesMeasurement.getChildren().get(i) instanceof Listitem){
                Listitem listItem = (Listitem) editAdvancesMeasurement.getChildren().get(i);
                AdvanceMeasurement advance = (AdvanceMeasurement) listItem
                        .getValue();
                if (advance != null) {
                    // Validate the value of the advance measurement
                    Decimalbox valueBox = getDecimalboxBy(listItem);
                    validateMeasurementValue(valueBox, advance.getValue());

                    // Validate the date of the advance measurement
                    Datebox dateBox = getDateboxBy(listItem);
                    if (advance.getDate() == null) {
                        validateMeasurementDate(dateBox, null);
                    } else {
                        validateMeasurementDate(dateBox, advance.getDate()
                            .toDateTimeAtStartOfDay().toDate());
                    }
                }
            }
        }
        return true;
    }

    private boolean validateAdvanceMeasurement(AdvanceMeasurement advance) {
        boolean result = true;
        // Validate the value of advance measurement
        if (advance.getValue() == null) {
            result = false;
        } else {
            String errorMessage = validateValueAdvanceMeasurement(advance);
            if (errorMessage != null) {
                result = false;
            }
        }

        // Validate the date of advance measurement
        if (advance.getDate() == null) {
            result = false;
        } else {
            String errorMessage = validateDateAdvanceMeasurement(advance
                    .getDate(), advance);
            if (errorMessage != null) {
                result = false;
            }
        }
        return result;
    }

    private Combobox getComboboxTypeBy(Listitem item) {
        return (Combobox) ((Listcell) item.getChildren().get(0))
                .getFirstChild();
    }

    private Combobox getDecimalboxMaxValueBy(Listitem item) {
        return (Combobox) ((Listcell) item.getChildren().get(1))
                .getFirstChild();
    }

    private Decimalbox getDecimalboxBy(Listitem item) {
        return (Decimalbox) ((Listcell) item.getChildren().get(0))
                .getFirstChild();
    }

    private Datebox getDateboxBy(Listitem item) {
        return (Datebox) ((Listcell) item.getChildren().get(2)).getFirstChild();
    }

    private boolean validateReportGlobalAdvance(){
        boolean existItems = false;
        for (AdvanceAssignment advance : this.getAdvanceAssignments()) {
            existItems = true;
            if (advance.getReportGlobalAdvance()) {
                    return true;
            }
        }
        return (!existItems);
    }

    public AdvanceMeasurementRenderer getAdvanceMeasurementRenderer() {
        return advanceMeasurementRenderer;
    }

    private class AdvanceMeasurementRenderer implements ListitemRenderer {

        @Override
        public void render(Listitem item, Object data) {
            AdvanceMeasurement advanceMeasurement = (AdvanceMeasurement) data;

            item.setValue(advanceMeasurement);

            appendDecimalBoxValue(item);
            appendLabelPercentage(item);
            appendDateboxDate(item);
            appendRemoveButton(item);
        }

        private void appendDecimalBoxValue(final Listitem listitem) {
            final AdvanceMeasurement advanceMeasurement = (AdvanceMeasurement) listitem
                    .getValue();
            final Decimalbox value = new Decimalbox();
            Listcell listcell = new Listcell();
            listcell.appendChild(value);
            listitem.appendChild(listcell);

            value.setScale(calculateScale(advanceMeasurement));
            value.setDisabled(isReadOnlyAdvanceMeasurements()
                    || manageOrderElementAdvancesModel
                            .hasConsolidatedAdvances(advanceMeasurement));
            value.addEventListener(Events.ON_CHANGE, new EventListener() {

                @Override
                public void onEvent(Event event) {
                    if (manageOrderElementAdvancesModel
                            .canRemoveOrChange(advanceMeasurement)) {
                        updatesValue(value);
                        validateMeasurementValue(value, value.getValue());
                    } else {
                        throw new WrongValueException(
                                value,
                                _("This progress measurement cannot be deleted or changed because it is consolidated"));
                    }
                }
            });

            Util.bind(value, new Util.Getter<BigDecimal>() {

                @Override
                public BigDecimal get() {
                    return advanceMeasurement.getValue();
                }
            }, new Util.Setter<BigDecimal>() {

                @Override
                public void set(BigDecimal value) {
                    if (manageOrderElementAdvancesModel
                            .canRemoveOrChange(advanceMeasurement)) {
                        advanceMeasurement.setValue(value);
                        reloadAdvances();
                    }
                }
            });
        }

        private void appendLabelPercentage(final Listitem listitem) {
            final AdvanceMeasurement advanceMeasurement = (AdvanceMeasurement) listitem
                    .getValue();

            BigDecimal percentage = manageOrderElementAdvancesModel
                    .getPercentageAdvanceMeasurement(advanceMeasurement);
            Label percentageLabel = new Label(percentage.toString() + " %");

            Listcell listcell = new Listcell();
            listcell.appendChild(percentageLabel);
            listitem.appendChild(listcell);
        }

        private void appendDateboxDate(final Listitem listitem) {
            final AdvanceMeasurement advanceMeasurement = (AdvanceMeasurement) listitem
                    .getValue();
            final Datebox date = new Datebox();

            Listcell listcell = new Listcell();
            listcell.appendChild(date);
            listitem.appendChild(listcell);

            date.setDisabled(isReadOnlyAdvanceMeasurements()
                    || manageOrderElementAdvancesModel
                            .hasConsolidatedAdvances(advanceMeasurement));
            date.addEventListener(Events.ON_CHANGE, new EventListener() {

                @Override
                public void onEvent(Event event) {
                    if (manageOrderElementAdvancesModel
                            .canRemoveOrChange(advanceMeasurement)) {
                        validateMeasurementDate(date, date.getValue());
                        setCurrentDate(listitem);
                    } else {
                        throw new WrongValueException(
                                date,
                                _("This progress measurement cannot be deleted or changed because it is consolidated"));
                    }
                }
            });

            Util.bind(date, new Util.Getter<Date>() {

                @Override
                public Date get() {
                    if (advanceMeasurement.getDate() == null) {
                        return null;
                    }
                    return advanceMeasurement.getDate()
                            .toDateTimeAtStartOfDay().toDate();
                }
            }, new Util.Setter<Date>() {

                @Override
                public void set(Date value) {
                    if (manageOrderElementAdvancesModel
                            .canRemoveOrChange(advanceMeasurement)) {

                        LocalDate oldDate = advanceMeasurement.getDate();
                        advanceMeasurement.setDate(new LocalDate(value));

                        if (manageOrderElementAdvancesModel
                                .hasConsolidatedAdvances(advanceMeasurement)) {
                            showMessagesConsolidation(new LocalDate(value));
                            advanceMeasurement.setDate(oldDate);
                        }

                        manageOrderElementAdvancesModel
                                    .sortListAdvanceMeasurement();
                        reloadAdvances();
                    }
                }
            });
        }

        private AdvanceMeasurement getAdvanceMeasurementByComponent(
                Component comp) {
            try {
                Listitem item = (Listitem) comp.getParent().getParent();
                return (AdvanceMeasurement) item.getValue();
            } catch (Exception e) {
                return null;
            }
        }

        private void appendRemoveButton(final Listitem listItem) {

            final AdvanceMeasurement measure = (AdvanceMeasurement) listItem
                    .getValue();
            final Button removeButton = createRemoveButton();

            DirectAdvanceAssignment advance = (DirectAdvanceAssignment) measure
                    .getAdvanceAssignment();
            if ((advance.getAdvanceType() != null)
                    && (advance.getAdvanceType().isQualityForm())) {
                removeButton.setDisabled(true);
                removeButton
                        .setTooltiptext(_("Progress measurements that are reported by quality forms can not be removed"));
            } else if (advance.isFake()) {
                removeButton.setDisabled(true);
                removeButton
                        .setTooltiptext(_("Calculated progress measurement can not be removed"));
            } else if (manageOrderElementAdvancesModel
                    .hasConsolidatedAdvances(measure)) {
                removeButton.setDisabled(true);
                removeButton
                        .setTooltiptext(_("Consolidated progress measurement can not be removed"));
            }

            removeButton.addEventListener(Events.ON_CLICK, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    goToRemoveLineAdvanceMeasurement(listItem);
                }
            });

            Listcell listCell = new Listcell();
            listCell.appendChild(removeButton);
            listItem.appendChild(listCell);
        }

    }

    public XYModel getChartData() {
        return this.manageOrderElementAdvancesModel.getChartData(selectedAdvances);
    }

    private int calculateScale(AdvanceMeasurement advanceMeasurement) {
        return advanceMeasurement.getAdvanceAssignment().getAdvanceType()
                .getUnitPrecision().stripTrailingZeros().scale();
    }

    private Button createRemoveButton() {
        Button removeButton = new Button();
        removeButton.setSclass("icono");
        removeButton.setImage("/common/img/ico_borrar1.png");
        removeButton.setHoverImage("/common/img/ico_borrar.png");
        removeButton.setTooltiptext(_("Delete"));

        return removeButton;
    }

    private Button createAddMeasurementButton() {
        Button addButton = new Button();
        addButton.setLabel(_("Add measure"));
        addButton.setTooltiptext(_("Add new progress measurement"));
        return addButton;
    }

    public void refreshChangesFromOrderElement() {
        manageOrderElementAdvancesModel.refreshChangesFromOrderElement();
    }

    private void showMessageNotAddMoreAdvances() {
        String message = _("All progress types have already been assigned.");
        increaseScreenHeight();
        messagesForUser.showMessage(Level.ERROR, message);
    }

    public void refreshSelectedAdvance() {
        if ((indexSelectedItem < 0)
                || (indexSelectedItem >= getAdvanceAssignments().size())) {
            selectSpreadAdvanceLine();
        }
        selectAdvanceLine(indexSelectedItem);
    }

    private void showMessageDeleteSpread() {
        String message = _("This progress can not be removed, because it is spread. It is necessary to select another progress as spread.");
        showErrorMessage(message);
    }

    private void showMessagesConsolidation(LocalDate date) {
        String message = _("This progress measurement can not be in "
                + date
                + ", because it is consolidated. it is necessary to select other date.");
        showErrorMessage(message);
    }

    private void showErrorMessage(String message) {
        increaseScreenHeight();
        messagesForUser.showMessage(Level.ERROR, message);
    }

    private String validateValueAdvanceMeasurement(
            AdvanceMeasurement measurement) {
        if (manageOrderElementAdvancesModel.greatThanMaxValue(measurement)) {
            return _("Value is not valid, the current value must be less than max value");
        }
        if (!manageOrderElementAdvancesModel.isPrecisionValid(measurement)) {
            return _(
                    "Value must be a multiple of the precission value of the progress type: {0}",
                    manageOrderElementAdvancesModel.getUnitPrecision()
                            .stripTrailingZeros().toPlainString());
        }
        if (manageOrderElementAdvancesModel.lessThanPreviousMeasurements()) {
            return _("Value is not valid, the value must be greater than the value of the previous progress.");
        }
        return null;
    }

    private String validateDateAdvanceMeasurement(LocalDate value,
            AdvanceMeasurement measurement) {
        LocalDate oldDate = measurement.getDate();
        measurement.setDate(value);

        if (!manageOrderElementAdvancesModel.isDistinctValidDate(value,
                measurement)) {
            return _("The date is not valid, the date must be unique for this progress assignment");
        }
        if (measurement != null) {
            if (manageOrderElementAdvancesModel
                    .hasConsolidatedAdvances(measurement)) {
                measurement.setDate(oldDate);
            } else {
                manageOrderElementAdvancesModel.sortListAdvanceMeasurement();
                if (manageOrderElementAdvancesModel
                        .lessThanPreviousMeasurements()) {
                    return _("Value is not valid, the value must be greater than the value of the previous progress.");
                }
            }

            if (!isReadOnlyAdvanceMeasurements()) {
                LocalDate consolidatedUntil = manageOrderElementAdvancesModel
                        .getLastConsolidatedMeasurementDate(measurement
                                .getAdvanceAssignment());
                if (consolidatedUntil != null) {
                    if (consolidatedUntil.compareTo(measurement.getDate()) >= 0) {
                        return _("Date is not valid, it must be greater than the last progress consolidation");
                    }
                }
            }

        }
        return null;
    }

    public boolean findErrorsInMeasurements() {
        boolean result = findPageWithError();
        validateListAdvanceMeasurement();
        return result;
    }

    private boolean findPageWithError() {
        int currentPage = editAdvancesMeasurement.getActivePage();
        int i = 0;
        int page = 0;
        changePage(page);
        for (Listitem item : (List<Listitem>) editAdvancesMeasurement
                .getItems()) {
            AdvanceMeasurement advance = (AdvanceMeasurement) item.getValue();
            if (advance != null) {
                if (!validateAdvanceMeasurement(advance)) {
                    return true;
                }
                i++;
                if (i == editAdvancesMeasurement.getPageSize()) {
                    i = 0;
                    changePage(++page);
                }
            }
        }
        changePage(currentPage);
        return false;
    }

    private void changePage(int page) {
        if ((page >= 0) && (page < editAdvancesMeasurement.getPageCount())) {
            editAdvancesMeasurement.setActivePage(page);
            editAdvancesMeasurement.invalidate();
        }
    }

    public void onPagingMeasurement() {
        validateListAdvanceMeasurement();
    }

    public void validateMeasurementDate(Component comp, Date value)
            throws WrongValueException {
        AdvanceMeasurement advanceMeasurement = getAdvanceMeasurementByComponent(comp);
        if ((manageOrderElementAdvancesModel
                .canRemoveOrChange(advanceMeasurement))) {
            if (value == null) {
                advanceMeasurement.setDate(null);
                ((Datebox) comp).setValue(null);
                throw new WrongValueException(comp,
                        _("The date is not valid, the date must be not empty"));
            } else {
                String errorMessage = validateDateAdvanceMeasurement(
                        new LocalDate(value), advanceMeasurement);
                LocalDate date = advanceMeasurement.getDate();
                if (date != null) {
                    ((Datebox) comp).setValue(date.toDateTimeAtStartOfDay()
                            .toDate());
                }
                if (errorMessage != null) {
                    throw new WrongValueException(comp, errorMessage);
                }
            }
        }
    }

    public void validateMeasurementValue(Component comp, Object value)
            throws WrongValueException {
        AdvanceMeasurement advanceMeasurement = getAdvanceMeasurementByComponent(comp);
        if ((advanceMeasurement != null)
                && (manageOrderElementAdvancesModel
                        .canRemoveOrChange(advanceMeasurement))) {
            advanceMeasurement.setValue((BigDecimal) value);
            ((Decimalbox) comp).setValue((BigDecimal) value);
            if (((BigDecimal) value) == null) {
                throw new WrongValueException(
                        comp,
                        _("Value is not valid, the current value must be not empty"));
            } else {
                String errorMessage = validateValueAdvanceMeasurement(advanceMeasurement);
                if (errorMessage != null) {
                    throw new WrongValueException(comp, errorMessage);
                }
            }
        }
    }

    private AdvanceMeasurement getAdvanceMeasurementByComponent(Component comp) {
        try {
            Listitem item = (Listitem) comp.getParent().getParent();
            return (AdvanceMeasurement) item.getValue();
        } catch (Exception e) {
            return null;
        }
    }
}
