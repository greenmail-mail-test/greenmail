<%@ page session="false" contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="javax.servlet.ServletContext,java.util.List,java.util.Collection" %>
<%@ page import="com.icegreen.greenmail.Managers,com.icegreen.greenmail.util.Service,com.icegreen.greenmail.user.GreenMailUser,com.icegreen.greenmail.user.UserManager,com.icegreen.greenmail.store.MailFolder,com.icegreen.greenmail.imap.ImapHostManager" %>
<%@ page import="com.icegreen.greenmail.webapp.Constants" %>

<%@include file="header.jsp" %>
<%

ServletContext ctx = getServletContext();

Object attribute = ctx.getAttribute( Constants.GM_MANAGERS_ATTRIBUTE_NAME );
com.icegreen.greenmail.Managers managers = null;
if( attribute != null ){
	managers = (com.icegreen.greenmail.Managers)attribute;
}

attribute = ctx.getAttribute( Constants.GM_SERVICES_ATTRIBUTE_NAME );
List<Service> services = null;
if( attribute != null ){
	services = (List<Service>)attribute;
}
%>
        <div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
          <h1 class="page-header">Green mail Overview:</h1>
          
          <div class="row placeholders">
<%  if( services != null ){
		if( !services.isEmpty() ){
		%><h2 class="sub-header">Listing active services: </h2>
		
         <div class="table-responsive">
			 <table style="width:100%">
			 <thead>
			 <tr><th>ClassName</th><th>isRunning</th></tr>
			 </thead>
			 <tbody>
			 <%
			    String running = "";
				for( Service service : services ){
					running = service.isRunning()? "true": "false";
					 %>
					 <tr><td><%=service.getClass().getName()%></td><td><%=running%></td></tr>
					 <%	 
				}
			 %>
			 </tbody>
			 </table>
		</div>
		 <%
		}else{
			%>
			<h2 style="color:red;"> No services registered!</h2>
			 <%	
		}
 }else{
 %>
	<h2 style="color:red;"> Unexpected context state, no services registered!</h2>
 <%
 }



if( managers != null ) {
	 UserManager userManager = managers.getUserManager();
	 ImapHostManager imapHostManager = managers.getImapHostManager();
	 Collection< GreenMailUser> users = userManager.listUser();
	 if( users != null && !users.isEmpty() ){
		 %><h2 class="sub-header">Listing registered users: </h2>
         <div class="table-responsive">
		 <table style="width:100%">
		 <thead>
		 <tr><th>Login</th><th>&nbsp;</th><th>Email</th><th>Password</th><th>Mail Count</th></tr>
		 </thead>
		 <tbody>
		 <%
		 int messageCount = 0;
		 MailFolder mailFolder;
		 for( GreenMailUser greenMailUser : users ){
			 mailFolder = imapHostManager.getInbox(greenMailUser);
			 if( mailFolder != null ){
				 	messageCount = mailFolder.getMessageCount();
			 }else{
					messageCount = -1;
			 }
			
			 %>
			 <tr><td><%=greenMailUser.getLogin()%><td><a class="btn btn-primary btn-lg"  class="btn btn-lg btn-primary"  href="messages.jsp?mailbox=<%=greenMailUser.getLogin()%>">View Mailbox</a></td></td><td><%=greenMailUser.getEmail()%></td><td><%=greenMailUser.getPassword()%></td><td><%=messageCount%></td></tr>
			 <%	 
		 }
		 %>
		 </tbody>
		 </table>
		</div>
		 <%
	 }else{
		 %><h2 style="color:red"> No user available</h2> <%
	 }
} else { %>
	<h2 style="color:red;"> Unexpected context state, no managers registered!</h2>
<%
}

%>
</div>
</div>
<%@include file="footer.jsp" %>