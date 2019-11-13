package org.osivia.directory.v2.service.preferences;

import fr.toutatice.portail.cms.nuxeo.api.INuxeoCommand;
import fr.toutatice.portail.cms.nuxeo.api.NuxeoController;
import fr.toutatice.portail.cms.nuxeo.api.services.NuxeoCommandContext;
import org.apache.commons.lang.math.NumberUtils;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.osivia.directory.v2.model.preferences.UserPreferences;
import org.osivia.directory.v2.model.preferences.UserPreferencesImpl;
import org.osivia.directory.v2.model.preferences.UserSavedSearch;
import org.osivia.directory.v2.model.preferences.UserSavedSearchImpl;
import org.osivia.directory.v2.repository.preferences.UpdateUserPreferencesCommand;
import org.osivia.directory.v2.service.DirServiceImpl;
import org.osivia.portal.api.PortalException;
import org.osivia.portal.api.cache.services.CacheInfo;
import org.osivia.portal.api.context.PortalControllerContext;
import org.osivia.portal.api.directory.v2.model.Person;
import org.osivia.portal.api.directory.v2.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User preferences service implementation.
 *
 * @author Cédric Krommenhoek
 * @see DirServiceImpl
 * @see UserPreferencesService
 */
@Service
public class UserPreferencesServiceImpl extends DirServiceImpl implements UserPreferencesService {

    /**
     * User preferences session attribute.
     */
    private static final String USER_PREFERENCES_ATTRIBUTE = "osivia.user.preferences";
    /**
     * Principal token session attribute.
     */
    private static final String PRINCIPAL_TOKEN_ATTRIBUTE = "PRINCIPAL_TOKEN";


    /**
     * Application context.
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Person service.
     */
    @Autowired
    private PersonService personService;


    /**
     * Constructor.
     */
    public UserPreferencesServiceImpl() {
        super();
    }


    @Override
    public UserPreferences getUserPreferences(PortalControllerContext portalControllerContext) throws PortalException {
        // HTTP session
        HttpSession httpSession = portalControllerContext.getHttpServletRequest().getSession();

        // User preferences session attribute
        Object userPreferencesAttribute = httpSession.getAttribute(USER_PREFERENCES_ATTRIBUTE);
        // Principal token session attribute
        Object principalTokenAttribute = httpSession.getAttribute(PRINCIPAL_TOKEN_ATTRIBUTE);

        // User preferences
        UserPreferences userPreferences;

        if (userPreferencesAttribute instanceof UserPreferences) {
            userPreferences = (UserPreferences) userPreferencesAttribute;
        } else if (principalTokenAttribute instanceof String) {
            // User identifier
            String uid = (String) principalTokenAttribute;

            userPreferences = this.getUserPreferences(portalControllerContext, uid);
        } else {
            userPreferences = null;
        }

        // Save user preferences in session
        if (userPreferences != null) {
            httpSession.setAttribute(USER_PREFERENCES_ATTRIBUTE, userPreferences);
        }

        return userPreferences;
    }


    @Override
    public UserPreferences getUserPreferences(PortalControllerContext portalControllerContext, String uid) throws PortalException {
        // User person object
        Person person = this.personService.getPerson(uid);

        // User profile Nuxeo document
        Document profile;
        if (person == null) {
            profile = null;
        } else {
            Object ecmProfile = this.personService.getEcmProfile(portalControllerContext, person);
            if (ecmProfile instanceof Document) {
                profile = (Document) ecmProfile;
            } else {
                profile = null;
            }
        }

        // User preferences
        UserPreferences userPreferences;

        if (profile == null) {
            userPreferences = null;
        } else {
            userPreferences = this.createUserPreferences(profile);
        }

        return userPreferences;
    }


