<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@page import="org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter"%><html><head>


    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <link href="css/main.css" media="screen" rel="stylesheet" type="text/css"/>
    <title>${journal.title}</title>


    <!-- Popup related includes -->
    <script type="text/javascript" src="js/jquery-1.4.2.min.js"></script>
    <script type="text/javascript" src="js/jquery.jmpopups-0.5.1.js"></script>
    <script type="text/javascript" src="js/jquery-ui-1.7.2.custom.min.js"></script>
    <script type="text/javascript" src="js/jquery.simplemodal-1.3.5.min.js"></script>
    <!--  End popup includes -->

     <!--  JQuery Lightbox includes -->
    <script type="text/javascript" src="js/jquery.lightbox-0.5.min.js"></script>
    <link rel="stylesheet" href="css/jquery.lightbox-0.5.css" type="text/css" media="screen" />
    <!--  End JQuery Lightbox includes -->

    <link href="css/jquery-ui.css" rel="stylesheet" type="text/css"/>

    <!-- DWR includes -->
    <script type="text/javascript" src="dwr/interface/AjaxController.js"></script>
    <script type="text/javascript" src="dwr/engine.js"></script>
    <!-- End DWR includes -->


    <script type="text/javascript" src="js/travellog.js"></script>

    <script type="text/javascript">

        function screenSetup () {
            //If an error occurred on one of the popup forms, we need to redisplay the form.
            //Controller will pass the name of the div and then we reopen it
            if (${popupScreen!=null}) {
                openPopup('${popupScreen}');
                fieldDiv = document.getElementById("field");
                fieldDiv.style.display='block';
            }
            else if (${bootstrap?"true":"false"}) {
                //We're in bootstrap mode to set up a new journal

                //If the user object has an ID it was created so move on to journal setup
                if (${!usercreated}) {
                    fieldDiv = document.getElementById("field");
                    fieldDiv.style.display ='none';
                    openPopup('bootstrap_div');
                }
                else {
                    //User was created so open up the form to create the journal
                    fieldDiv = document.getElementById("field");
                    fieldDiv.style.display='none';
                    openPopup('journal_div');
                }
            }
            else {
                //Not in bootstrap mode or an error state so show the page
                fieldDiv = document.getElementById("field");
                fieldDiv.style.display='block';
            }
        }

        //Initialize the date pickers in the various forms
        $(document).ready(function() {
            $("#photoDate").datepicker( {
                altField : '#photoDate',
                altFormat : 'mm/dd/yy'
            });

            $("#entryDate").datepicker( {
                altFormat : 'mm/dd/yy',
                altField : '#entryDate'
            });

            $("#journal_start_date").datepicker( {
                altField : '#journal_start_date',
                altFormat : 'mm/dd/yy'
            });
            $("#journal_end_date").datepicker( {
                altField : '#journal_end_date',
                altFormat : 'mm/dd/yy'
            });

        });



    </script>
  </head><body onload="screenSetup();">
    <div id="field">
      <div id="main">
        <div id="header">
          <a href="">
            <img alt="MyTravelLog" class="mtllogo" src="images/mtl-logo-274x51.png" height="51" width="274"/>
          </a>
          <div id="actions">
            <div class="addJournal">
              <sec:authorize ifAllGranted="ROLE_ADMIN">
                <a href="javascript:" onclick="newEntry()">
                <img alt="" src="images/plus-box-16x16.png" height="16" width="16"/>
                Add Journal Entry
              </a>
              </sec:authorize>
            </div>
            <sec:authorize ifAllGranted="ROLE_ADMIN">
                <span class="sep"> | </span>
                <div class="addJournal">
                <a href="javascript:" onclick="manageJournal()">Manage</a>
                </div>
                <span class="sep"> | </span>
            </sec:authorize>
            <div class="signOut">
            <sec:authorize ifNotGranted="ROLE_ADMIN"><a href="javascript:" onclick="openPopup('login_div')">
               Sign in</a>
            </sec:authorize>
            <sec:authorize ifAllGranted="ROLE_ADMIN">
            <a href="<c:url value="/j_spring_security_logout"/>">Sign out</a></sec:authorize>
            </div>
          </div>
        </div>
        <div id="intro">
          <div id="titleBox">
            <h1>
              ${journal.title}
            </h1>
            <div class="dates">
              ${journal.dateRangeString}
            </div>
          </div>
          <div class="text">
            ${journal.description}
          </div>
        </div>
        <div id="entries">
            <c:forEach var="entry" items="${entries}" varStatus="status">
                  <c:set var="openCloseEntry" >
                  ${status.index>2?"closedDefault entry":"entry open"}
                  </c:set>


                  <div class="${openCloseEntry}">
                <div class="liner">
                  <div class="header">
                    <div class="expando"></div>
                    <h2>
                      ${entry.title}
                    </h2>
                    <div class="meta">
                      <span class="first">${entry.destination}</span>
                      <span>${entry.formattedDate}</span>
                    </div>
                  </div>
                  <div class="body">
                      <c:if test="${photoMap[entry.id][0]!=null}">
                    <div class="mainPhoto">
                        <img width="148" src="${photoMap[entry.id][0].thumbnailPath}"/>
                    </div>
                     </c:if>
                    <div class="content">
                     <c:if test="${fn:length(photoMap[entry.id])>0}" >
                      <div class="photoStrip scroll">
                          <c:if test="${fn:length(photoMap[entry.id])>9}">
                        <div class="ctl prev" onclick="shiftGallery('gallery_${status.index}_index','${entry.id}',-1,${fn:length(photoMap[entry.id])})"> </div>
                        </c:if>
                        <span class="photos" id="gallery_${entry.id}">
                            <!--
                                This is a little hard to read but lightbox allows us to set text using the title attribute
                                of the href.  So we insert the html we need for edit/delete of an image as well as the name
                                and description.
                             -->
                            <c:forEach var="photo" items="${photoMap[entry.id]}" varStatus="img_status">
                              <a href="${photo.websizePath}" id="photo_${entry.id}_${img_status.index}" rel="lightbox[${entry.id}]"
                                title="[&lt;a href=&quot;javascript:editPhoto('${photo.id}')&quot;&gt;Edit&lt;/a&gt; | &lt;a href=&quot;javascript:deletePhoto('${photo.id}')&quot;&gt;Delete&lt;/a&gt;]&lt;br/&gt; ${fn:replace(photo.title,'&quot;','&quot;')}
                                &lt;br/&gt;${fn:replace(photo.description,'&quot;','&quot;')}">
                                <img alt="" class="photo" src="${photo.thumbnailPath}" height="44" width="57"/>
                              </a>
                             </c:forEach>
                        </span>
                        <script type="text/javascript">

                        //Activate the lightbox for this gallery
                        $(function() {
                             $('#gallery_${entry.id} a').lightBox();
                         });

                        </script>

                        <c:if test="${fn:length(photoMap[entry.id])>9}">
                            <input type="hidden" id="gallery_${status.index}_index" value="0"/>
                            <div class="ctl next" onclick="shiftGallery('gallery_${status.index}_index','${entry.id}',1,${fn:length(photoMap[entry.id])})"></div>
                          </c:if>
                      </div>
                      </c:if>
                      <div class="liner">
                        <div class="synopsis">
                          ${entry.entryText}
                        </div>
                        <div class="actions">
                          <ul>
                              <sec:authorize ifAllGranted="ROLE_ADMIN">
                            <li>
                             <a href="javascript:" onclick="editEntry('${entry.id}')">edit text</a>
                            </li>
                            </sec:authorize>
                            <sec:authorize ifAllGranted="ROLE_ADMIN">
                            <li>
                                <a href="javascript:" onclick="deleteEntry('${entry.id}')">delete this entry</a>
                            </li>
                            </sec:authorize>
                            <li class="last">
                              <sec:authorize ifAllGranted="ROLE_ADMIN"><a href="javascript:" onclick="uploadPhoto('${entry.id}')">
                                <img alt="" src="images/plus-box-16x16.png" height="16" width="16"/>
                                add new image
                              </a></sec:authorize>
                            </li>
                          </ul>
                        </div>
                        <div class="commentsBlock">
                          <div class="head">
                            <h3>Comments</h3>
                          </div>
                          <div class="comments">
                              <c:forEach var="comment" items="${commentMap[entry.id]}">
                                <div class="comment">
                                  <div class="user">
                                    ${comment.commenter.name} <sec:authorize ifAllGranted="ROLE_ADMIN">(${comment.commenter.email})</sec:authorize>
                                    -
                                  </div>
                                  <div class="text">
                                    ${comment.body}
                                  </div>
                                  <div class="date">
                                    ${comment.formattedDate}
                                    <sec:authorize ifAllGranted="ROLE_ADMIN">
                                        - <a href="javascript:" onclick="deleteComment('${comment.id}')">delete</a>
                                    </sec:authorize>
                                  </div>
                                </div>
                             </c:forEach>
                          </div>
                          <div class="addComment">
                            <a href="javascript:" onclick="addComment('${entry.id}')">
                              <img alt="" src="images/plus-box-16x16.png" height="16" width="16">
                              add new comment
                            </a>
                          </div>
                        </div>
                      </div>

                    </div>
                  </div>
                </div>
               </div>
                </c:forEach>
          </div>
        </div>
        <div id="footer">
            <a href="http://aws.amazon.com/">
              <img alt="Powered by AWS" class="pbawslogo" src="images/aws-logo-white-142x59.png" height="40" width="109"/>
            </a>
        </div>
      </div>
      <!-- Popup divs are included from seperate JSP to cut down on clutter -->
      <%@include file="popups.jsp" %>
  <script src="js/main.js" type="text/javascript"></script>

</body></html>