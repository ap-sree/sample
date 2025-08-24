package com.sreemat.ldap.manager;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.sreemat.ldap.constants.LdapConstants;
import com.sreemat.ldap.dao.LdapDAO;
import com.sreemat.ldap.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager class for Organization operations
 */
public class OrgManager {
    
    private final LdapDAO ldapDAO;
    
    public OrgManager() {
        this.ldapDAO = new LdapDAO();
    }
    
    /**
     * Get list of organizations that user can view in a specific branch
     */
    public List<String> getOrganizationsForUser(String uid, String branch, String orgName, boolean includeSubOrgs) {
        List<String> visibleOrgs = new ArrayList<>();
        String branchDN = ldapDAO.getBranchDN(branch);
        
        if (branchDN == null) {
            return visibleOrgs;
        }
        
        // Search all organizations in the branch
        List<LDAPEntry> orgEntries = ldapDAO.search(branchDN, LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_OU_FILTER);
        
        for (LDAPEntry entry : orgEntries) {
            String entryDN = entry.getDN();
            
            // Skip the "ou=groups" entries
            if (entryDN.contains("ou=groups,ou=groups")) {
                continue;
            }
            
            // Check if user can view this organization
            if (PermissionUtils.canViewOrganization(uid, entryDN)) {
                String currentOrgName = PermissionUtils.extractOrgName(entryDN);
                
                // Filter by org name if specified
                if (orgName == null || orgName.equals(currentOrgName)) {
                    visibleOrgs.add(entryDN);
                    
                    // Add sub-organizations if requested
                    if (includeSubOrgs) {
                        visibleOrgs.addAll(getSubOrganizations(entryDN));
                    }
                }
            }
        }
        
        return visibleOrgs;
    }
    
    /**
     * Get sub-organizations of a parent organization
     */
    public List<String> getSubOrganizations(String parentOrgDN) {
        List<String> subOrgs = new ArrayList<>();
        
        List<LDAPEntry> entries = ldapDAO.search(parentOrgDN, LDAPConnection.SCOPE_ONE, LdapConstants.SEARCH_OU_FILTER);
        
        for (LDAPEntry entry : entries) {
            String entryDN = entry.getDN();
            // Exclude "ou=groups" entries
            if (!entryDN.startsWith("ou=groups,")) {
                subOrgs.add(entryDN);
            }
        }
        
        return subOrgs;
    }
    
    /**
     * Create new organization
     */
    public boolean createOrganization(String orgName, String branch) {
        String branchDN = ldapDAO.getBranchDN(branch);
        if (branchDN == null) {
            return false;
        }
        
        String orgDN = "ou=" + orgName + "," + branchDN;
        
        try {
            // Create organization OU
            boolean orgCreated = ldapDAO.createOU(orgDN, orgName);
            if (!orgCreated) {
                return false;
            }
            
            // Create groups OU inside organization
            String groupsOUDN = "ou=groups," + orgDN;
            boolean groupsOUCreated = ldapDAO.createOU(groupsOUDN, "groups");
            if (!groupsOUCreated) {
                return false;
            }
            
            // Create domain administrator group
            String adminGroupDN = "cn=" + LdapConstants.DOMAIN_ADMIN_CN + "," + orgDN;
            boolean adminGroupCreated = ldapDAO.createGroup(adminGroupDN, LdapConstants.DOMAIN_ADMIN_CN);
            
            return adminGroupCreated;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Create sub-organization
     */
    public boolean createSubOrganization(String subOrgName, String parentOrgDN) {
        String subOrgDN = "ou=" + subOrgName + "," + parentOrgDN;
        
        try {
            // Create sub-organization OU
            boolean subOrgCreated = ldapDAO.createOU(subOrgDN, subOrgName);
            if (!subOrgCreated) {
                return false;
            }
            
            // Create groups OU inside sub-organization
            String groupsOUDN = "ou=groups," + subOrgDN;
            boolean groupsOUCreated = ldapDAO.createOU(groupsOUDN, "groups");
            if (!groupsOUCreated) {
                return false;
            }
            
            // Create domain administrator group for sub-organization
            String adminGroupDN = "cn=" + LdapConstants.DOMAIN_ADMIN_CN + "," + subOrgDN;
            boolean adminGroupCreated = ldapDAO.createGroup(adminGroupDN, LdapConstants.DOMAIN_ADMIN_CN);
            
            return adminGroupCreated;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Add organization admin
     */
    public boolean addOrgAdmin(String orgDN, String adminUid) {
        String adminGroupDN = "cn=" + LdapConstants.DOMAIN_ADMIN_CN + "," + orgDN;
        String memberDN = "uid=" + adminUid + "," + adminGroupDN;
        
        return ldapDAO.addMemberToGroup(adminGroupDN, memberDN);
    }
    
    /**
     * Remove organization admin
     */
    public boolean removeOrgAdmin(String orgDN, String adminUid) {
        String adminGroupDN = "cn=" + LdapConstants.DOMAIN_ADMIN_CN + "," + orgDN;
        String memberDN = "uid=" + adminUid + "," + adminGroupDN;
        
        return ldapDAO.removeMemberFromGroup(adminGroupDN, memberDN);
    }
    
    /**
     * Check if organization exists
     */
    public boolean organizationExists(String orgDN) {
        return ldapDAO.entryExists(orgDN);
    }
    
    /**
     * Find organization DN by name and branch
     */
    public String findOrganizationDN(String orgName, String branch) {
        String branchDN = ldapDAO.getBranchDN(branch);
        if (branchDN == null) {
            return null;
        }
        
        // First check direct child
        String directOrgDN = "ou=" + orgName + "," + branchDN;
        if (ldapDAO.entryExists(directOrgDN)) {
            return directOrgDN;
        }
        
        // Search recursively in branch for organization with this name
        List<LDAPEntry> entries = ldapDAO.search(branchDN, LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_OU_FILTER);
        
        for (LDAPEntry entry : entries) {
            String entryDN = entry.getDN();
            String currentOrgName = PermissionUtils.extractOrgName(entryDN);
            if (orgName.equals(currentOrgName)) {
                return entryDN;
            }
        }
        
        return null;
    }
    
    /**
     * Get all organizations that user is admin of (for any level)
     */
    public List<String> getOrganizationsUserAdmins(String uid, String branch) {
        List<String> adminOrgs = new ArrayList<>();
        String branchDN = ldapDAO.getBranchDN(branch);
        
        if (branchDN == null) {
            return adminOrgs;
        }
        
        // Search all organizations in the branch
        List<LDAPEntry> orgEntries = ldapDAO.search(branchDN, LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_OU_FILTER);
        
        for (LDAPEntry entry : orgEntries) {
            String entryDN = entry.getDN();
            
            // Skip "ou=groups" entries
            if (entryDN.contains("ou=groups,ou=groups")) {
                continue;
            }
            
            if (PermissionUtils.isOrgAdmin(uid, entryDN)) {
                adminOrgs.add(entryDN);
            }
        }
        
        return adminOrgs;
    }
}