    /**
     * Create user preferences.
     *
     * @param profile user profile Nuxeo document
     * @return
     */
    protected UserPreferences createUserPreferences(Document profile) {
        // User preferences implementation
        UserPreferencesImpl userPreferences = this.applicationContext.getBean(UserPreferencesImpl.class, profile.getId());

        // Terms of service
        String termsOfService = profile.getString(UpdateUserPreferencesCommand.TERMS_OF_SERVICE);
        userPreferences.setTermsOfService(termsOfService);

        // Folder displays
        Map<String, String> folderDisplays = new HashMap<>();
        PropertyList foldersPropertyList = profile.getProperties().getList(UpdateUserPreferencesCommand.METADATA_FOLDERS_PREFS);
        if ((foldersPropertyList != null) && !foldersPropertyList.isEmpty()) {
            for (int i = 0; i < foldersPropertyList.size(); i++) {
                PropertyMap folderPropertyMap = foldersPropertyList.getMap(i);

                Object objectValue = folderPropertyMap.get(UpdateUserPreferencesCommand.METADATA_FOLDERS_PREFS_VALUE);
                Object objectId = folderPropertyMap.get(UpdateUserPreferencesCommand.METADATA_FOLDERS_PREFS_ID);

                if (objectId != null && objectValue != null) {
                    folderDisplays.put(objectId.toString(), objectValue.toString());
                }
            }
        }
        userPreferences.setFolderDisplays(folderDisplays);

        // Saved searches
        List<UserSavedSearch> savedSearches;
        PropertyList savedSearchesPropertyList = profile.getProperties().getList(UpdateUserPreferencesCommand.SAVED_SEARCHES_XPATH);
        if ((savedSearchesPropertyList == null) || savedSearchesPropertyList.isEmpty()) {
            savedSearches = null;
        } else {
            savedSearches = new ArrayList<>(savedSearchesPropertyList.size());

            for (int i = 0; i < savedSearchesPropertyList.size(); i++) {
                PropertyMap savedSearchPropertyMap = savedSearchesPropertyList.getMap(i);

                // Identifier
                int id = NumberUtils.toInt(savedSearchPropertyMap.getString(UpdateUserPreferencesCommand.SAVED_SEARCH_ID));
                // Saved search
                UserSavedSearchImpl savedSearch = this.applicationContext.getBean(UserSavedSearchImpl.class, id);
                // Display name
                String displayName = savedSearchPropertyMap.getString(UpdateUserPreferencesCommand.SAVED_SEARCH_DISPLAY_NAME);
                savedSearch.setDisplayName(displayName);
                // Order
                Integer order = NumberUtils.createInteger(savedSearchPropertyMap.getString(UpdateUserPreferencesCommand.SAVED_SEARCH_ORDER));
                savedSearch.setOrder(order);
                // Data
                String data = savedSearchPropertyMap.getString(UpdateUserPreferencesCommand.SAVED_SEARCH_DATA);
                savedSearch.setData(data);

                savedSearches.add(savedSearch);
            }
        }
        userPreferences.setSavedSearches(savedSearches);

        return userPreferences;
    }


    /**
     * Get user preferences from HTTP session.
     *
     * @param httpSession HTTP session
     * @return user preferences, may be null if not found in session
     */
    public UserPreferences getUserPreferences(HttpSession httpSession) {
        // User preferences session attribute
        Object userPreferencesAttribute = httpSession.getAttribute(USER_PREFERENCES_ATTRIBUTE);

        // User preferences
        UserPreferences userPreferences;

        if (userPreferencesAttribute instanceof UserPreferences) {
            userPreferences = (UserPreferences) userPreferencesAttribute;
        } else {
            userPreferences = null;
        }

        return userPreferences;
    }


    @Override
    public UserSavedSearch createUserSavedSearch(PortalControllerContext portalControllerContext, int id) throws PortalException {
        return this.applicationContext.getBean(UserSavedSearch.class, id);
    }


    @Override
    public void saveUserPreferences(PortalControllerContext portalControllerContext, UserPreferences userPreferences) throws PortalException {
        // Nuxeo controller
        NuxeoController nuxeoController = new NuxeoController(portalControllerContext);
        nuxeoController.setAuthType(NuxeoCommandContext.AUTH_TYPE_SUPERUSER);
        nuxeoController.setCacheType(CacheInfo.CACHE_SCOPE_NONE);

        // Nuxeo command
        INuxeoCommand command = this.applicationContext.getBean(UpdateUserPreferencesCommand.class, userPreferences);
        nuxeoController.executeNuxeoCommand(command);
    }

}