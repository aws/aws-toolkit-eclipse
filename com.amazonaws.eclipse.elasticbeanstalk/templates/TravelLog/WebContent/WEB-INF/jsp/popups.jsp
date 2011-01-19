      <c:if test="${bootstrap!=null}">
      <div class="travellog_popup" id="bootstrap_div">
      		<h1>Create Account</h1>
			<form:form name="createAccountForm" method="post" action="createAccount.do" commandName="user" >
				<spring:bind path="user">
					<c:if test="${not empty status.errorMessages}">
						<div class="error">
						<c:forEach var="error" items="${status.errorMessages}">
							Error: <c:out value="${error}" escapeXml="false"/><br/>
						</c:forEach>
						</div>
					</c:if>
				</spring:bind>
				<div class="popupFieldLabel">Username:</div>
				<div class="popupFieldInput"><form:input path="username"/></div>
				
				<div class="popupFieldLabel">Password:</div>
				<div class="popupFieldInput"><form:password path="password"/></div>
				
				<div class="popupFieldLabel">Re-Type Password:</div>
				<div class="popupFieldInput"><input type="password" name="password2"/></div>
				
				<div class="popupSubmitButtons">
					<a class="saveButton" href="javascript:" onclick="document.createAccountForm.submit()">create account</a>
				</div>
				
				
			</form:form>
		</div>
		<div class="travellog_popup" id="journal_div" >
		
			<form:form method="post" id="journalForm" name="journalForm" action="createJournal.do" commandName="journal" onsubmit="wait('journal_wait')">
				<spring:bind path="journal">
					<c:if test="${not empty status.errorMessages}">
						<div class="error">
						<c:forEach var="error" items="${status.errorMessages}">
							Error: <c:out value="${error}" escapeXml="false"/><br/>
						</c:forEach>
						</div>
					</c:if>
				</spring:bind>
				
				<h1>Journal Setup</h1>
				<div class="popupFieldLabel">Start with...</div>
				<div class="popupFieldLabel">
					<input checked onclick="toggleJournalType(false)" type="radio" name="preload" value="false"/>New Journal<br/>
					<input onclick="toggleJournalType(true)" type="radio" name="preload" value="true"/>Sample Journal
				</div>
				
				<div class="popupFieldLabel">Title:</div>
				<div class="popupFieldInput"><form:input path="title"/></div>
			
				<div class="popupFieldLabel">Start Date: <form:errors path="startDate" cssClass="error"/></div>	
					
				<div id="journal_start_datepicker" class="popupFieldDateInput">
					<form:input path="startDate" id="journal_start_date"/>
				</div>
				
				<div class="popupFieldLabel">End Date: <form:errors path="endDate" cssClass="error"/></div>	
				
				<div id="journal_end_datepicker" class="popupFieldDateInput"> 
					<form:input path="endDate"  id="journal_end_date"/>
				</div>
				
				<div class="popupFieldLabel">Description:</div>
				<div class="popupFieldInput"><form:textarea path="description" rows="5"/></div>
				
				
				<div class="popupSubmitButtons">
					<img src="images/wait.gif" style="display: none; float: right" id="journal_wait"/>
					<a class="saveButton" href="javascript:" onclick="document.journalForm.submit()">create journal</a>
				</div>
			</form:form>
		</div> 
      </c:if>
      <div class="travellog_popup" id="entry_div">
      		<H1>Edit Entry</H1>
      		<div class="popupForm">
			<form:form name="entryForm"  method="post" action="saveEntry.do" commandName="entry" onsubmit="wait('entry_wait')">
				<form:hidden path="id"/><br/>
				<spring:bind path="entry">
					<c:if test="${not empty status.errorMessages}">
						<div class="error">
						<c:forEach var="error" items="${status.errorMessages}">
							Error: <c:out value="${error}" escapeXml="false"/><br/>
						</c:forEach>
						</div>
					</c:if>
				</spring:bind>
				<div class="popupFieldLabel">Title:</div>
				<div class="popupFieldInput"><form:input path="title"/></div>
				<div class="popupFieldLabel">Destination:</div>
				<div class="popupFieldInput"><form:input path="destination"/></div>
				<div class="popupFieldLabel">Date: <form:errors path="date" cssClass="error"/></div> 
				<div  class="popupFieldDateInput" id="entry_datepicker">
				<form:input path="date" id="entryDate"/><br/>
				</div>
				
			    <div class="popupFieldLabel">Entry:</div>
				<div class="popupFieldInput"><form:textarea path="entryText" rows="10"/></div>
				<div class="popupSubmitButtons">
				<a class="saveButton" href="javascript:" onclick="wait('entry_wait');document.entryForm.submit()">save</a>
				<a class="cancelButton" href="javascript:" onclick="$.modal.close();">cancel</a>
				<img src="images/wait.gif" style="display: none; float: right" id="entry_wait"/>
				</div>
			
			</form:form>
			</div>
	  </div>
	  <div class="travellog_popup" id="photo_div">
	  		<H1>Upload Photo</H1>
			<form:form name="photoForm" method="post" action="uploadPhoto.do" onsubmit="wait('photo_wait')" commandName="photo" enctype="multipart/form-data">
				<spring:bind path="photo">
					<c:if test="${not empty status.errorMessages}">
						<div class="error">
						<c:forEach var="error" items="${status.errorMessages}">
							Error: <c:out value="${error}" escapeXml="false"/><br/>
						</c:forEach>
						</div>
					</c:if>
				</spring:bind>
				<input type="hidden" name="id" value=""/>
				<input type="hidden" name="entryId" value="0"/>
				<div class="popupFieldLabel"><br/>File:</div>
				<div class="popupFieldInput"><input type="file" name="file"/></div>
				<div class="popupFieldLabel">Title:</div>
				<div class="popupFieldInput"><form:input path="title"/></div>
				
				<div class="popupFieldLabel">Location/Subject:</div>
				<div class="popupFieldInput"><form:input path="subject"/></div>
				
				<div class="popupFieldLabel">Date: <form:errors path="date" cssClass="error"/></div>
				<div class="popupFieldDateInput" id="photo_datepicker">
					<form:input path="date" id="photoDate"/>
				</div>
				
				<div class="popupFieldLabel">Description:</div>
				<div class="popupFieldInput"><form:textarea path="description" rows="5" cols="60"/></div>
				
				<div class="popupSubmitButtons">
					
					<a class="saveButton" href="javascript:" onclick="wait('photo_wait');document.photoForm.submit()">save</a>
					<a class="cancelButton" href="javascript:" onclick="$.modal.close();">cancel</a>
					<img src="images/wait.gif" style="display: none; float: right" id="photo_wait"/>
				
				</div>
				
			</form:form>
      </div>
      
      <div class="travellog_popup" id="comment_div">
      		<h1>Add Comment</h1>
			<form:form name="commentForm"  method="post" action="saveComment.do" commandName="comment" onsubmit="wait('comment_wait')">
				<form:hidden path="id"/><br/>
				<div class="popupFieldLabel">
				<spring:bind path="comment">
					<c:if test="${not empty status.errorMessages}">
						<div class="error">
						<c:forEach var="error" items="${status.errorMessages}">
							Error: <c:out value="${error}" escapeXml="false"/><br/>
						</c:forEach>
						<br/>
						</div>
					</c:if>
				</spring:bind>
				</div>
				<form:input type="hidden" path="entry.id"/>
				<div class="popupFieldLabel">*Name:</div>
				<div class="popupFieldInput"><form:input path="commenter.name"/></div>
				
				<div class="popupFieldLabel">E-mail: (will not be displayed)</div>
				<div class="popupFieldInput"><form:input path="commenter.email"/> </div>
				
				<div class="popupFieldLabel"><input type="checkbox" name="emailComments" value="true"/>&nbsp;E-mail me follow-up comments<br/><br/></div>
				
				<div class="popupFieldLabel">*Comment:</div>
				<div class="popupFieldInput"><form:textarea path="body" rows="10" cols="60"/></div>
				
				<div class="popupSubmitButtons">
					
					<a class="saveButton" href="javascript:" onclick="wait('comment_wait');document.commentForm.submit()">save</a>
					<a class="cancelButton" href="javascript:" onclick="$.modal.close();">cancel</a>
					<img src="images/wait.gif" style="display: none; float: right" id="comment_wait"/>
				
				</div>
				
			</form:form>
	  </div>
      <div id="login_div" class="travellog_popup">
      	<h1>Sign in</h1>
		<form action="j_spring_security_check" name="loginForm" method="post">
			<c:if test="${popupScreen!=null}">
				<div class="popupFieldLabel">
				<div class="error"><br/>Bad username/password</div>
				</div>
			</c:if>
			<div class="popupFieldLabel"><br/>Username:</div>
			<div class="popupFieldInput">
				<input name="j_username"/>
			</div>
			
			<div class="popupFieldLabel">Password:</div>
			<div class="popupFieldInput"><input type="password" name="j_password"/></div>
			
			<input type="hidden" value="on" name="_spring_security_remember_me" style="display: none"/>
			
			<div class="popupSubmitButtons">
				<a class="saveButton" href="javascript:" onclick="document.loginForm.submit()">sign in</a>
				<a class="cancelButton" href="javascript:" onclick="$.modal.close();">cancel</a>
			
			</div>
		</form>
      </div>
      
      <div id="manage_div" class="travellog_popup">
      	<form:form name="backupForm" action="backupRestore.do" onsubmit="return confirmBackupRestore()">
      	<h1>Backup/Restore Journal</h1>
      	
      	<div class="popupFieldLabel"><br/>Bucket:</div>
		<div class="popupFieldInput"><input name="backupBucket"/></div>
		
      	<div class="popupFieldLabel">Path:</div>
		<div class="popupFieldInput"><input name="backupPath"/></div>
		
		<div class="popupFieldLabel">
			<input type="radio" name="backupRestoreFlag" checked value="backup">&nbsp;Backup
			&nbsp;&nbsp;
			<input type="radio" name="backupRestoreFlag" value="restore">&nbsp;Restore
		</div>
		
		<div class="popupSubmitButtons">
			<a class="saveButton" href="javascript:" onclick="wait('manage_wait');document.backupForm.submit()">submit</a>
			<a class="cancelButton" href="javascript:" onclick="$.modal.close();">cancel</a>
			<img src="images/wait.gif" style="display: none; float: right" id="manage_wait"/>
		</div>
      
      	</form:form>
			
		
	  </div>