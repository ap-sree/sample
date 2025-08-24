package com.sreemat.ldap.manager;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.sreemat.ldap.constants.LdapConstants;
import com.sreemat.ldap.dao.LdapDAO;
import com.sreemat.ldap.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager class for Group operations
 */
public class GroupManager {
    
    private final LdapDAO ldapDAO;
    private final OrgManager orgManager;
    
    public GroupManager() {
        this.ldapDAO = new LdapDAO();
        this.orgManager = new OrgManager();
    }
    
    /**
     * Get list of groups that user can view in a specific branch
     */
    public List<String> getGroupsForUser(String uid, String branch) {
        List<String> visibleGroups = new ArrayList<>();
        
        if (PermissionUtils.isSuperAdmin(uid)) {
            // Super admin can see all groups in the branch
            return getAllGroupsInBranch(branch);
        }
        
        // Get organizations where user is admin
        List<String> adminOrgs = orgManager.getOrganizationsUserAdmins(uid, branch);
        
        for (String orgDN : adminOrgs) {
            // Get all groups in this organization
            String groupsPath = "ou=groups," + orgDN;
            List<LDAPEntry> groupEntries = ldapDAO.search(groupsPath, LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_GROUP_FILTER);
            
            for (LDAPEntry entry : groupEntries) {
                String groupDN = entry.getDN();
                // Skip admin groups
                if (!isAdminGroup(groupDN)) {
                    visibleGroups.add(groupDN);
                }
            }
        }
        
        // Also add groups where user is group admin (but not org admin)
        List<String> groupAdminGroups = getGroupsWhereUserIsAdmin(uid, branch);
        for (String groupDN : groupAdminGroups) {
            if (!visibleGroups.contains(groupDN)) {
                visibleGroups.add(groupDN);
            }
        }
        
        return visibleGroups;
    }
    
    /**
     * Get all groups in a branch (for super admin)
     */
    private List<String> getAllGroupsInBranch(String branch) {
        List<String> allGroups = new ArrayList<>();
        String branchDN = ldapDAO.getBranchDN(branch);
        
        if (branchDN == null) {
            return allGroups;
        }
        
        List<LDAPEntry> groupEntries = ldapDAO.search(branchDN, LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_GROUP_FILTER);
        
        for (LDAPEntry entry : groupEntries) {
            String groupDN = entry.getDN();
            // Skip admin groups
            if (!isAdminGroup(groupDN)) {
                allGroups.add(groupDN);
            }
        }
        
        return allGroups;
    }
    
    /**
     * Get groups where user is group admin
     */
    private List<String> getGroupsWhereUserIsAdmin(String uid, String branch) {
        List<String> adminGroups = new ArrayList<>();
        String branchDN = ldapDAO.getBranchDN(branch);
        
        if (branchDN == null) {
            return adminGroups;
        }
        
        List<LDAPEntry> groupEntries = ldapDAO.search(branchDN, LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_GROUP_FILTER);
        
        for (LDAPEntry entry : groupEntries) {
            String groupDN = entry.getDN();
            // Skip admin groups and check if user is admin
            if (!isAdminGroup(groupDN) && PermissionUtils.isGroupAdmin(uid, groupDN)) {
                adminGroups.add(groupDN);
            }
        }
        
        return adminGroups;
    }
    
    /**
     * Check if group is an admin group (DomainAdministrator or GroupAdministrator)
     */
    private boolean isAdminGroup(String groupDN) {
        String groupName = PermissionUtils.extractGroupName(groupDN);
        return LdapConstants.DOMAIN_ADMIN_CN.equals(groupName) || 
               LdapConstants.GROUP_ADMIN_CN.equals(groupName);
    }
    
    /**
     * Create new group in organization
     */
    public boolean createGroup(String groupName, String orgDN) {
        String groupsPath = "ou=groups," + orgDN;
        String groupDN = "cn=" + groupName + "," + groupsPath;
        
        try {
            // Create the group
            boolean groupCreated = ldapDAO.createGroup(groupDN, groupName);
            if (!groupCreated) {
                return false;
            }
            
            // Create group administrator group
            String adminGroupDN = "cn=" + LdapConstants.GROUP_ADMIN_CN + "," + groupDN;
            boolean adminGroupCreated = ldapDAO.createGroup(adminGroupDN, LdapConstants.GROUP_ADMIN_CN);
            
            return adminGroupCreated;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Add group admin
     */
    public boolean addGroupAdmin(String groupDN, String adminUid) {
        String adminGroupDN = "cn=" + LdapConstants.GROUP_ADMIN_CN + "," + groupDN;
        String memberDN = "uid=" + adminUid + "," + adminGroupDN;
        
        return ldapDAO.addMemberToGroup(adminGroupDN, memberDN);
    }
    
    /**
     * Remove group admin
     */
    public boolean removeGroupAdmin(String groupDN, String adminUid) {
        String adminGroupDN = "cn=" + LdapConstants.GROUP_ADMIN_CN + "," + groupDN;
        String memberDN = "uid=" + adminUid + "," + adminGroupDN;
        
        return ldapDAO.removeMemberFromGroup(adminGroupDN, memberDN);
    }
    
    /**
     * Add group member
     */
    public boolean addGroupMember(String groupDN, String memberUid) {
        String memberDN = "uid=" + memberUid + "," + groupDN;
        return ldapDAO.addMemberToGroup(groupDN, memberDN);
    }
    
    /**
     * Remove group member
     */
    public boolean removeGroupMember(String groupDN, String memberUid) {
        String memberDN = "uid=" + memberUid + "," + groupDN;
        return ldapDAO.removeMemberFromGroup(groupDN, memberDN);
    }
    
    /**
     * Check if group exists
     */
    public boolean groupExists(String groupDN) {
        return ldapDAO.entryExists(groupDN);
    }
    
    /**
     * Find group DN by name, organization and branch
     */
    public String findGroupDN(String groupName, String orgName, String branch) {
        String orgDN = PermissionUtils.buildOrgDN(orgName, branch);
        if (orgDN == null) {
            return null;
        }
        
        String groupDN = "cn=" + groupName + ",ou=groups," + orgDN;
        if (ldapDAO.entryExists(groupDN)) {
            return groupDN;
        }
        
        return null;
    }
    
    /**
     * Get all groups in an organization
     */
    public List<String> getGroupsInOrganization(String orgDN) {
        List<String> groups = new ArrayList<>();
        String groupsPath = "ou=groups," + orgDN;
        
        List<LDAPEntry> groupEntries = ldapDAO.search(groupsPath, LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_GROUP_FILTER);
        
        for (LDAPEntry entry : groupEntries) {
            String groupDN = entry.getDN();
            // Skip admin groups
            if (!isAdminGroup(groupDN)) {
                groups.add(groupDN);
            }
        }
        
        return groups;
    }
    
    /**
     * Get group members
     */
    public List<String> getGroupMembers(String groupDN) {
        List<String> members = new ArrayList<>();
        
        LDAPEntry entry = ldapDAO.searchSingle(groupDN, LDAPConnection.SCOPE_BASE, "(objectClass=*)");
        if (entry != null) {
            com.novell.ldap.LDAPAttribute memberAttr = entry.getAttribute(LdapConstants.ATTR_MEMBER);
            if (memberAttr != null) {
                String[] memberValues = memberAttr.getStringValueArray();
                for (String member : memberValues) {
                    if (!member.isEmpty()) {
                        members.add(member);
                    }
                }
            }
        }
        
        return members;
    }
}