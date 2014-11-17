<!DOCTYPE html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
		<title><g:layoutTitle default="Grails"/></title>
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<link rel="shortcut icon" href="${assetPath(src: 'favicon.ico')}" type="image/x-icon">
		<g:set var="entityName" value="${message(code: 'vacationRequest.label', default: 'VacationRequest')}" />
  		<asset:stylesheet src="application.css"/>
		<asset:javascript src="application.js"/>
		<g:layoutHead/>
	</head>
	<body>
		<div id="grailsLogo" role="banner"><a href="http://grails.org"><asset:image src="grails_logo.png" alt="Grails"/></a></div>

    <div class="nav" role="navigation">
        <ul>
            <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
			<li><g:link class="list" controller="task" action="index"><g:message code="default.tasks.label" default="My Tasks ({0})" args="[session.userTaskCount?:0]" /></g:link></li>
			<li><g:link class="list" controller="vacationRequest" action="index"><g:message code="default.vacationRequest.label" default="Vacation Request"/></g:link></li>
			<li><g:link class="create" controller="user" action="index"><g:message code="default.user.label" default="User Info"/></g:link></li>
        </ul>
    </div>

        <g:layoutBody/>
		<div class="footer" role="contentinfo"></div>
	</body>
</html>
