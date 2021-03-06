package org.osivia.services.user.savedsearches.administration.portlet.controller;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.osivia.portal.api.context.PortalControllerContext;
import org.osivia.services.user.savedsearches.administration.portlet.model.UserSavedSearchesAdministrationForm;
import org.osivia.services.user.savedsearches.administration.portlet.model.UserSavedSearchesAdministrationWindowSettings;
import org.osivia.services.user.savedsearches.administration.portlet.service.UserSavedSearchesAdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;

import javax.portlet.*;

/**
 * User saved searches administration portlet controller.
 *
 * @author Cédric Krommenhoek
 */
@Controller
@RequestMapping("VIEW")
public class UserSavedSearchesAdministrationController {

    /**
     * Portlet context.
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private PortletContext portletContext;

    /**
     * Portlet service.
     */
    @Autowired
    private UserSavedSearchesAdministrationService service;


    /**
     * Constructor.
     */
    public UserSavedSearchesAdministrationController() {
        super();
    }


    /**
     * View render mapping.
     *
     * @param request  render request
     * @param response render response
     * @return view path
     */
    @RenderMapping
    public String view(RenderRequest request, RenderResponse response) throws PortletException {
        // Portal controller context
        PortalControllerContext portalControllerContext = new PortalControllerContext(this.portletContext, request, response);

        return this.service.renderView(portalControllerContext);
    }


    /**
     * Save user saved searches administration action mapping.
     *
     * @param request  action request
     * @param response action response
     * @param form     user saved searches administration form model attribute
     */
    @ActionMapping("save")
    public void save(ActionRequest request, ActionResponse response, @ModelAttribute("form") UserSavedSearchesAdministrationForm form) throws PortletException {
        // Portal controller context
        PortalControllerContext portalControllerContext = new PortalControllerContext(this.portletContext, request, response);

        this.service.save(portalControllerContext, form);
    }


    /**
     * Move saved search action mapping.
     *
     * @param request   action request
     * @param response  action response
     * @param id        saved search identifier request parameter
     * @param direction move direction request parameter
     */
    @ActionMapping("move")
    public void move(ActionRequest request, ActionResponse response, @RequestParam("id") String id, @RequestParam("direction") String direction) throws PortletException {
        // Portal controller context
        PortalControllerContext portalControllerContext = new PortalControllerContext(this.portletContext, request, response);

        this.service.move(portalControllerContext, NumberUtils.toInt(id), StringUtils.equals("up", direction));
    }


    /**
     * Rename saved search action mapping.
     *
     * @param request     action request
     * @param response    action response
     * @param id          saved search identifier request parameter
     * @param displayName saved search display name request parameter
     */
    @ActionMapping("rename")
    public void rename(ActionRequest request, ActionResponse response, @RequestParam("id") String id, @RequestParam("displayName") String displayName) throws PortletException {
        // Portal controller context
        PortalControllerContext portalControllerContext = new PortalControllerContext(this.portletContext, request, response);

        this.service.rename(portalControllerContext, NumberUtils.toInt(id), displayName);
    }


    /**
     * Delete saved search action mapping.
     *
     * @param request  action request
     * @param response action response
     * @param id       saved search identifier request parameter
     */
    @ActionMapping("delete")
    public void delete(ActionRequest request, ActionResponse response, @RequestParam("id") String id) throws PortletException {
        // Portal controller context
        PortalControllerContext portalControllerContext = new PortalControllerContext(this.portletContext, request, response);

        this.service.delete(portalControllerContext, NumberUtils.toInt(id));
    }


    /**
     * Get user saved searches administration form model attribute.
     *
     * @param request  portlet request
     * @param response portlet response
     * @return form
     */
    @ModelAttribute("form")
    public UserSavedSearchesAdministrationForm getForm(PortletRequest request, PortletResponse response) throws PortletException {
        // Portal controller context
        PortalControllerContext portalControllerContext = new PortalControllerContext(this.portletContext, request, response);

        return this.service.getForm(portalControllerContext);
    }


    /**
     * Get window settings model attribute.
     *
     * @param request  portlet request
     * @param response portlet response
     * @return window settings
     */
    @ModelAttribute("windowSettings")
    public UserSavedSearchesAdministrationWindowSettings getWindowSettings(PortletRequest request, PortletResponse response) throws PortletException {
        // Portal controller context
        PortalControllerContext portalControllerContext = new PortalControllerContext(this.portletContext, request, response);

        return this.service.getWindowSettings(portalControllerContext);
    }

}
