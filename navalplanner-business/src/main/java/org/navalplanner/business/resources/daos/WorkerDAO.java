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

package org.navalplanner.business.resources.daos;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.navalplanner.business.common.daos.GenericDAOHibernate;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.Worker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * Hibernate DAO for the <code>Worker</code> entity.
 *
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class WorkerDAO extends GenericDAOHibernate<Worker, Long>
    implements IWorkerDAO {

    @Override
    public Worker findUniqueByNif(String nif) throws InstanceNotFoundException {
        Criteria criteria = getSession().createCriteria(Worker.class);
        criteria.add(Restrictions.eq("nif", nif).ignoreCase());

        List<Worker> list = criteria.list();

        if (list.size() != 1) {
            throw new InstanceNotFoundException(nif, Worker.class.getName());
        }

        return list.get(0);
    }

    @Override
    public List<Worker> getWorkers() {
        return list(Worker.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Worker> findByNameAndCriterions(String name,
            List<Criterion> criterionList) {

        // Prepare query
        String strQuery = "SELECT worker FROM Worker worker ";

        if (criterionList != null && criterionList.size() > 0) {
            strQuery += "JOIN worker.criterionSatisfactions AS satisfaction "
                    + "JOIN satisfaction.criterion AS criterion ";
        }

        strQuery += "WHERE (UPPER(worker.firstName) LIKE :name OR worker.nif LIKE :name) ";
        if (criterionList != null && criterionList.size() > 0) {
            strQuery += " AND criterion IN (:criterionList)";
        }

        // Execute query
        Query query = getSession().createQuery(strQuery);
        query.setParameter("name", "%" + name.toUpperCase() + "%");
        if (criterionList != null && criterionList.size() > 0) {
            query.setParameterList("criterionList", criterionList);
        }

        // Get result
        return query.list();
    }
}
