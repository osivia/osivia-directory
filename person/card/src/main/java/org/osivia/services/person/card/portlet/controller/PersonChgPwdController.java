/**
 *
 */
package org.osivia.services.person.card.portlet.controller;

import fr.toutatice.portail.cms.nuxeo.api.CMSPortlet;
import org.dom4j.Element;
import org.dom4j.io.HTMLWriter;
import org.osivia.portal.api.context.PortalControllerContext;
import org.osivia.portal.api.internationalization.Bundle;
import org.osivia.portal.api.internationalization.IBundleFactory;
import org.osivia.portal.api.notifications.INotificationsService;
import org.osivia.portal.api.notifications.NotificationsType;
import org.osivia.services.person.card.portlet.service.LevelChgPwd;
import org.osivia.services.person.card.portlet.service.PersonCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;
import org.springframework.web.portlet.context.PortletConfigAware;
import org.springframework.web.portlet.context.PortletContextAware;

import javax.portlet.*;
import java.io.IOException;

/**
 * @author Loïc Billon
 *
 */
@Controller
@RequestMapping(value = "VIEW", params = "controller=chgPwd")
@SessionAttributes({"card", "formChgPwd"})
public class PersonChgPwdController extends CMSPortlet implements PortletContextAware, PortletConfigAware {

    private PortletContext portletContext;
    private PortletConfig portletConfig;

    @Autowired
    private PersonCardService service;


    /** Bundle factory. */
    @Autowired
    private IBundleFactory bundleFactory;

    /** Notifications service. */
    @Autowired
    private INotificationsService notificationsService;


    @Autowired
    private Validator chgPwdValidator;

    @Autowired
    private Validator overwritePwdValidator;


    @RenderMapping
    public String changePassword() {

        return "modify-password";
    }


    @ModelAttribute("formChgPwd")
    public FormChgPwd getForm() {
        FormChgPwd form = new FormChgPwd();
        return form;
    }

    @ActionMapping(value = "edit", params = "save")
    public void updateChgtPwd(@ModelAttribute("card") Card card, @ModelAttribute FormChgPwd formChgPwd, BindingResult result, ActionRequest request,
                              ActionResponse response, SessionStatus status) {

        PortalControllerContext pcc = new PortalControllerContext(this.portletContext, request, response);

        if (card.getLevelChgPwd() == LevelChgPwd.ALLOW) {

            chgPwdValidator.validate(formChgPwd, result);

            if (!result.hasErrors()) {


                boolean changeOk = service.changePassword(pcc, card, formChgPwd);

                if (changeOk) {

                    // Notification
                    Bundle bundle = this.bundleFactory.getBundle(request.getLocale());
                    String message = bundle.getString("PASSWORD_UPDATE_OK");
                    this.notificationsService.addSimpleNotification(pcc, message, NotificationsType.SUCCESS);

                    status.setComplete();
                } else {
                    response.setRenderParameter("controller", "chgPwd");

                    // Notification
                    Bundle bundle = this.bundleFactory.getBundle(request.getLocale());
                    String message = bundle.getString("WRONG_PASSWORD");
                    this.notificationsService.addSimpleNotification(pcc, message, NotificationsType.ERROR);
                }

            } else {
                response.setRenderParameter("controller", "chgPwd");
            }
        } else if (card.getLevelChgPwd() == LevelChgPwd.OVERWRITE) {

            overwritePwdValidator.validate(formChgPwd, result);

            if (!result.hasErrors()) {
                service.overwritePassword(card, formChgPwd);

                // Notification
                Bundle bundle = this.bundleFactory.getBundle(request.getLocale());
                String message = bundle.getString("PASSWORD_UPDATE_OK");
                this.notificationsService.addSimpleNotification(pcc, message, NotificationsType.SUCCESS);

                status.setComplete();

            } else {
                response.setRenderParameter("controller", "chgPwd");
            }
        }
    }

    @ActionMapping(value = "edit", params = "cancel")
    public void cancel(ActionResponse response, SessionStatus status, ModelMap model) throws Exception {

        status.setComplete();
    }


    /**
     * Password rules information resource mapping.
     *
     * @param request  resource request
     * @param response resource response
     * @param password password
     */
    @ResourceMapping("password-information")
    public void passwordInformation(ResourceRequest request, ResourceResponse response, @RequestParam(name = "password", required = false) String password) throws PortletException, IOException {
        // Portal controller context
        PortalControllerContext portalControllerContext = new PortalControllerContext(this.portletContext, request, response);

        // Password rules information
        Element information = this.service.getPasswordRulesInformation(portalControllerContext, password);

        // Content type
        response.setContentType("text/html");

        // Content
        HTMLWriter htmlWriter = new HTMLWriter(response.getPortletOutputStream());
        htmlWriter.write(information);
        htmlWriter.close();
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
