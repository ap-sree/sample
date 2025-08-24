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
            if (nextParent.equals(parentDN)) {
                break; // Avoid infinite loop
            }
            parentDN = nextParent;
        }
        
        return false;
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