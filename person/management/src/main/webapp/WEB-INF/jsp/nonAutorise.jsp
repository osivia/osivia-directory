<%@ include file="/WEB-INF/jsp/include.jsp"%>
<%@page import="javax.portlet.WindowState"%>

<head>

<link type="text/css" rel="stylesheet"
	href="<%=request.getContextPath()%>/css/recherchePersonneStyle.css" />

</head>


<h3>Acc�s non autoris�</h3>

	<div class="ligne">
		Vous n'�tes pas habilit� � acc�der � cette fonctionnalit�
	</div>

	<div class="ligne">
		<div class="bouton">
			<a href="<portlet:renderURL windowState="<%= WindowState.NORMAL.toString() %>"><portlet:param name="action" value="recherchePersonne" /></portlet:renderURL>">Retour �la recherche de personne</a>   
		</div>
	</div>


