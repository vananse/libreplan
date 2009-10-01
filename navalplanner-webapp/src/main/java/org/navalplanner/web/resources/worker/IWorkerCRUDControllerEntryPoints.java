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

package org.navalplanner.web.resources.worker;

import org.navalplanner.business.resources.entities.Worker;
import org.navalplanner.web.common.entrypoints.EntryPoint;
import org.navalplanner.web.common.entrypoints.EntryPoints;

/**
 * Contract for {@link WorkerCRUDController}. <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@EntryPoints(page = "/resources/worker/worker.zul", registerAs = "workerCRUD")
public interface IWorkerCRUDControllerEntryPoints {

    @EntryPoint("edit")
    public abstract void goToEditForm(Worker worker);

    @EntryPoint("workRelationships")
    public abstract void goToWorkRelationshipsForm(Worker worker);

    @EntryPoint("create")
    public abstract void goToCreateForm();

    @EntryPoint("list")
    void goToList();

}