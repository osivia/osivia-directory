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
package org.osivia.directory.v2.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Name;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.osivia.directory.v2.dao.CollabProfileDao;
import org.osivia.directory.v2.model.CollabProfile;
import org.osivia.directory.v2.model.ext.WorkspaceGroupType;
import org.osivia.directory.v2.model.ext.WorkspaceMember;
import org.osivia.directory.v2.model.ext.WorkspaceMemberImpl;
import org.osivia.directory.v2.model.ext.WorkspaceRole;
import org.osivia.directory.v2.repository.GetWorkspaceCommand;
import org.osivia.directory.v2.repository.PurgeWorkspaceCommand;
import org.osivia.directory.v2.repository.GetUserProfileCommand;
import org.osivia.directory.v2.repository.ReIndexUserCommand;
import org.osivia.directory.v2.repository.UpdateWorkspaceCommand;
import org.osivia.portal.api.PortalException;
import org.osivia.portal.api.context.PortalControllerContext;
import org.osivia.portal.api.directory.v2.model.Person;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.toutatice.portail.cms.nuxeo.api.INuxeoCommand;
import fr.toutatice.portail.cms.nuxeo.api.NuxeoController;
import fr.toutatice.portail.cms.nuxeo.api.NuxeoException;
import fr.toutatice.portail.cms.nuxeo.api.forms.FormFilterException;
import fr.toutatice.portail.cms.nuxeo.api.forms.IFormsService;
import fr.toutatice.portail.cms.nuxeo.api.services.NuxeoCommandContext;

/**
 * Implementation of the workspace service.
 *
 * @author Loïc Billon
 * @since 4.4
 * @see DirServiceImpl
 * @see WorkspaceService
 */
@Service
public class WorkspaceServiceImpl extends DirServiceImpl implements WorkspaceService, ApplicationContextAware {


	private final static Log ldapLogger = LogFactory.getLog("org.osivia.directory.v2");

	private final static Log logger_integrity = LogFactory.getLog("ldap.integrity");

    private static final String LDAP_INTEGRITY_MODEL_WEBID = IFormsService.FORMS_WEB_ID_PREFIX + "ldap_integrity";

    /** Person service. */
    @Autowired
    private PersonUpdateService personService;
    
    /** Person sample. */
    @Autowired
    private Person personSample;

    /** Collab profile sample. */
    @Autowired
    private CollabProfile sample;

    /** Collab profile DAO. */
    @Autowired
    private CollabProfileDao dao;


    /** Application context. */
    protected ApplicationContext applicationContext;


