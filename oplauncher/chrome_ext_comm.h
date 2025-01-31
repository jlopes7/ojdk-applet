#ifndef _CHROME_EXT_COMM_H
#define _CHROME_EXT_COMM_H

#define CHROME_EXT_FILETYPEJAR      "jar"
#define CHROME_EXT_FILETYPECLAZZ    "class"

#define CHROME_EXT_MSG_JARFILE      "archiveUrl"    /*Defines the name of the Jar file*/
#define CHROME_EXT_MSG_CLZZFILE     "className"     /*Defines the name of the Jar file*/
#define CHROME_EXT_MSG_FILETYPE     "fileType"      /*Defines if the asset is a Jar or Class file*/
#define CHROME_EXT_MSG_FILECONTENT  "fileContent"   /*Element that contains the B64 data of the binary file downloaded from Chrome*/
#define CHROME_EXT_MSG_PARAMETERS   "parameters"    /*Define the name of the element that contains the parameters*/
#define CHROME_EXT_MSG_OP           "op"            /*The name of the operation*/
#define CHROME_EXT_MSG_BASEURL      "baseUrl"       /*The applet base URL*/
#define CHROME_EXT_MSG_APPLETNAME   "appletName"    /*The Applet name*/
#define CHROME_EXT_MSG_CODEBASE     "codebase"      /*The applet code base*/
#define CHROME_EXT_MSG_COOKIES      "cookies"       /*All available cookies*/
#define CHROME_EXT_MSG_HEIGHT       "height"        /*The Applet configured height*/
#define CHROME_EXT_MSG_WIDTH        "width"         /*The Applet configured width*/
#define CHROME_EXT_MSG_POSX         "posx"          /*Location of the Applet in the screen under the X-axis*/
#define CHROME_EXT_MSG_POSY         "posy"          /*Location of the Applet in the screen under the Y-axis*/

#endif //CHROME_EXT_COMM_H
