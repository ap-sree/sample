package com.sreemat.ldap.constants;

/**
 * LDAP Constants for directory structure and queries
 */
public class LdapConstants {
    
    // Base DN
    public static final String BASE_DN = "o=sreemat";
    
    // Branch DNs
    public static final String INTERNAL_BRANCH = "ou=internal," + BASE_DN;
    public static final String EXTERNAL_BRANCH = "ou=external," + BASE_DN;
    
    // Groups DNs
    public static final String INTERNAL_GROUPS = "ou=groups," + INTERNAL_BRANCH;
    public static final String EXTERNAL_GROUPS = "ou=groups," + EXTERNAL_BRANCH;
    
    // Super Admin Group
    public static final String SUPER_ADMIN_GROUP = "cn=SuperAdministrators," + INTERNAL_GROUPS;
    
    // Admin Group Names
    public static final String DOMAIN_ADMIN_CN = "DomainAdministrator";
    public static final String GROUP_ADMIN_CN = "GroupAdministrator";
    
    // LDAP Object Classes
    public static final String ORGANIZATIONAL_UNIT = "organizationalUnit";
    public static final String GROUP_OF_NAMES = "groupOfNames";
    
    // LDAP Search Filters
    public static final String SEARCH_OU_FILTER = "(objectClass=organizationalUnit)";
    public static final String SEARCH_GROUP_FILTER = "(objectClass=groupOfNames)";
    public static final String SEARCH_MEMBER_FILTER = "(member=uid={0},{1})";
    public static final String SEARCH_UID_FILTER = "(uid={0})";
    public static final String SEARCH_ADMIN_MEMBER_FILTER = "(member=uid={0},cn={1},{2})";
    
    // LDAP Attributes
    public static final String ATTR_OBJECT_CLASS = "objectclass";
    public static final String ATTR_OU = "ou";
    public static final String ATTR_CN = "cn";
    public static final String ATTR_MEMBER = "member";
    
    // Branch Types
    public static final String BRANCH_INTERNAL = "internal";
    public static final String BRANCH_EXTERNAL = "external";
    
    // Private constructor to prevent instantiation
    private LdapConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}