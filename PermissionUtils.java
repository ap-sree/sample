package com.sreemat.ldap.utils;

import com.sreemat.ldap.constants.LdapConstants;
import com.sreemat.ldap.dao.LdapDAO;

/**
 * Utility class for checking user permissions
 */
public class PermissionUtils {
    
    private static final LdapDAO ldapDAO = new LdapDAO();
    
    /**
     * Check if user is Super Admin
     */
    public static boolean isSuperAdmin(String uid) {
        return ldapDAO.isMemberOfGroup(uid, LdapConstants.SUPER_ADMIN_GROUP);
    }
    
    /**
     * Check if user is Organization Admin for a specific organization
     */
    public static boolean isOrgAdmin(String uid, String orgDN) {
        String adminGroupDN = "cn=" + LdapConstants.DOMAIN_ADMIN_CN + "," + orgDN;
        return ldapDAO.isMemberOfGroup(uid, adminGroupDN);
    }
    
    /**
     * Check if user is Group Admin for a specific group
     */
    public static boolean isGroupAdmin(String uid, String groupDN) {
        String adminGroupDN = "cn=" + LdapConstants.GROUP_ADMIN_CN + "," + groupDN;
        return ldapDAO.isMemberOfGroup(uid, adminGroupDN);
    }
    
    /**
     * Check if user is Organization Admin for any parent organization (recursive)
     * Also checks if user has org admin role in any org within the branch
     */
    public static boolean isOrgAdminOfParentOrg(String uid, String orgDN) {
        // Check current org
        if (isOrgAdmin(uid, orgDN)) {
            return true;
        }
        
        // Check parent organizations recursively
        String parentDN = getParentOrgDN(orgDN);
        while (parentDN != null && !parentDN.equals(orgDN)) {
            if (isOrgAdmin(uid, parentDN)) {
                return true;
            }
            String nextParent = getParentOrgDN(parentDN);
            if (nextParent == null || nextParent.equals(parentDN)) {
                break; // Avoid infinite loop
            }
            parentDN = nextParent;
        }
        
        return false;
    }
    
