package io.netnotes.system.nodes;

import io.netnotes.noteBytes.NoteBytesReadOnly;

public class NodeConstants {
    public static final NoteBytesReadOnly LIST_INSTALLED    = new NoteBytesReadOnly("list_installed");
    public static final NoteBytesReadOnly LIST_INSTANCES    = new NoteBytesReadOnly("list_instances");

    public static final NoteBytesReadOnly INSTALL_PACKAGE   = new NoteBytesReadOnly("install_package");
    public static final NoteBytesReadOnly UNINSTALL_PACKAGE = new NoteBytesReadOnly("uninstall_package");
    public static final NoteBytesReadOnly LOAD_NODE         = new NoteBytesReadOnly("load_node");
    public static final NoteBytesReadOnly UNLOAD_INSTANCE   = new NoteBytesReadOnly("unload_instance");
    public static final NoteBytesReadOnly UPDATE_CONFIG     = new NoteBytesReadOnly("update_config");
    public static final NoteBytesReadOnly BROWSE_PACKAGES   = new NoteBytesReadOnly("browse_packages");
    public static final NoteBytesReadOnly UPDATE_CACHE      = new NoteBytesReadOnly("update_cache");

    public static final NoteBytesReadOnly PACKAGE_INFO      = new NoteBytesReadOnly("package_info");
    public static final NoteBytesReadOnly PROCESS_CONFIG    = new NoteBytesReadOnly("process_config");
    public static final NoteBytesReadOnly POLICY_MANIFEST   = new NoteBytesReadOnly("policy_manifest");
    public static final NoteBytesReadOnly LOAD_IMMEDIATELY  = new NoteBytesReadOnly("load_immediately");

    public static final NoteBytesReadOnly INSTALLED_PACKAGE = new NoteBytesReadOnly("installed_package");
    public static final NoteBytesReadOnly PACKAGES          = new NoteBytesReadOnly("packages");
    public static final NoteBytesReadOnly PACKAGE_COUNT     = new NoteBytesReadOnly("package_count");
    public static final NoteBytesReadOnly REPOSITORY        = new NoteBytesReadOnly("repository");
    public static final NoteBytesReadOnly DOWNLOAD_URL      = new NoteBytesReadOnly("download_url");
    public static final NoteBytesReadOnly MANIFEST          = new NoteBytesReadOnly("manifest");
    public static final NoteBytesReadOnly LOAD_TIME         = new NoteBytesReadOnly("load_time");
    public static final NoteBytesReadOnly CRASH_COUNT       = new NoteBytesReadOnly("crash_count");
    public static final NoteBytesReadOnly UPTIME            = new NoteBytesReadOnly("uptime");
    public static final NoteBytesReadOnly SECURITY_POLICY   = new NoteBytesReadOnly("security_policy");
    public static final NoteBytesReadOnly INSTALLED_PATH   = new NoteBytesReadOnly("install_path");
    public static final NoteBytesReadOnly INSTALLED_DATE   = new NoteBytesReadOnly("installed_date");
   
    

    public static final NoteBytesReadOnly BY_PROCESS        = new NoteBytesReadOnly("by_process");
    public static final NoteBytesReadOnly BY_PACKAGE        = new NoteBytesReadOnly("by_package");
    public static final NoteBytesReadOnly DELETE_DATA       = new NoteBytesReadOnly( "delete_data");
}