    /**
     * Constructor.
     */
    public WorkspaceServiceImpl() {
        super();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CollabProfile getEmptyProfile() {
        return this.applicationContext.getBean(sample.getClass());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CollabProfile getProfile(String cn) {
        Name dn = this.sample.buildDn(cn);

        return this.getProfile(dn);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CollabProfile getProfile(Name dn) {
        return this.dao.findByDn(dn);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<CollabProfile> findByWorkspaceId(String workspaceId) {
        CollabProfile searchProfile = this.applicationContext.getBean(sample.getClass());
        searchProfile.setWorkspaceId(workspaceId);

        return this.findByCriteria(searchProfile);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<CollabProfile> findByCriteria(CollabProfile profile) {
        return this.dao.findByCriteria(profile);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    // @Cacheable(key = "#workspaceId", value = { "membersByWksCache" })
    public List<WorkspaceMember> getAllMembers(String workspaceId) {
        List<CollabProfile> list = this.findByWorkspaceId(workspaceId);

        // Get the members
        List<Person> allPers = new ArrayList<Person>();
        for (CollabProfile cp : list) {
            if (cp.getType() == WorkspaceGroupType.space_group) {
                Person searchPers = this.applicationContext.getBean(personSample.getClass());

                List<Name> profiles = new ArrayList<Name>();
                profiles.add(cp.getDn());
                searchPers.setProfiles(profiles);

                // find all the members
                allPers = this.personService.findByCriteria(searchPers);
            }
        }

        // For each member, get his security group and local groups
        List<WorkspaceMember> members = new ArrayList<WorkspaceMember>();
        for (Person p : allPers) {
            WorkspaceMemberImpl member = new WorkspaceMemberImpl(p);

            for (CollabProfile cp : list) {
                if (cp.getType() == WorkspaceGroupType.security_group) {
                    if (cp.getUniqueMember().contains(p.getDn())) {
                        member.setRole(cp.getRole());
                    }
                } else if (cp.getType() == WorkspaceGroupType.local_group) {
                    if (cp.getUniqueMember().contains(p.getDn())) {
                        member.getLocalGroups().add(cp);
                    }
                }
            }

            members.add(member);
        }

        return members;
    }

    /**
     * {@inheritDoc}
     * @throws PortalException 
     */
    @Override
    // @Cacheable(key = "#workspaceId", value = { "membersByWksCache" })
    public List<WorkspaceMember> getAllMembers(PortalControllerContext pcc, String workspaceId) throws PortalException {
    	List<WorkspaceMember> allMembers = getAllMembers(workspaceId);
    	for(WorkspaceMember wm : allMembers) {
    		wm.setCard(personService.getCardUrl(pcc, wm.getMember()));
    	}
    	return allMembers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkspaceMember getMember(String workspaceId, String uid) {
        WorkspaceMember result = null;

        // Workspace members
        List<WorkspaceMember> members = this.getAllMembers(workspaceId);

        for (WorkspaceMember member : members) {
            if (StringUtils.equalsIgnoreCase(member.getMember().getUid(), uid)) {
                result = member;
                break;
            }
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(String workspaceId, Person owner) {
        this.create(workspaceId, Arrays.asList(WorkspaceRole.values()), owner);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(String workspaceId, List<WorkspaceRole> roles, Person owner) {
        // Creation of the member group
        CollabProfile members = this.applicationContext.getBean(sample.getClass());
        String cn = workspaceId + "_members";
        members.setCn(cn);
        members.setWorkspaceId(workspaceId);
        members.setType(WorkspaceGroupType.space_group);
        members.setDn(this.sample.buildDn(cn));

        // Get a fresh copy of the owner in the directory
        owner = personService.getPersonNoCache(owner.getDn());
        
        this.dao.create(members);

        // The owner is a member of the workspace
        this.attachPerson(owner, members);

        // Création of security groups
        for (WorkspaceRole entry : roles) {
            CollabProfile roleGroup = this.applicationContext.getBean(sample.getClass());
            String cnRole = workspaceId + "_" + entry.getId();
            roleGroup.setCn(cnRole);
            roleGroup.setWorkspaceId(workspaceId);
            roleGroup.setType(WorkspaceGroupType.security_group);
            roleGroup.setRole(entry);
            roleGroup.setDn(this.sample.buildDn(cnRole));

            this.dao.create(roleGroup);

            // Define the owner
            if (WorkspaceRole.OWNER.equals(entry)) {
                this.attachPerson(owner, roleGroup);
            }
        }

        ldapLogger.info("Space created : "+workspaceId);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // @CacheEvict(key = "#workspaceId", value = "membersByWksCache")
    public void delete(String workspaceId, String docId) {
        List<CollabProfile> list = this.findByWorkspaceId(workspaceId);

        List<Person> allPers = new ArrayList<Person>();
        for (CollabProfile cp : list) {
            if (cp.getType() == WorkspaceGroupType.space_group) {
                Person searchPers = this.applicationContext.getBean(personSample.getClass());

                List<Name> profilesDn = new ArrayList<Name>();
                profilesDn.add(cp.getDn());
                searchPers.setProfiles(profilesDn);

                // find all the members
                allPers = this.personService.findByCriteria(searchPers);
            }
        }

        // unlink all groups to all persons
        for (Person p : allPers) {
            for (CollabProfile cp : list) {
                this.detachPerson(p, cp, false);
            }
        }

        // remove all groups about this workspace
        for (CollabProfile cp : list) {
            this.dao.delete(cp);
        }

        ldapLogger.info("Space deleted : "+workspaceId);


        // Delete workspace
        // Nuxeo controller
        NuxeoController nuxeoController = new NuxeoController(this.getPortletContext());
        nuxeoController.setAuthType(NuxeoCommandContext.AUTH_TYPE_SUPERUSER);

        // Nuxeo command
        INuxeoCommand command = this.applicationContext.getBean(PurgeWorkspaceCommand.class, docId);
        nuxeoController.executeNuxeoCommand(command);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // @CacheEvict(key = "#workspaceId", value = "membersByWksCache")
    public WorkspaceMember addOrModifyMember(String workspaceId, Name memberDn, WorkspaceRole role) {
        List<CollabProfile> list = this.findByWorkspaceId(workspaceId);

        for (CollabProfile cp : list) {
            // add this new member in the members group (if needed)
            if (cp.getType() == WorkspaceGroupType.space_group) {
                this.attachPerson(memberDn, cp);
            }

            if ((cp.getType() == WorkspaceGroupType.security_group) && (cp.getRole() == role)) {
                this.attachPerson(memberDn, cp);
            }

            if ((cp.getType() == WorkspaceGroupType.security_group) && (cp.getRole() != role)) {
                this.detachPerson(memberDn, cp, true);
            }
        }

        Person person = this.personService.getPerson(memberDn);
        WorkspaceMemberImpl member = new WorkspaceMemberImpl(person);
        member.setRole(role);

        ldapLogger.info("Space member modified : "+workspaceId+ " "+person.getUid()+ " ("+role.toString()+")");


        return member;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // @CacheEvict(key = "#workspaceId", value = "membersByWksCache")
    public void removeMember(String workspaceId, Name memberDn) {
        List<CollabProfile> list = this.findByWorkspaceId(workspaceId);

        for (CollabProfile cp : list) {
            this.detachPerson(memberDn, cp, true);
        }

        ldapLogger.info("Space member removed : "+workspaceId+ " "+memberDn.toString());

    }


    /**
     * Attach person to a collab profile.
     * 
     * @param person person
     * @param profile collab profile
     */
    private void attachPerson(Person person, CollabProfile profile) {
        // Update indicator
        boolean update = false;

        // Update profile
        if (!profile.getUniqueMember().contains(person.getDn())) {
            profile.getUniqueMember().add(person.getDn());

            this.dao.update(profile);

            update = true;
        }

        // Update person
        if (!person.getProfiles().contains(profile.getDn())) {
            person.getProfiles().add(profile.getDn());

            this.personService.update(person);

            update = true;
        }

        // Update workspace document
        if (update && WorkspaceGroupType.space_group.equals(profile.getType())) {
            this.updateWorkspace(profile.getWorkspaceId(), person.getUid(), true);
        }

    }


    /**
     * Attach person to a collab profile.
     * 
     * @param memberDn member DN
     * @param cp collab profile
     */
    private void attachPerson(Name memberDn, CollabProfile cp) {
        Person person = this.personService.getPersonNoCache(memberDn);
        this.attachPerson(person, cp);
    }


    /**
     * Detach person from a collab profile.
     * 
     * @param p person
     * @param profile collab profile
     * @param updateNuxeo true if update Nuxeo
     */
    private void detachPerson(Person person, CollabProfile profile, boolean updateNuxeo) {
        // Update indicator
        boolean update = false;

        // Update profile
        if (profile.getUniqueMember().contains(person.getDn())) {
            profile.getUniqueMember().remove(person.getDn());

            this.dao.update(profile);

            update = true;
        }

        // Update person
        if (person.getProfiles().contains(profile.getDn())) {
            person.getProfiles().remove(profile.getDn());

            this.personService.update(person);

            update = true;
        }

        // Update workspace
        if (update && WorkspaceGroupType.space_group.equals(profile.getType())) {
        	try {
        		this.updateWorkspace(profile.getWorkspaceId(), person.getUid(), false);
        	}
        	catch(Exception e) {

    	        ldapLogger.error("Cannot remove "+person.getUid()+" from "+profile.getWorkspaceId()+". Space not found. "+e.getClass());

        		if (!(e instanceof NuxeoException)) {
        			throw e;
        		}
        	}
        }

    }


    /**
     * Detach person from a collab profile.
     * 
     * @param memberDn member DN
     * @param cp collab profile
     * @param updateNuxeo update Nuxeo
     */
    private void detachPerson(Name memberDn, CollabProfile cp, boolean updateNuxeo) {
        Person person = this.personService.getPersonNoCache(memberDn);
        this.detachPerson(person, cp, updateNuxeo);
    }


    /**
     * Update workspace.
     * 
     * @param workspaceId workspace identifier
     * @param user user identifier
     * @param attach true if user is attached, false if user is detached
     */
    private void updateWorkspace(String workspaceId, String user, boolean attach) {
        // Nuxeo controller
        NuxeoController nuxeoController = new NuxeoController(this.getPortletContext());
        nuxeoController.setAuthType(NuxeoCommandContext.AUTH_TYPE_SUPERUSER);

        // Modify members lists
        INuxeoCommand updateWorkspace = this.applicationContext.getBean(UpdateWorkspaceCommand.class, workspaceId, user, attach);
        nuxeoController.executeNuxeoCommand(updateWorkspace);

        // Get the UserProfile UUID
        INuxeoCommand getUserProfile = this.applicationContext.getBean(GetUserProfileCommand.class, user);
        Object profile = nuxeoController.executeNuxeoCommand(getUserProfile);
        if(profile != null) {
        	Document nxProfile = (Document) profile;

        	INuxeoCommand reindexUser = this.applicationContext.getBean(ReIndexUserCommand.class, nxProfile);
            nuxeoController.executeNuxeoCommand(reindexUser);

        }

    }


    /**
     * Update workspace.
     *
     * @param workspaceId workspace identifier
     * @param user user identifier
     * @param attach true if user is attached, false if user is detached
     */
    private Document getWorkspaceDocument(String workspaceId) {
        // Nuxeo controller
        NuxeoController nuxeoController = new NuxeoController(this.getPortletContext());
        nuxeoController.setAuthType(NuxeoCommandContext.AUTH_TYPE_SUPERUSER);

        // Nuxeo command
        INuxeoCommand command = this.applicationContext.getBean(GetWorkspaceCommand.class, workspaceId);
        Documents documents = (Documents) nuxeoController.executeNuxeoCommand(command);

        return documents.get(0);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollabProfile createLocalGroup(String workspaceId, String displayName, String description) {
        List<CollabProfile> list = this.findByWorkspaceId(workspaceId);

        // search the max ID setted in the group and add 1.
        int i = 1;
        for (CollabProfile cp : list) {
            if (cp.getType() == WorkspaceGroupType.local_group) {
                String cpSuffix = cp.getCn().replace(workspaceId + "_", "");

                int parseInt = Integer.parseInt(cpSuffix);
                if (parseInt >= i) {
                    i = parseInt + 1;
                }
            }
        }

        // local group creation
        CollabProfile localGroup = this.applicationContext.getBean(sample.getClass());
        String cn = workspaceId + "_" + Integer.toString(i);
        localGroup.setCn(cn);
        localGroup.setWorkspaceId(workspaceId);
        localGroup.setType(WorkspaceGroupType.local_group);
        localGroup.setDisplayName(displayName);
        if (StringUtils.isNotBlank(description)) {
            localGroup.setDescription(description);
        }
        localGroup.setDn(this.sample.buildDn(cn));

        this.dao.create(localGroup);

        ldapLogger.info("Local group created : "+workspaceId+ " "+displayName);


        return localGroup;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMemberToLocalGroup(String workspaceId, Name localGroupDn, Name memberDn) {
        List<CollabProfile> list = this.findByWorkspaceId(workspaceId);

        for (CollabProfile cp : list) {
            if (cp.getType() == WorkspaceGroupType.space_group) {
                if (cp.getUniqueMember().contains(memberDn)) {
                    CollabProfile localGroup = this.dao.findByDn(localGroupDn);

                    if (localGroup.getType() == WorkspaceGroupType.local_group) {
                        this.attachPerson(memberDn, localGroup);
                    }
                }
            }
        }

        ldapLogger.info("Local group member modified : "+workspaceId+ " "+memberDn.toString()+ " > "+localGroupDn.toString()+")");

    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMemberToLocalGroup(String workspaceId, String localGroupCn, String memberUid) {
        Name localGroupDn = this.getEmptyProfile().buildDn(localGroupCn);
        Name memberDn = this.personService.getEmptyPerson().buildDn(memberUid);
        this.addMemberToLocalGroup(workspaceId, localGroupDn, memberDn);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMemberFromLocalGroup(String workspaceId, Name localGroupDn, Name memberDn) {
        CollabProfile localGroup = this.dao.findByDn(localGroupDn);
        if (localGroup.getType() == WorkspaceGroupType.local_group) {
            this.detachPerson(memberDn, localGroup, true);
        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMemberFromLocalGroup(String workspaceId, String localGroupCn, String memberUid) {
        Name localGroupDn = this.getEmptyProfile().buildDn(localGroupCn);
        Name memberDn = this.personService.getEmptyPerson().buildDn(memberUid);
        this.removeMemberFromLocalGroup(workspaceId, localGroupDn, memberDn);

        ldapLogger.info("Local group member removed : "+workspaceId+ " "+memberUid+ " > "+localGroupCn);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyLocalGroup(CollabProfile localGroup) {
        this.dao.update(localGroup);

        ldapLogger.info("Local group modified : "+localGroup.getCn());

    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLocalGroup(String workspaceId, Name dn) {
        CollabProfile groupToRemove = this.dao.findByDn(dn);

        Person searchPers = this.applicationContext.getBean(personSample.getClass());

        List<Name> profilesDn = new ArrayList<Name>();
        profilesDn.add(groupToRemove.getDn());
        searchPers.setProfiles(profilesDn);

        // find all the members and unlink them
        List<Person> personToDetach = this.personService.findByCriteria(searchPers);
        for (Person p : personToDetach) {
            this.detachPerson(p, groupToRemove, true);
        }

        this.dao.delete(groupToRemove);

        ldapLogger.info("Local group removed : "+workspaceId + " "+dn.toString());

    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLocalGroup(String workspaceId, String cn) {
        Name dn = this.sample.buildDn(cn);
        this.removeLocalGroup(workspaceId, dn);

    }


    @Override
    public boolean checkGroups(String workspaceId) {

    	boolean ret = false;

    	logger_integrity.info("checkGroups starts for "+workspaceId+".");

    	Map<Name, Boolean> names = new HashMap<Name, Boolean>();

    	List<WorkspaceMember> allMembers = getAllMembers(workspaceId);
    	for(WorkspaceMember member : allMembers) {
    		names.put(member.getMember().getDn(), Boolean.FALSE);
    	}

    	Document workspaceDocument = getWorkspaceDocument(workspaceId);

    	PropertyList spaceMembers = workspaceDocument.getProperties().getList("ttcs:spaceMembers");
    	for (int i = 0; i < spaceMembers.size(); i++) {
    		PropertyMap member = spaceMembers.getMap(i);

    		String login = member.getString("login");
    		Name dn = personSample.buildDn(login);

    		// if found
    		Boolean found = names.get(dn);
    		if(found != null) {

    	    	logger_integrity.debug(dn.toString() +" found.");

    			names.put(dn, Boolean.TRUE);
    		}
    		else {
    			// if not found, report a problem
    			logger_integrity.error("No member found in workspace with dn "+dn);
    			ret = true;
    		}

    	}

    	for(Map.Entry<Name, Boolean>  entry : names.entrySet()) {
    		if(entry.getValue() == Boolean.FALSE) {
    			logger_integrity.error("Orphan member found in workspace with dn "+entry.getKey().toString());
    			ret = true;
    		}
    	}

    	return ret;

    }

    @Override
    public void sendIntegrityAlert(PortalControllerContext pcc) {

		NuxeoController nuxeoController = new NuxeoController(pcc.getPortletCtx());

        IFormsService formsService = nuxeoController.getNuxeoCMSService().getFormsService();

        Map<String, String> variables = new HashMap<>();

		try {
            formsService.start(pcc, LDAP_INTEGRITY_MODEL_WEBID, variables);
		} catch (PortalException | FormFilterException e) {
			logger_integrity.error("Unable to send notification", e);
		}

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