    /**
     * Check if user has any org admin role in a specific branch
     */
    public static boolean hasOrgAdminRoleInBranch(String uid, String branch) {
        String branchDN = ldapDAO.getBranchDN(branch);
        if (branchDN == null) {
            return false;
        }
        
        // Search for all organizations in the branch and check if user is admin of any
        java.util.List<com.novell.ldap.LDAPEntry> orgEntries = ldapDAO.search(branchDN, 
            com.novell.ldap.LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_OU_FILTER);
        
        for (com.novell.ldap.LDAPEntry entry : orgEntries) {
            String entryDN = entry.getDN();
            // Skip "ou=groups" entries
            if (entryDN.contains("ou=groups,ou=groups") || entryDN.startsWith("ou=groups,")) {
                continue;
            }
            
            if (isOrgAdmin(uid, entryDN)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if user can read organizations in a branch
     */
    public static boolean canReadOrganizations(String uid, String branch, String orgName) {
        if (isSuperAdmin(uid)) {
            return true;
        }
        
        if (orgName != null) {
            // Check specific organization
            String orgDN = buildOrgDN(orgName, branch);
            return orgDN != null && canViewOrganization(uid, orgDN);
        } else {
            // Check if user has any org admin role in the branch
            return hasOrgAdminRoleInBranch(uid, branch);
        }
    }
    
    /**
     * Check if user can create organization (only super admin)
     */
    public static boolean canCreateOrganization(String uid) {
        return isSuperAdmin(uid);
    }
    
    /**
     * Check if user can create sub-organization
     */
    public static boolean canCreateSubOrganization(String uid, String parentOrgDN) {
        return isSuperAdmin(uid) || isOrgAdminOfParentOrg(uid, parentOrgDN);
    }
    
    /**
     * Check if user can manage organization (add/remove org admins)
     */
    public static boolean canManageOrganization(String uid, String orgDN) {
        return isSuperAdmin(uid) || isOrgAdminOfParentOrg(uid, orgDN);
    }
    
    /**
     * Check if user can create group in an organization
     */
    public static boolean canCreateGroup(String uid, String orgDN) {
        return isSuperAdmin(uid) || isOrgAdminOfParentOrg(uid, orgDN);
    }
    
    /**
     * Check if user can manage group admins
     */
    public static boolean canManageGroupAdmins(String uid, String groupDN) {
        String orgDN = extractOrgDNFromGroup(groupDN);
        return isSuperAdmin(uid) || 
               isOrgAdminOfParentOrg(uid, orgDN) || 
               isGroupAdmin(uid, groupDN);
    }
    
    /**
     * Check if user can manage group members
     */
    public static boolean canManageGroupMembers(String uid, String groupDN) {
        return isGroupAdmin(uid, groupDN);
    }
    
    /**
     * Check if user can view organization
     */
    public static boolean canViewOrganization(String uid, String orgDN) {
        return isSuperAdmin(uid) || isOrgAdminOfParentOrg(uid, orgDN);
    }
    
    /**
     * Check if user can view groups in a branch
     */
    public static boolean canViewGroups(String uid, String branch) {
        return isSuperAdmin(uid) || hasOrgAdminRoleInBranch(uid, branch);
    }
    
    /**
     * Validate if branch is valid
     */
    public static boolean isValidBranch(String branch) {
        return LdapConstants.BRANCH_INTERNAL.equalsIgnoreCase(branch) || 
               LdapConstants.BRANCH_EXTERNAL.equalsIgnoreCase(branch);
    }
    
    /**
     * Extract organization DN from group DN
     * Example: cn=testgrp1,ou=groups,ou=testorg1,ou=groups,ou=internal,o=sreemat
     * Returns: ou=testorg1,ou=groups,ou=internal,o=sreemat
     */
    public static String extractOrgDNFromGroup(String groupDN) {
        if (groupDN == null || !groupDN.startsWith("cn=")) {
            return null;
        }
        
        // Find first comma, then skip "ou=groups,"
        int firstComma = groupDN.indexOf(",");
        if (firstComma != -1) {
            String remaining = groupDN.substring(firstComma + 1);
            // Skip "ou=groups,"
            if (remaining.startsWith("ou=groups,")) {
                return remaining.substring("ou=groups,".length());
            }
        }
        return null;
    }
    
    /**
     * Extract organization name from organization DN
     * Example: ou=testorg1,ou=groups,ou=internal,o=sreemat
     * Returns: testorg1
     */
    public static String extractOrgName(String orgDN) {
        if (orgDN != null && orgDN.startsWith("ou=")) {
            int commaIndex = orgDN.indexOf(",");
            if (commaIndex > 3) {
                return orgDN.substring(3, commaIndex);
            }
        }
        return null;
    }
    
    /**
     * Extract group name from group DN
     * Example: cn=testgrp1,ou=groups,ou=testorg1,ou=groups,ou=internal,o=sreemat
     * Returns: testgrp1
     */
    public static String extractGroupName(String groupDN) {
        if (groupDN != null && groupDN.startsWith("cn=")) {
            int commaIndex = groupDN.indexOf(",");
            if (commaIndex > 3) {
                return groupDN.substring(3, commaIndex);
            }
        }
        return null;
    }
    
    /**
     * Build organization DN from org name and branch
     */
    public static String buildOrgDN(String orgName, String branch) {
        String branchDN = ldapDAO.getBranchDN(branch);
        if (branchDN != null) {
            return "ou=" + orgName + "," + branchDN;
        }
        return null;
    }
    
    /**
     * Build group DN from group name, org name and branch
     */
    public static String buildGroupDN(String groupName, String orgName, String branch) {
        String orgDN = buildOrgDN(orgName, branch);
        if (orgDN != null) {
            return "cn=" + groupName + ",ou=groups," + orgDN;
        }
        return null;
    }
    
    /**
     * Find organization DN by searching in the branch (handles sub-orgs)
     */
    public static String findOrgDNInBranch(String orgName, String branch) {
        String branchDN = ldapDAO.getBranchDN(branch);
        if (branchDN == null) {
            return null;
        }
        
        // Search recursively in branch for organization with this name
        java.util.List<com.novell.ldap.LDAPEntry> entries = ldapDAO.search(branchDN, 
            com.novell.ldap.LDAPConnection.SCOPE_SUB, LdapConstants.SEARCH_OU_FILTER);
        
        for (com.novell.ldap.LDAPEntry entry : entries) {
            String entryDN = entry.getDN();
            // Skip "ou=groups" entries
            if (entryDN.contains("ou=groups,ou=groups") || entryDN.startsWith("ou=groups,")) {
                continue;
            }
            
            String currentOrgName = extractOrgName(entryDN);
            if (orgName.equals(currentOrgName)) {
                return entryDN;
            }
        }
        
        return null;
    }
    
    /**
     * Get parent organization DN
     * Example: ou=subOrg,ou=testorg1,ou=groups,ou=internal,o=sreemat
     * Returns: ou=testorg1,ou=groups,ou=internal,o=sreemat
     */
    private static String getParentOrgDN(String orgDN) {
        if (orgDN == null || !orgDN.startsWith("ou=")) {
            return null;
        }
        
        int firstComma = orgDN.indexOf(",");
        if (firstComma != -1) {
            String parentDN = orgDN.substring(firstComma + 1);
            // Check if we've reached the groups level
            if (parentDN.equals(LdapConstants.INTERNAL_GROUPS) || 
                parentDN.equals(LdapConstants.EXTERNAL_GROUPS)) {
                return null;
            }
            return parentDN;
        }
        return null;
    }
}