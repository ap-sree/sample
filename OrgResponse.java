package com.sreemat.ldap.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Organization
 */
public class OrgResponse {
    private String dn;
    private String name;
    private String branch;
    private List<OrgResponse> subOrgs;
    
    public OrgResponse() {
        this.subOrgs = new ArrayList<>();
    }
    
    public OrgResponse(String dn, String name, String branch) {
        this.dn = dn;
        this.name = name;
        this.branch = branch;
        this.subOrgs = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getDn() {
        return dn;
    }
    
    public void setDn(String dn) {
        this.dn = dn;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public List<OrgResponse> getSubOrgs() {
        return subOrgs;
    }
    
    public void setSubOrgs(List<OrgResponse> subOrgs) {
        this.subOrgs = subOrgs;
    }
    
    public void addSubOrg(OrgResponse subOrg) {
        this.subOrgs.add(subOrg);
    }
}

/**
 * Response DTO for Group
 */
class GroupResponse {
    private String dn;
    private String name;
    private String orgDn;
    private String orgName;
    private String branch;
    private List<String> members;
    
    public GroupResponse() {
        this.members = new ArrayList<>();
    }
    
    public GroupResponse(String dn, String name, String orgDn, String orgName, String branch) {
        this.dn = dn;
        this.name = name;
        this.orgDn = orgDn;
        this.orgName = orgName;
        this.branch = branch;
        this.members = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getDn() {
        return dn;
    }
    
    public void setDn(String dn) {
        this.dn = dn;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getOrgDn() {
        return orgDn;
    }
    
    public void setOrgDn(String orgDn) {
        this.orgDn = orgDn;
    }
    
    public String getOrgName() {
        return orgName;
    }
    
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public List<String> getMembers() {
        return members;
    }
    
    public void setMembers(List<String> members) {
        this.members = members;
    }
}

/**
 * Generic API Response wrapper
 */
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    public ApiResponse() {}
    
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
}