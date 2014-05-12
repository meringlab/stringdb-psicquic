The official http://string-db.org PSICQUIC implementation.

Important note

The STRING-DB PSICQUIC API provides just a subset of all data available through the STRING-DB's API
string-db.org/version_9_0/newstring_cgi/show_info_page.pl?page=api.html:

    * only the 'core' species are available through PSICQUIC API (417 out of 1133)
    * each STRING-DB evidence channel is reported as a separate interaction in PSICQUIC
    * only interactions with score > 400 are available


Howto setup development environment

 - run mvn generate-sources and add target/generated/java as a source folder

To debug:
 
 mvn [clean] jetty:run
 
 http://localhost:8080/psicquic/webservices/v1.1/search/query/species:human?firstResult=0&maxResults=100
 http://localhost:8080/psicquic/webservices

To index:

nohup  MAVEN_OPTS=" -Xmx2059m " mvn -PcreateIndex clean compile
# can take a lot of time, ~60hrs!



Notes

http://code.google.com/p/psimi/wiki/PsimiTabFormat

string's tab format: 

	http://string-db.org/api/psi-mi-tab/interactions?identifier=drd2_human&species=9606

	string:9606.ENSP00000347474	string:9606.ENSP00000270349	DRD2	SLC6A3	-	-	-	-	-	taxid:9606	taxid:9606	-	-	-	score:0.983|escore:0.636|tscore:0.956


http://code.google.com/p/psicquic/wiki/HowToInstall

MI CV:
http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI

Viewer:
http://www.ebi.ac.uk/Tools/webservices/psicquic/view/main.xhtml

Registry:
http://www.ebi.ac.uk/Tools/webservices/psicquic/registry/registry?action=STATUS
