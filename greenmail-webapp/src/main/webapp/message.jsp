
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
String messageId = request.getParameter( "messageId" );
%>
        <div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
          <h1 class="page-header">Green mail user mailbox:<%=targetUser%> message</h1>
          
          <div class="row placeholders">
<%

if( managers != null ) {
	 UserManager userManager = managers.getUserManager();
	 ImapHostManager imapHostManager = managers.getImapHostManager();
	 GreenMailUser greenMailUser = userManager.getUser( targetUser);
	 if( greenMailUser != null ){
		 %>
		 <h2 class="sub-header">Showing message from user mailbox: </h2>
		 <%
		 MailFolder mailFolder;
		 mailFolder = imapHostManager.getInbox(greenMailUser);
		 if( mailFolder != null ){
			 	DateFormat formatter = SimpleDateFormat.getDateTimeInstance();
			 	StoredMessage message =  mailFolder.getMessage( Long.parseLong( messageId ) );
			 	if( message != null ){
	 		%>
	 		
         <div class="panel panel-primary">
	 		
			<% 
			 		String receivedDate = formatter.format( message.getReceivedDate() );
			 		String subject = message.getMimeMessage().getSubject();
			 		javax.mail.Address[] from = message.getMimeMessage().getFrom();
			 		javax.mail.Address[] recipients = message.getMimeMessage().getAllRecipients();
			 		
					 %>
					 <div class="panel-heading">
					 	<div class="panel-title">
					 	<table style="width:100%">
							 <thead>
							 <tr><th>From</th><th>Subject</th><th>ReceivedDate</th><th>Recipients</th></tr>
							 </thead>
							 <tbody>
							 <tr><td><%=from[0].toString()%></td><td><%=subject%></td><td><%= receivedDate%></td>
					 <td><%
						for( int i = 0; i < recipients.length; i++ ){
							javax.mail.Address address = recipients[i]; 
							%><%=address.toString()%><%
							if( i + 1 < recipients.length ) { %> <br> <%}
						}
					  %></td></tr>
					 		</tbody></table>
					 	</div>
					 </div>
					 <div class="panel-body">
						<%
						Object content = message.getMimeMessage().getContent();
		                if (content instanceof javax.mail.internet.MimeMultipart) {
		                	javax.mail.internet.MimeMultipart multipart = (javax.mail.internet.MimeMultipart) content;
		                    %><%= multipart.getBodyPart(0).getContent() %> <%
		                } else {
		                	 %><%= content%> <%
		                }
							
						%>
					 </div>
					 <div class="panel-footer">
					 
					 <% 
					 	java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
					 	message.getMimeMessage().writeTo( baos ); 
					 
					 %>
					 	<table style="width:100%">
							 <thead>
							 <tr><th>Raw:</th></tr>
							 </thead>
							 <tbody>
							 <tr><td><pre>
							 
							 <![CDATA[
							 
							 <%=new String( baos.toByteArray(), "UTF-8")%> 
							 
							 ]]>
							 
							 </pre></td>
							 </tr>
							 </tbody>
						</table>
							 
					 </div>
		 </div>
			  <% 
			 	}else{
			 		%>
			 		<h2 style="color:red">No message</h2>
			 		<%
			 	}
		 }else{
			 %>
	 			<h2 style="color:red">No messagee</h2>
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