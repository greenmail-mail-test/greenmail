
<%@ page session="false" contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="javax.servlet.ServletContext,java.util.List,java.util.Collection, java.util.Date,java.text.SimpleDateFormat,java.text.DateFormat" %>
<%@ page import="com.icegreen.greenmail.Managers,com.icegreen.greenmail.user.GreenMailUser,com.icegreen.greenmail.user.UserManager,com.icegreen.greenmail.store.MailFolder,com.icegreen.greenmail.store.StoredMessage,com.icegreen.greenmail.imap.ImapHostManager" %>
<%@ page import="com.icegreen.greenmail.webapp.Constants" %>


<%@include file="header.jsp" %>
<%

ServletContext ctx = getServletContext();

Object attribute = ctx.getAttribute( Constants.GM_MANAGERS_ATTRIBUTE_NAME );
com.icegreen.greenmail.Managers managers = null;
if( attribute != null ){
	managers = (com.icegreen.greenmail.Managers)attribute;
}


String targetUser = request.getParameter( "mailbox" );
%>
        <div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
          <h1 class="page-header">Green mail user mailbox:<%=targetUser%> </h1>
          
          <div class="row placeholders">
<%

if( managers != null ) {
	 UserManager userManager = managers.getUserManager();
	 ImapHostManager imapHostManager = managers.getImapHostManager();
	 GreenMailUser greenMailUser = userManager.getUser( targetUser);
	 if( greenMailUser != null ){
		 %>
		 <h2 class="sub-header">Listing user mailbox: </h2>
		 <%
		 int messageCount = 0;
		 MailFolder mailFolder;
		 mailFolder = imapHostManager.getInbox(greenMailUser);
		 if( mailFolder != null ){
			 	messageCount = mailFolder.getMessageCount();
			 	DateFormat formatter = SimpleDateFormat.getDateTimeInstance();
			 	List<StoredMessage> messages =  mailFolder.getMessages();
			 	if( messages != null && !messages.isEmpty() ){
	 		%>
	 		
         <div class="table-responsive">
	 		<table style="width:100%">
			 <thead>
			 <tr><th>From</th><th>Subject</th><th>ReceivedDate</th><th>Recipients</th></tr>
			 </thead>
			 <tbody>
			<% 
				 	for( StoredMessage message : messages){
				 		String receivedDate = formatter.format( message.getReceivedDate() );
				 		String subject = message.getMimeMessage().getSubject();
				 		javax.mail.Address[] from = message.getMimeMessage().getFrom();
				 		javax.mail.Address[] recipients = message.getMimeMessage().getAllRecipients();
				 		
						 %>
						 <tr><td><%=from[0].toString()%></td><td><a href="message.jsp?mailbox=<%=greenMailUser.getLogin()%>&messageId=<%=message.getUid()%>"><%=subject%> </a></td><td><%= receivedDate%></td>
						 <td><%
							for( int i = 0; i < recipients.length; i++ ){
								javax.mail.Address address = recipients[i]; 
								%><%=address.toString()%><%
								if( i + 1 < recipients.length ) { %> <br> <%}
							}
						  %></td></tr>
						 <%	 
					}
			  %>
			  </tbody>
			  </table>
		 </div>
			  <% 
			 	}else{
			 		%>
			 		<h2 style="color:red">No messages</h2>
			 		<%
			 	}
		 }else{
			 %>
	 			<h2 style="color:red">No messages</h2>
	 		<%
		 }
	 }else{
		 %><h2 style="color:red"> No user available</h2> <%
	 }
} else { 
%>
	<h2 style="color:red;"> Unexpected context state, no managers registered!</h2>
<%
}
	
%>
<a href="#" onclick="javascript:history.back();"> back </a>
</div>
</div>
<%@include file="footer.jsp" %>