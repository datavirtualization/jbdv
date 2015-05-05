<%@ page import="java.io.File,java.io.BufferedReader,java.io.FileReader,java.util.Arrays,java.lang.management.ManagementFactory,javax.management.MBeanServer,javax.management.ObjectName" %>
<!DOCTYPE html>
<html>
<head>

    <title>JBoss Data Virtualization 6</title>
    <!-- proper charset -->
    <meta http-equiv="content-type" content="text/html;charset=utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE8" />
    <link rel="stylesheet" type="text/css" href="eap.css" />
    <link rel="shortcut icon" href="favicon.ico" />
</head>

<body>

<div id="container">

  <!-- header -->
  <div id="header">

        <!-- logo section -->
        <table border=0 style="height:100%">
        <tr style="vertical-align:bottom;">
        <td style="padding-left:15px;">
                        <img src="images/prod_name.png"/>
                </td>
                <td class="prod-version">
                        6
                </td>

        </tr>
        </table>
  </div>

  <!-- main content -->
  <div id="content">

    <div class="section">

      <h1>Welcome to JBoss Data Virtualization 6</h1>

      <h3>Your JBoss Enterprise Application Platform is running.</h3>

      <%
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("jboss.as:socket-binding-group=standard-sockets,socket-binding=management-http");
        Integer version = (Integer) server.getAttribute(name, "boundPort");

        String consoleUrl = request.getScheme() + "://" + request.getServerName() + ":" + version + "/console";
      %>

       <p>
        <a href="<%=consoleUrl%>">Administration Console</a> |
        <a href="https://access.redhat.com/site/documentation/JBoss_Enterprise_Application_Platform/">Documentation</a> |
        <a href="https://access.redhat.com/groups/jboss-enterprise-middleware">Online User Groups</a> <br/>
      </p>

      <%
        File dir = new File(application.getRealPath("/consoles"));
        File[] consoles = dir.listFiles();
      %>

      <% if (consoles != null && consoles.length != 0) { %>
        <h4>Other consoles:</h4>
        <ul id="consoles">
          <%
            Arrays.sort (consoles);
            for (File console: consoles) {
                BufferedReader br = new BufferedReader(new FileReader(console));
                String line;
                try {
                    line = br.readLine();
                } finally {
                    br.close();
                }
          %>
            <li><a href="/<%=console.getName()%>"><%=line%></a></li>
          <% } %>
        </ul>
      <% } %>

      <sub>To replace this page set "enable-welcome-root" to false in your server configuration and deploy
      your own war with / as its context path.</sub>

    </div>

  </div>

  <!-- footer -->
  <div id="footer">
      &nbsp;
  </div>

</div>

</body >
</html>
