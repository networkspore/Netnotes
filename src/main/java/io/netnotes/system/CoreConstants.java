package io.netnotes.system;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.noteBytes.NoteBytesReadOnly;

public class CoreConstants {

    public static final String SYSTEM = "system";
    
    public static final String IO_DAEMON = "io-daemon";
    public static final String SERVICES = "services";
    public static final String RUNTIME = "runtime";
    public static final String NODE_CONTROLLER = "node-controller";
    public static final String REPOSITORIES = "repositories";
    public static final String NODE_REGISTRY = "node-registry";
    public static final String NODE_LOADER = "node-loader";
    public static final String RENDERING_SERVICE = "rendering-service";

    public static final String NODES = "nodes";
    public static final String CONTROLLERS = "controllers";

    public static final String DEFAULT_KEYBOARD_ID = "default-keyboard";

    public static final String SYSTEM_CONTAINER_NAME = "sys-term";


 

    public static final ContextPath SYSTEM_PATH = ContextPath.of(CoreConstants.SYSTEM);
    public static final ContextPath REPOSITORIES_PATH = SYSTEM_PATH.append(REPOSITORIES);
    public static final ContextPath SYSTEM_CONTAINER_HANDLE_PATH = SYSTEM_PATH.append(SYSTEM_CONTAINER_NAME);
    public static final ContextPath SYSTEM_DEFAULT_KEYBOARD_PATH = SYSTEM_PATH.append(DEFAULT_KEYBOARD_ID);

    public static final ContextPath SERVICES_PATH = SYSTEM_PATH.append(SERVICES);
    public static final ContextPath RENDERING_SERVICE_PATH = SERVICES_PATH.append(RENDERING_SERVICE);
    public static final ContextPath IO_DAEMON_PATH = SERVICES_PATH.append(IO_DAEMON);
    public static final ContextPath TERMINAL_LAYOUT_MANAGER_PATH = SERVICES_PATH.append(IO_DAEMON);
    
    public static final ContextPath RUNTIME_PATH = SYSTEM_PATH.append(RUNTIME);
    public static final ContextPath NODES_PATH = RUNTIME_PATH.append(NODES);

    public static final ContextPath CONTROLLERS_PATH = RUNTIME_PATH.append(CONTROLLERS);
    public static final ContextPath NODE_CONTROLLER_PATH = CONTROLLERS_PATH.append(NODE_CONTROLLER);
    public static final ContextPath NODE_LOADER_PATH = CONTROLLERS_PATH.append(NODE_LOADER);
    public static final ContextPath NODE_REGISTRY_PATH = CONTROLLERS_PATH.append(NODE_REGISTRY);

    // DATA PATHS
    public static final ContextPath NODE_DATA_PATH = ContextPath.of("data");
    public static final ContextPath RUNTIME_DATA = ContextPath.of("run");
    public static final ContextPath PACKAGE_STORE_PATH = RUNTIME_DATA.append("pkg");
    public static final ContextPath REPOSITORIES_DATA_PATH = RUNTIME_DATA.append(REPOSITORIES);
    public static final ContextPath NODE_LOADER_DATA_PATH = RUNTIME_DATA.append(NODE_LOADER);
    public static final ContextPath NODE_REGISTRY_DATA_PATH = RUNTIME_DATA.append(NODE_REGISTRY);

}
