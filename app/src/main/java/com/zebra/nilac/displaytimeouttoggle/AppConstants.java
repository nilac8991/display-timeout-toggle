package com.zebra.nilac.displaytimeouttoggle;

public class AppConstants {

    public static final String PROFILE_NAME = "DisplayStayAwakeToggle";
    public static final String ENABLE_STAY_AWAKE_PROFILE =
            "<wap-provisioningdoc>\n" +
                    "  <characteristic type=\"Profile\">\n" +
                    "    <parm name=\"ProfileName\" value=\"DisplayStayAwakeToggle\"/>\n" +
                    "    <characteristic version=\"4.3\" type=\"DisplayMgr\">\n"
                    + "      <parm name=\"StayAwake\" value=\"1\" />\n"
                    + "    </characteristic>\n" +
                    "  </characteristic>" +
                    "</wap-provisioningdoc>";

    public static final String DISABLE_STAY_AWAKE_PROFILE =
            "<wap-provisioningdoc>\n" +
                    "  <characteristic type=\"Profile\">\n" +
                    "    <parm name=\"ProfileName\" value=\"DisplayStayAwakeToggle\"/>\n" +
                    "    <characteristic version=\"4.3\" type=\"DisplayMgr\">\n"
                    + "      <parm name=\"StayAwake\" value=\"2\" />\n"
                    + "    </characteristic>\n" +
                    "  </characteristic>" +
                    "</wap-provisioningdoc>";
}
