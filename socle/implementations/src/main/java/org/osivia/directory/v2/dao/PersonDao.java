/*
 * (C) Copyright 2016 OSIVIA (http://www.osivia.com)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.osivia.directory.v2.dao;

import java.util.List;

import javax.naming.Name;

import org.osivia.portal.api.directory.v2.model.Person;

/**
 * @author Loïc Billon
 *
 */
public interface PersonDao {

	/**
	 * @param dn
	 * @return
	 */
	Person getPerson(Name dn);

	/**
	 * @param ps
	 * @return
	 */
	List<Person> findByCriteria(Person p);

	/**
	 * @param p
	 */
	void create(Person p);

	/**
	 * @param p
	 */
	void update(Person p);

	/**
	 * @param uid
	 * @param currentPassword
	 * @return
	 */
	boolean verifyPassword(String uid, String currentPassword);

	/**
	 * @param p
	 * @param newPassword
	 */
	void updatePassword(Person p, String newPassword);

}