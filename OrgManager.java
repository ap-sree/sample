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
    public List<String> getOrganizationsForUser(String uid, String branch, String orgName, boolean nested) {
        List<String> visibleOrgs = new ArrayList<>();
        String branchDN = ldapDAO.getBranchDN(branch);
        
        if (branchDN == null) {
            return visibleOrgs;
        }
        
        if (PermissionUtils.isSuperAdmin(uid)) {
            // Super admin can see all organizations
            return getAllOrganizationsInBranch(branch, orgName, nested);
        }
        
        if (orgName != null) {
            // Search for specific organization
            String specificOrgDN = findOrganizationDN(orgName, branch);
            if (specificOrgDN != null && PermissionUtils.canViewOrganization(uid, specificOrgDN)) {
                visibleOrgs.add(specificOrgDN);
                
                // Add sub-organizations based on nested parameter (only if user can view them)
                if (nested) {
                    // nested=true: get all sub-orgs (SCOPE_SUB) that user can view
                    List<String> allSubOrgs = getSubOrganizations(specificOrgDN);
                    for (String subOrgDN : allSubOrgs) {
                        if (PermissionUtils.canViewOrganization(uid, subOrgDN)) {
                            visibleOrgs.add(subOrgDN);
                        }
                    }
                } else {
                    // nested=false: get direct sub-orgs only (SCOPE_ONE) that user can view
                    List<String> directSubOrgs = getDirectSubOrganizations(specificOrgDN);
                    for (String subOrgDN : directSubOrgs) {
                        if (PermissionUtils.canViewOrganization(uid, subOrgDN)) {
                            visibleOrgs.add(subOrgDN);
                        }
                    }
                }
            }
        } else {
            // Get organizations where user is admin (and their sub-orgs if they can view them)
            List<String> userAdminOrgs = getOrganizationsUserAdmins(uid, branch);
            
            for (String adminOrgDN : userAdminOrgs) {
                visibleOrgs.add(adminOrgDN);
                
                if (nested) {
                    // Add all sub-orgs that user can view
                    List<String> allSubOrgs = getSubOrganizations(adminOrgDN);
                    for (String subOrgDN : allSubOrgs) {
                        if (PermissionUtils.canViewOrganization(uid, subOrgDN)) {
                            visibleOrgs.add(subOrgDN);
                        }
                    }
                } else {
                    // Add only direct sub-orgs that user can view
                    List<String> directSubOrgs = getDirectSubOrganizations(adminOrgDN);
                    for (String subOrgDN : directSubOrgs) {
                        if (PermissionUtils.canViewOrganization(uid, subOrgDN)) {
                            visibleOrgs.add(subOrgDN);
                        }
                    }
                }
            }
        }
        
        return visibleOrgs;
    }
    
    /**
     * Get all organizations in branch (for super admin only)
     */
    private List<String> getAllOrganizationsInBranch(String branch, String orgName, boolean nested) {
        List<String> allOrgs = new ArrayList<>();
        String branchDN = ldapDAO.getBranchDN(branch);
        
        if (branchDN == null) {
            return allOrgs;
        }
        
        if (orgName != null) {
            // Get specific org and its sub-orgs
            String specificOrgDN = findOrganizationDN(orgName, branch);
            if (specificOrgDN != null) {
                allOrgs.add(specificOrgDN);
                
                if (nested) {
                    allOrgs.addAll(getSubOrganizations(specificOrgDN));
                } else {
                    allOrgs.addAll(getDirectSubOrganizations(specificOrgDN));
                }
            }
        } else {
            // Get all organizations in branch
            int searchScope = nested ? LDAPConnection.SCOPE_SUB : LDAPConnection.SCOPE_ONE;
            List<LDAPEntry> orgEntries = ldapDAO.search(branchDN, searchScope, LdapConstants.SEARCH_OU_FILTER);
            
            for (LDAPEntry entry : orgEntries) {
                String entryDN = entry.getDN();
                
                // Skip the "ou=groups" entries
                if (entryDN.contains("ou=groups,ou=groups") || entryDN.startsWith("ou=groups,")) {
                    continue;
                }
                
                allOrgs.add(entryDN);
            }
        }
        
        return allOrgs;
    }
    
    /**
     * Get sub-organizations of a parent organization (all levels - SCOPE_SUB)
     */
    public List<String> getSubOrganizations(String parentOrgDN) {
        List<String> subOrgs = new ArrayList<>();
        
        List<LDAPEntry> entries = ldapDAO.search(parentOrgDN, LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_OU_FILTER);
        
        for (LDAPEntry entry : entries) {
            String entryDN = entry.getDN();
            // Exclude "ou=groups" entries and the parent org itself
            if (!entryDN.startsWith("ou=groups,") && !entryDN.equals(parentOrgDN)) {
                subOrgs.add(entryDN);
            }
        }
        
        return subOrgs;
    }
    
    /**
     * Get direct sub-organizations of a parent organization (one level only - SCOPE_ONE)
     */
    public List<String> getDirectSubOrganizations(String parentOrgDN) {
        List<String> directSubOrgs = new ArrayList<>();
        
        List<LDAPEntry> entries = ldapDAO.search(parentOrgDN, LDAPConnection.SCOPE_ONE, LdapConstants.SEARCH_OU_FILTER);
        
        for (LDAPEntry entry : entries) {
            String entryDN = entry.getDN();
            // Exclude "ou=groups" entries
            if (!entryDN.startsWith("ou=groups,")) {
                directSubOrgs.add(entryDN);
            }
        }
        
        return directSubOrgs;
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
     * Find organization DN by name and branch (searches recursively)
     */
    public String findOrganizationDN(String orgName, String branch) {
        return PermissionUtils.findOrgDNInBranch(orgName, branch);
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
            if (entryDN.contains("ou=groups,ou=groups") || entryDN.startsWith("ou=groups,")) {
                continue;
            }
            
            // Check if user is org admin of this specific organization (not parent)
            if (PermissionUtils.isOrgAdmin(uid, entryDN)) {
                adminOrgs.add(entryDN);
            }
        }
        
        return adminOrgs;
    }
}