<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.osivia.org/jsp/taglib/osivia-portal" prefix="op" %>

<%@ page contentType="text/html" isELIgnored="false"%>


<portlet:defineObjects />


<portlet:actionURL name="save" var="saveUrl" />

<portlet:resourceURL id="password-information" var="passwordInformationUrl"/>


<c:set var="emptyPasswordLabel"><op:translate key="PASSWORD_EMPTY" /></c:set>
<c:set var="tooWeakPasswordLabel"><op:translate key="PASSWORD_TOO_WEAK" /></c:set>
<c:set var="weakPasswordLabel"><op:translate key="PASSWORD_WEAK" /></c:set>
<c:set var="goodPasswordLabel"><op:translate key="PASSWORD_GOOD" /></c:set>
<c:set var="strongPasswordLabel"><op:translate key="PASSWORD_STRONG" /></c:set>


<div class="first-connection">
    <%--Warning--%>
    <div class="alert alert-warning">
        <span><op:translate key="FIRST_CONNECTION_WARNING"/></span>
    </div>

    <form:form action="${saveUrl}" method="post" modelAttribute="userForm" cssClass="form-horizontal" role="form">

    
        <!-- Title-->
        <spring:bind path="title">
            <div class="form-group">
                <form:label path="title" cssClass="col-sm-4 col-lg-3 col-form-label"><op:translate key="TITLE" /></form:label>
                <div class="col-sm-8 col-lg-9">
                <form:select path="title" name="title" cssClass="form-control">
					<form:option value=""></form:option>
					<form:option value="M.">M.</form:option>
					<form:option value="Mme">Mme</form:option>
				</form:select>
                </div>
            </div>
        </spring:bind>
    
        <%--First name--%>
        <spring:bind path="firstName">
            <div class="form-group required ${status.error ? 'has-error has-feedback' : ''}">
                <form:label path="firstName" cssClass="col-sm-4 col-lg-3 col-form-label"><op:translate key="FIRST_NAME" /></form:label>
                <div class="col-sm-8 col-lg-9">
                    <form:input path="firstName" cssClass="form-control" />
                    <c:if test="${status.error}">
                        <span class="form-control-feedback">
                            <i class="glyphicons glyphicons-remove"></i>                    
                        </span>
                    </c:if>
                    <form:errors path="firstName" cssClass="help-block" />
                </div>
            </div>
        </spring:bind>
        
        <%--Last name--%>
        <spring:bind path="lastName">
            <div class="form-group required ${status.error ? 'has-error has-feedback' : ''}">
                <form:label path="lastName" cssClass="col-sm-4 col-lg-3 col-form-label"><op:translate key="LAST_NAME" /></form:label>
                <div class="col-sm-8 col-lg-9">
                    <form:input path="lastName" cssClass="form-control" />
                    <c:if test="${status.error}">
                        <span class="form-control-feedback">
                            <i class="glyphicons glyphicons-remove"></i>                    
                        </span>
                    </c:if>
                    <form:errors path="lastName" cssClass="help-block" />
                </div>
            </div>
        </spring:bind>
        
        <%--Email--%>
        <spring:bind path="email">
            <div class="form-group required ${status.error ? 'has-error has-feedback' : ''}">
                <form:label path="email" cssClass="col-sm-4 col-lg-3 col-form-labell"><op:translate
                        key="EMAIL"/></form:label>
                <div class="col-sm-8 col-lg-9">
                    <form:input path="email" cssClass="form-control"/>
                    <c:if test="${status.error}">
                        <span class="form-control-feedback">
                            <i class="glyphicons glyphicons-remove"></i>                    
                        </span>
                    </c:if>
                    <form:errors path="email" cssClass="help-block"/>
                </div>
            </div>
        </spring:bind>
        

        <c:if test="${userForm.mustChangePassword}">
            <%--Password--%>
            <spring:bind path="password">
                <div class="form-group required ${status.error ? 'has-error has-feedback' : ''}">
                    <form:label path="password" cssClass="col-sm-4 col-lg-3 col-form-label"><op:translate
                            key="PASSWORD"/></form:label>
                    <div class="col-sm-8 col-lg-9">
                        <div class="input-group">
	                        <span class="input-group-addon">
	                            <i class="glyphicons glyphicons-keys"></i>
	                        </span>
                            <form:password path="password" showPassword="true" cssClass="form-control"/>
                        </div>
                        <c:if test="${status.error}">
	                        <span class="form-control-feedback">
	                            <i class="glyphicons glyphicons-remove"></i>                    
	                        </span>
                        </c:if>
                        <form:errors path="password" cssClass="help-block"/>
                    </div>
                </div>

                <%--Password rules information--%>
                <div class="row">
                    <div class="col-sm-offset-4 col-sm-8 col-lg-offset-3 col-lg-9">
                        <div class="panel panel-default">
                            <div class="panel-body">
                                <p><op:translate key="PASSWORD_INFORMATION_TITLE"/></p>
                                <div data-password-information-placeholder data-url="${passwordInformationUrl}"></div>
                            </div>
                        </div>
                    </div>
                </div>
            </spring:bind>

            <%--Password confirmation--%>
            <spring:bind path="passwordConfirmation">
                <div class="form-group required ${status.error ? 'has-error has-feedback' : ''}">
                    <form:label path="passwordConfirmation" cssClass="col-sm-4 col-lg-3 col-form-label"><op:translate
                            key="PASSWORD_CONFIRMATION"/></form:label>
                    <div class="col-sm-8 col-lg-9">
                        <div class="input-group">
	                        <span class="input-group-addon">
	                            <i class="glyphicons glyphicons-keys"></i>
	                        </span>
                            <form:password path="passwordConfirmation" showPassword="true" cssClass="form-control"/>
                        </div>
                        <c:if test="${status.error}">
	                        <span class="form-control-feedback">
	                            <i class="glyphicons glyphicons-remove"></i>                    
	                        </span>
                        </c:if>
                        <form:errors path="passwordConfirmation" cssClass="help-block"/>
                    </div>
                </div>
            </spring:bind>
        </c:if>

        
        <%--Buttons--%>
        <div class="row">
            <div class="col-sm-offset-4 col-sm-8 col-lg-offset-3 col-lg-9">
                    <%--Save--%>
                <button type="submit" class="btn btn-primary">
                    <i class="glyphicons glyphicons-floppy-disk"></i>
                    <span><op:translate key="SAVE" /></span>
                </button>
            </div>
        </div>
    </form:form>
</div>
