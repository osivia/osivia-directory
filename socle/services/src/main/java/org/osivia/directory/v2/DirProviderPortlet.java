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
package org.osivia.directory.v2;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;

import org.osivia.directory.v2.service.LdapServiceImpl;
import org.osivia.portal.api.directory.v2.IDirDelegate;
import org.osivia.portal.api.directory.v2.IDirProvider;
import org.osivia.portal.api.locator.Locator;
import org.springframework.context.ApplicationContext;
import org.springframework.web.portlet.context.PortletApplicationContextUtils;

import fr.toutatice.portail.cms.nuxeo.api.CMSPortlet;

/**
 * Starter class used to provide the webapp portlet context through a ldap service Spring context
 * @author Loïc Billon
 * @since 4.4
 */
public class DirProviderPortlet extends CMSPortlet {

	/** The provider (socle) */
	private IDirProvider provider;

	/** The delegate (in this webapp). */
	private DirDelegate deletagate;

    @Override
    public void init(PortletConfig config) throws PortletException {
        super.init(config);

        // Portlet context
        PortletContext portletContext = this.getPortletContext();

        // export the directory service for other portlets
        ApplicationContext context = PortletApplicationContextUtils.getWebApplicationContext(portletContext);
        deletagate =  context.getBean(DirDelegate.class);

        deletagate.setPortletContext(portletContext);

        provider = Locator.findMBean(IDirProvider.class, IDirProvider.MBEAN_NAME);

        IDirDelegate iface = deletagate;
        provider.registerDelegate(iface);

        LdapServiceImpl.setPortletContext(portletContext);
    }


    // @Override
    public void destroy() {

        super.destroy();

        provider.unregisterDelegate(deletagate);
    }

}
