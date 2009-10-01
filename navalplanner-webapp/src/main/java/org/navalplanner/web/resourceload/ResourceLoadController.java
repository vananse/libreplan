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

package org.navalplanner.web.resourceload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zkoss.ganttz.resourceload.ResourcesLoadPanel;
import org.zkoss.ganttz.timetracker.TimeTracker;
import org.zkoss.zk.ui.util.GenericForwardComposer;

/**
 * Controller for global resourceload view
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResourceLoadController extends GenericForwardComposer {

    @Autowired
    private IResourceLoadModel resourceLoadModel;

    public ResourceLoadController() {
    }

    @Override
    public void doAfterCompose(org.zkoss.zk.ui.Component comp) throws Exception {
        resourceLoadModel.initGlobalView();
        ResourcesLoadPanel resourcesLoadPanel = buildResourcesLoadPanel();
        comp.appendChild(resourcesLoadPanel);
        resourcesLoadPanel.afterCompose();
    }

    private ResourcesLoadPanel buildResourcesLoadPanel() {
        return new ResourcesLoadPanel(resourceLoadModel.getLoadTimeLines(),
                new TimeTracker(resourceLoadModel.getViewInterval()));
    }
}
