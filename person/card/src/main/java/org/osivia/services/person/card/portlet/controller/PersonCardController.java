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
package org.osivia.services.person.card.portlet.controller;

import javax.annotation.PostConstruct;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osivia.portal.api.PortalException;
import org.osivia.portal.api.context.PortalControllerContext;
import org.osivia.portal.api.notifications.NotificationsType;
import org.osivia.portal.core.cms.CMSException;
import org.osivia.services.person.card.portlet.service.LevelEdition;
import org.osivia.services.person.card.portlet.service.PersonCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import org.springframework.web.portlet.context.PortletConfigAware;
import org.springframework.web.portlet.context.PortletContextAware;

import fr.toutatice.portail.cms.nuxeo.api.CMSPortlet;

/**
 * Controller of the view-card jsp
 * @author Loïc Billon
 *
 */
@Controller
@RequestMapping("VIEW")
@SessionAttributes({ "card" })
public class PersonCardController extends CMSPortlet implements PortletContextAware, PortletConfigAware {

	
	private PortletContext portletContext;
	private PortletConfig portletConfig;


	@Autowired
	private PersonCardService service;
	
	@Autowired
	private Validator chgPwdValidator;

	
	@PostConstruct
	public void initNuxeoService() throws Exception {
		super.init();
		if ((this.portletContext != null) && (this.portletContext.getAttribute("nuxeoService") == null)) {

			this.init(this.portletConfig);
		}

	}

	

	@ModelAttribute("card")
	public Card initCard(PortletRequest request, PortletResponse response) throws PortalException {

		return service.loadCard(new PortalControllerContext(portletContext, request, response));

	}
	
	@RenderMapping
	public String showCard(@ModelAttribute("card") Card card, RenderRequest request, RenderResponse response) {
		
		if(card.getUserConsulte() == null) {
			
			PortalControllerContext pcc = new PortalControllerContext(this.portletContext, request, response);
			this.addNotification(pcc, "label.modifNonAutorise", NotificationsType.WARNING);
			
			return "blank";
		}else {
		
			return "view-card";
		}
	}
	
	@RenderMapping(params = "action=chgPwd")
	public String showChgPwd(@ModelAttribute("card") Card card, RenderRequest request, RenderResponse response, PortletSession session)
			throws PortalException, CMSException {

		
		String retour = null;

		if (card.getLevelEdition() == LevelEdition.ALLOW) {
			retour = "chgPwd";
		} else {
			PortalControllerContext pcc = new PortalControllerContext(this.portletContext, request, response);
			this.addNotification(pcc, "label.modifNonAutorise", NotificationsType.WARNING);
			retour = this.showCard(card, request, response);
		}

		return retour;
	}

	@ActionMapping(value="modify")
	public void showModify(@ModelAttribute("card") Card card, ActionRequest request, ActionResponse response, PortletSession session)
			throws PortalException, CMSException {

        response.setRenderParameter("controller", "edit");

	}
	
	@ActionMapping(value="chgPwd")
	public void showChgPwd(@ModelAttribute("card") Card card, ActionRequest request, ActionResponse response, PortletSession session)
			throws PortalException, CMSException {

        response.setRenderParameter("controller", "chgPwd");
	}

	@RenderMapping("blank")
	public String getBlankPage() {
		return "blank";
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.web.portlet.context.PortletConfigAware#setPortletConfig(javax.portlet.PortletConfig)
	 */
	@Override
	public void setPortletConfig(PortletConfig portletConfig) {
		this.portletConfig = portletConfig;
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.portlet.context.PortletContextAware#setPortletContext(javax.portlet.PortletContext)
	 */
	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
		
	}

	
}